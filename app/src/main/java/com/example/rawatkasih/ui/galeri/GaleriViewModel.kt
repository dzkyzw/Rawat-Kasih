package com.example.rawatkasih.ui.galeri

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rawatkasih.feature.galeri.data.FotoTerlaluBesar
import com.example.rawatkasih.feature.galeri.data.GaleriRepository
import com.example.rawatkasih.feature.galeri.data.PemrosesFoto
import com.example.rawatkasih.feature.galeri.model.AlasanKoreksi
import com.example.rawatkasih.feature.galeri.model.BidangForm
import com.example.rawatkasih.feature.galeri.model.DrafDokumentasi
import com.example.rawatkasih.feature.galeri.model.FotoFisik
import com.example.rawatkasih.feature.galeri.model.KategoriKeluhan
import com.example.rawatkasih.feature.galeri.model.MesinP3K
import com.example.rawatkasih.feature.galeri.model.StatusTindak
import com.example.rawatkasih.feature.penugasan.data.PenugasanRepository
import com.example.rawatkasih.feature.penugasan.data.Sesi
import com.example.rawatkasih.feature.penugasan.model.Lansia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Fungsi 4 · Galeri fisik & SOP P3K. Pramurukti mendokumentasikan kondisi fisik lansia dampingannya, mendapat SOP
 * dari [MesinP3K], mencatat tindak lanjut, dan mengoreksi data tanpa menghapus versi asli. Keluarga hanya melihat.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class GaleriViewModel(
    private val repository: GaleriRepository,
    penugasanRepository: PenugasanRepository,
    private val pemroses: PemrosesFoto,
    private val sekarang: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val _state = MutableStateFlow(GaleriUiState())
    val state = _state.asStateFlow()
    private var sesi: Sesi? = null
    private val ulang = MutableStateFlow(0)
    private var pendengarDetail: Job? = null

    private class Data(val sesi: Sesi?, val lansia: List<Lansia>, val foto: List<FotoFisik>, val gagal: Boolean = false)

    init {
        viewModelScope.launch {
            combine(penugasanRepository.amatiSesi(), ulang) { s, _ -> s }
                .flatMapLatest { s ->
                    // Akun berganti: draf, detail, dan toast akun lama tidak boleh terbawa.
                    if (s?.uid != sesi?.uid) {
                        pendengarDetail?.cancel()
                        _state.value = GaleriUiState()
                    }
                    sesi = s
                    if (s == null || s.peran !in PERAN_GALERI || s.lansiaIds.isEmpty()) flowOf(Data(s, emptyList(), emptyList()))
                    else muat(s, penugasanRepository).catch { error ->
                        Log.e("GaleriViewModel", "Gagal memuat galeri fisik", error)
                        emit(Data(s, emptyList(), emptyList(), gagal = true))
                    }
                }
                .collect { d ->
                    _state.update {
                        it.copy(
                            dimuat = true, gagalMemuat = d.gagal, peran = d.sesi?.peran, emailSaya = d.sesi?.email.orEmpty(),
                            lansia = d.lansia, foto = d.foto, pratinjau = MesinP3K.nilai(it.draf, d.foto, sekarang())
                        )
                    }
                }
        }
    }

    private fun muat(s: Sesi, penugasanRepository: PenugasanRepository): Flow<Data> {
        val sejak = sekarang() - TimeUnit.DAYS.toMillis(HARI_GALERI)
        // Nama lansia dari rencana obat (Fungsi 1), yang sudah boleh dibaca pramurukti dan keluarga.
        val nama: Flow<List<Lansia>> = combine(
            s.lansiaIds.map { id -> penugasanRepository.amatiRencanaObat(id).map { Lansia(id, it?.namaLansia ?: "Lansia") } }
        ) { it.toList() }
        val galeri: Flow<List<FotoFisik>> = combine(s.lansiaIds.map { repository.amatiGaleri(it, sejak) }) { semua ->
            semua.flatMap { it }.sortedByDescending { it.waktu }
        }
        return combine(nama, galeri) { daftar, foto -> Data(s, daftar, foto) }
    }

    fun muat() {
        ulang.value++
    }

    fun pilihFilter(kategori: KategoriKeluhan?) {
        _state.update { it.copy(filter = kategori) }
    }

    /** Membuka form layar 51; [lansiaId] dan [kategori] terisi dari konteks (kartu tugas atau SOP). */
    fun mulaiTambah(lansiaId: String?, kategori: KategoriKeluhan? = null) {
        _state.update {
            val id = lansiaId?.takeIf { l -> it.lansia.any { x -> x.id == l } } ?: it.lansia.singleOrNull()?.id
            val draf = DrafDokumentasi(lansiaId = id, kategori = kategori)
            it.copy(draf = draf, galat = emptyMap(), pesanGagal = null, pratinjau = MesinP3K.nilai(draf, it.foto, sekarang()))
        }
    }

    fun ubahDraf(ubah: (DrafDokumentasi) -> DrafDokumentasi) {
        _state.update {
            val baru = ubah(it.draf)
            // Galat bidang yang sudah diperbaiki langsung hilang; sisanya tetap tampil.
            val galat = if (it.galat.isEmpty()) it.galat else MesinP3K.validasi(baru).filterKeys { k -> k in it.galat }
            it.copy(draf = baru, galat = galat, pratinjau = MesinP3K.nilai(baru, it.foto, sekarang()))
        }
    }

    fun prosesFoto(uri: Uri) {
        if (_state.value.draf.foto.size >= MesinP3K.FOTO_MAKS) return
        _state.update { it.copy(sedangMemprosesFoto = true, pesanGagal = null) }
        viewModelScope.launch {
            val pesan = try {
                val foto = pemroses.proses(uri)
                ubahDraf { it.copy(foto = (it.foto + foto).take(MesinP3K.FOTO_MAKS)) }
                null
            } catch (error: CancellationException) {
                throw error
            } catch (error: FotoTerlaluBesar) {
                "Foto terlalu detail untuk disimpan. Ambil ulang dari jarak sedikit lebih jauh."
            } catch (error: Exception) {
                Log.e("GaleriViewModel", "Gagal membaca foto", error)
                "Foto tidak dapat dibaca. Coba ambil ulang."
            }
            _state.update { it.copy(sedangMemprosesFoto = false, pesanGagal = pesan) }
        }
    }

    fun hapusFotoDraf(indeks: Int) {
        ubahDraf { it.copy(foto = it.foto.filterIndexed { i, _ -> i != indeks }) }
    }

    /** Validasi (foto valid & kategori dipilih), nilai mesin aturan, lalu simpan. */
    fun simpan() {
        val akun = sesi ?: return
        val s = _state.value
        if (!s.bisaMengubah || s.menyimpan) return
        val galat = MesinP3K.validasi(s.draf)
        if (galat.isNotEmpty()) {
            _state.update { it.copy(galat = galat) }
            return
        }
        val draf = s.draf
        val rekomendasi = MesinP3K.nilai(draf, s.foto, sekarang()) ?: return
        val lansiaId = draf.lansiaId!!
        jalankan("Gagal menyimpan dokumentasi") {
            val id = repository.simpan(
                lansiaId, akun.uid, akun.email, draf.foto, draf.kategori!!, draf.lokasi, draf.observasi, draf.tandaBahaya, rekomendasi,
                draf.keparahan!!
            )
            val pesan = "Dokumentasi ${s.namaLansia(lansiaId)} tersimpan · ${jam(sekarang())}" +
                if (rekomendasi.perluDokter) ". Pengelola panti diberi tahu." else ""
            _state.update {
                it.copy(draf = DrafDokumentasi(), pratinjau = null, galat = emptyMap(), toast = ToastGaleri(pesan, id), selesai = AksiSelesai.DOKUMENTASI to sekarang())
            }
        }
    }

    fun bukaDetail(foto: FotoFisik) {
        if (_state.value.detail?.fotoId == foto.id) return
        pendengarDetail?.cancel()
        _state.update { it.copy(detail = DetailGaleri(foto.id)) }
        pendengarDetail = viewModelScope.launch {
            launch {
                val gambar = runCatching { repository.muatGambar(foto.lansiaId, foto.id) }
                    .onFailure { Log.e("GaleriViewModel", "Gagal memuat foto", it) }
                perbaruiDetail(foto.id) { d -> d.copy(gambar = gambar.getOrNull(), gagalMuatGambar = gambar.isFailure) }
            }
            launch {
                repository.amatiTindakLanjut(foto.lansiaId, foto.id)
                    .catch { Log.e("GaleriViewModel", "Gagal memuat tindak lanjut", it) }
                    .collect { daftar -> perbaruiDetail(foto.id) { it.copy(tindakLanjut = daftar) } }
            }
            launch {
                repository.amatiKoreksi(foto.lansiaId, foto.id)
                    .catch { Log.e("GaleriViewModel", "Gagal memuat riwayat koreksi", it) }
                    .collect { daftar -> perbaruiDetail(foto.id) { it.copy(koreksi = daftar) } }
            }
        }
    }

    fun tutupDetail() {
        pendengarDetail?.cancel()
        _state.update { it.copy(detail = null) }
    }

    private fun perbaruiDetail(fotoId: String, ubah: (DetailGaleri) -> DetailGaleri) {
        _state.update { s ->
            val d = s.detail
            if (d != null && d.fotoId == fotoId) s.copy(detail = ubah(d)) else s
        }
    }

    /** Mengembalikan pesan galat, atau null bila mulai disimpan. */
    fun tambahTindakLanjut(foto: FotoFisik, tindakan: String, status: StatusTindak, waktu: Long): String? {
        val akun = sesi ?: return "Sesi berakhir. Silakan masuk lagi."
        MesinP3K.validasiTindakLanjut(tindakan, status, waktu, sekarang())?.let { return it }
        if (!_state.value.bisaMengubah || _state.value.menyimpan) return null
        jalankan("Gagal menyimpan tindak lanjut") {
            repository.tambahTindakLanjut(foto, tindakan, status, waktu, akun.uid, akun.email)
            val pesan = if (status == StatusTindak.SELESAI) "Tindak lanjut ${jam(sekarang())} tersimpan" else "Tindak lanjut dijadwalkan ${tanggalJam(waktu)}"
            _state.update { it.copy(toast = ToastGaleri(pesan), selesai = AksiSelesai.TINDAK_LANJUT to sekarang()) }
        }
        return null
    }

    fun koreksi(foto: FotoFisik, kategori: KategoriKeluhan, lokasi: String, alasan: AlasanKoreksi?): String? {
        val akun = sesi ?: return "Sesi berakhir. Silakan masuk lagi."
        MesinP3K.validasiKoreksi(foto, kategori, lokasi, alasan)?.let { return it }
        if (!_state.value.bisaMengubah || _state.value.menyimpan) return null
        val perubahan = MesinP3K.perubahan(foto, kategori, lokasi)
        val sop = MesinP3K.pilihSop(kategori, lokasi, foto.observasi)
        jalankan("Gagal menyimpan koreksi") {
            val versi = repository.koreksi(foto, kategori, lokasi, sop?.id, perubahan, alasan!!, akun.uid, akun.email)
            _state.update { it.copy(toast = ToastGaleri("Koreksi tersimpan · versi $versi"), selesai = AksiSelesai.KOREKSI to sekarang()) }
        }
        return null
    }

    fun tutupToast() {
        _state.update { it.copy(toast = null) }
    }

    fun tutupPesanGagal() {
        _state.update { it.copy(pesanGagal = null) }
    }

    fun selesaiDitangani() {
        _state.update { it.copy(selesai = null) }
    }

    private fun jalankan(pesanGagal: String, aksi: suspend () -> Unit) {
        _state.update { it.copy(menyimpan = true, pesanGagal = null) }
        viewModelScope.launch {
            try {
                aksi()
                _state.update { it.copy(menyimpan = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("GaleriViewModel", pesanGagal, error)
                _state.update { it.copy(menyimpan = false, pesanGagal = "$pesanGagal. Periksa koneksi lalu coba lagi.") }
            }
        }
    }

    companion object {
        private val PERAN_GALERI = setOf("PRAMURUKTI", "KELUARGA")
        const val HARI_GALERI = 28L
        private val ID = Locale.forLanguageTag("id")
        fun jam(ms: Long): String = SimpleDateFormat("HH.mm", ID).format(Date(ms))
        fun tanggalJam(ms: Long): String = SimpleDateFormat("d MMM yyyy · HH.mm", ID).format(Date(ms))
        fun tanggalPendek(ms: Long): String = SimpleDateFormat("d MMM · HH.mm", ID).format(Date(ms))
    }
}

/** Bidang yang dipakai layar untuk menampilkan galat. */
internal fun GaleriUiState.galatDi(bidang: BidangForm) = galat[bidang]
