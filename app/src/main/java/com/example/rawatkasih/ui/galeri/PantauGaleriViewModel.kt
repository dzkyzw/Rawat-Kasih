package com.example.rawatkasih.ui.galeri

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rawatkasih.feature.galeri.data.GaleriRepository
import com.example.rawatkasih.feature.galeri.model.FotoFisik
import com.example.rawatkasih.feature.galeri.model.KategoriKeluhan
import com.example.rawatkasih.feature.galeri.model.MesinP3K
import com.example.rawatkasih.feature.galeri.model.StatusTindak
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Fungsi 4 · layar 65g "Pantau · Galeri fisik" untuk pengelola panti: ringkasan minggu ini, dokumentasi yang
 * dinilai mesin aturan perlu dokter (sampai ditangani), dan dokumentasi terbaru dari semua lansia.
 */
internal class PantauGaleriViewModel(
    private val repository: GaleriRepository,
    /** uid dan email admin yang sedang masuk, atau null bila sesi berakhir. */
    private val akunSekarang: () -> Pair<String, String>?,
    private val sekarang: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val _state = MutableStateFlow(PantauGaleriUiState())
    val state = _state.asStateFlow()
    private var pendengar: Job? = null
    private var lansiaTerakhir: Map<String, String>? = null

    /** Dipanggil saat tab dibuka atau daftar lansia berubah; [lansia] = id → nama. */
    fun mulai(lansia: Map<String, String>, paksa: Boolean = false) {
        if (!paksa && lansia == lansiaTerakhir && pendengar?.isActive == true) return
        lansiaTerakhir = lansia
        pendengar?.cancel()
        _state.update { it.copy(namaLansia = lansia, gagalMemuat = false) }
        if (lansia.isEmpty()) {
            _state.update { it.copy(dimuat = true, perluDokter = emptyList(), terbaru = emptyList(), ringkasan = RingkasanPantau()) }
            return
        }
        val seminggu = sekarang() - TimeUnit.DAYS.toMillis(7)
        val terbaru = combine(lansia.keys.map { repository.amatiGaleri(it, sekarang() - TimeUnit.DAYS.toMillis(GaleriViewModel.HARI_GALERI)) }) { daftar ->
            daftar.flatMap { it }.sortedByDescending { it.waktu }
        }
        val darurat = combine(lansia.keys.map { repository.amatiPerluDokter(it) }) { daftar ->
            daftar.flatMap { it }.sortedByDescending { it.waktu }
        }
        pendengar = viewModelScope.launch {
            combine(terbaru, darurat) { t, d -> t to d }
                .catch { error ->
                    Log.e("PantauGaleriViewModel", "Gagal memuat galeri fisik", error)
                    _state.update { it.copy(dimuat = true, gagalMemuat = true) }
                }
                .collect { (t, d) ->
                    val minggu = t.filter { it.waktu >= seminggu }
                    _state.update {
                        it.copy(
                            dimuat = true,
                            gagalMemuat = false,
                            perluDokter = d,
                            terbaru = t.filter { f -> d.none { x -> x.id == f.id } }.take(20),
                            ringkasan = RingkasanPantau(
                                mingguIni = minggu.size,
                                lukaKulit = minggu.count { f -> f.kategori == KategoriKeluhan.LUKA_KULIT },
                                memarBengkak = minggu.count { f -> f.kategori == KategoriKeluhan.MEMAR || f.kategori == KategoriKeluhan.BENGKAK }
                            )
                        )
                    }
                }
        }
    }

    /** Pengelola mencatat tindakan dokter/perawat; dokumentasi menjadi "Ditangani". Mengembalikan pesan galat atau null. */
    fun tandaiDitangani(foto: FotoFisik, tindakan: String): String? {
        val (uid, email) = akunSekarang() ?: return "Sesi berakhir. Silakan masuk lagi."
        MesinP3K.validasiTindakLanjut(tindakan, StatusTindak.SELESAI, sekarang(), sekarang())?.let { return it }
        if (_state.value.menyimpan) return null
        _state.update { it.copy(menyimpan = true, pesan = null) }
        viewModelScope.launch {
            val pesan = try {
                repository.tambahTindakLanjut(foto, tindakan, StatusTindak.SELESAI, null, uid, email)
                "Tindak lanjut ${GaleriViewModel.jam(sekarang())} tersimpan"
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("PantauGaleriViewModel", "Gagal menyimpan tindak lanjut", error)
                "Gagal menyimpan. Periksa koneksi lalu coba lagi."
            }
            _state.update { it.copy(menyimpan = false, pesan = pesan) }
        }
        return null
    }
}
