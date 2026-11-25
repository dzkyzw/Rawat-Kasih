package com.example.rawatkasih.ui.catatan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rawatkasih.feature.catatan.data.CatatanRepository
import com.example.rawatkasih.feature.catatan.model.CatatanShift
import com.example.rawatkasih.feature.catatan.model.Handover
import com.example.rawatkasih.feature.catatan.model.Mood
import com.example.rawatkasih.feature.obat.model.RencanaObat
import com.example.rawatkasih.feature.penugasan.data.PenugasanRepository
import com.example.rawatkasih.feature.penugasan.data.Sesi
import com.example.rawatkasih.feature.penugasan.model.AturanPenugasan
import com.example.rawatkasih.feature.penugasan.model.Lansia
import com.example.rawatkasih.feature.penugasan.model.Penugasan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

/** Fungsi 2 · Catatan shift & handover untuk pramurukti yang sedang masuk. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CatatanViewModel(
    private val repository: CatatanRepository,
    private val penugasanRepository: PenugasanRepository,
    private val jamSekarang: () -> Int = { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
) : ViewModel() {
    private val _state = MutableStateFlow(CatatanUiState())
    val state = _state.asStateFlow()
    private var sesi: Sesi? = null
    private val ulang = MutableStateFlow(0)

    private class DataLansia(val lansia: Lansia, val obat: RencanaObat?, val penugasan: List<Penugasan>, val catatan: List<CatatanShift>)

    init {
        viewModelScope.launch {
            combine(penugasanRepository.amatiSesi(), ulang) { s, _ -> s }
                .flatMapLatest { s ->
                    sesi = s
                    if (s == null || s.peran != "PRAMURUKTI") flowOf(CatatanUiState(dimuat = true))
                    else muatPramurukti(s).catch { error ->
                        Log.e("CatatanViewModel", "Gagal memuat catatan shift", error)
                        emit(CatatanUiState(gagalMemuat = true))
                    }
                }
                .collect { baru ->
                    _state.update { lama ->
                        baru.copy(statusSimpan = lama.statusSimpan, jumlahTersimpan = lama.jumlahTersimpan, lansiaDipilih = lama.lansiaDipilih)
                    }
                }
        }
    }

    fun pilihLansia(id: String) {
        _state.update { it.copy(lansiaDipilih = id) }
    }

    fun muat() {
        ulang.value++
    }

    private fun muatPramurukti(s: Sesi): Flow<CatatanUiState> {
        // Jam dicek tiap menit supaya pergantian shift terbaca tanpa membuka ulang aplikasi.
        val jam = flow { while (true) { emit(jamSekarang()); delay(60_000) } }
        val perLansia: Flow<List<DataLansia>> = if (s.lansiaIds.isEmpty()) flowOf(emptyList()) else combine(
            s.lansiaIds.map { id ->
                combine(
                    penugasanRepository.amatiRencanaObat(id),
                    penugasanRepository.amatiPenugasan(id),
                    repository.amatiCatatan(id)
                ) { obat, p, c -> DataLansia(Lansia(id, obat?.namaLansia ?: "Lansia"), obat, p, c) }
            }
        ) { it.toList() }

        return combine(penugasanRepository.amatiPola(), perLansia, jam) { pola, daftar, j ->
            val milikSaya = daftar.flatMap { d ->
                // Penugasan lama tanpa rincian shift dianggap penuh waktu.
                d.penugasan.filter { it.pramuruktiId == s.uid }.ifEmpty { listOf(Penugasan(d.lansia.id, s.uid, null, s.email)) }
                    .map { TugasLansia(d.lansia, it) }
            }
            val catatan = daftar.flatMap { it.catatan }.sortedByDescending { it.waktu }
            CatatanUiState(
                dimuat = true, email = s.email, semua = catatan, lansia = daftar.map { it.lansia },
                penugasan = daftar.flatMap { it.penugasan }, pola = pola, shiftSekarang = pola.shiftPada(j),
                tugasSaya = milikSaya, obat = daftar.mapNotNull { d -> d.obat?.let { d.lansia.id to it } }.toMap(), belumDibaca = Handover.belumDibaca(catatan, s.uid, s.lansiaIds.toSet())
            )
        }
    }

    /** Mengembalikan pesan kesalahan form, atau null bila catatan mulai disimpan. */
    fun simpan(lansiaId: String?, mood: Mood?, isi: String): String? {
        val akun = sesi ?: return "Sesi berakhir. Silakan masuk lagi."
        if (lansiaId == null) return "Pilih lansia."
        Handover.validasi(mood, isi)?.let { return it }
        if (_state.value.statusSimpan == StatusSimpanCatatan.MENYIMPAN || mood == null) return null

        val s = _state.value
        val tugas = s.tugasSaya.firstOrNull { it.lansia.id == lansiaId && AturanPenugasan.sedangBertugas(it.tugas, s.shiftSekarang) }
            ?: s.tugasSaya.firstOrNull { it.lansia.id == lansiaId }
        val label = AturanPenugasan.labelShift(tugas?.tugas, s.pola, s.shiftSekarang)
        jalankan("Gagal menyimpan catatan shift") {
            repository.simpan(lansiaId, akun.uid, akun.email, label, mood, isi)
            _state.update { it.copy(jumlahTersimpan = it.jumlahTersimpan + 1) }
        }
        return null
    }

    fun konfirmasiDibaca() {
        val akun = sesi ?: return
        val daftar = _state.value.belumDibaca
        if (daftar.isEmpty() || _state.value.statusSimpan == StatusSimpanCatatan.MENYIMPAN) return
        jalankan("Gagal menandai catatan dibaca") { repository.tandaiDibaca(daftar, akun.uid, akun.email) }
    }

    fun hapusKesalahanSimpan() {
        _state.update { if (it.statusSimpan == StatusSimpanCatatan.GAGAL) it.copy(statusSimpan = StatusSimpanCatatan.SIAP) else it }
    }

    private fun jalankan(pesanGagal: String, aksi: suspend () -> Unit) {
        _state.update { it.copy(statusSimpan = StatusSimpanCatatan.MENYIMPAN) }
        viewModelScope.launch {
            try {
                aksi()
                _state.update { it.copy(statusSimpan = StatusSimpanCatatan.SIAP) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("CatatanViewModel", pesanGagal, error)
                _state.update { it.copy(statusSimpan = StatusSimpanCatatan.GAGAL) }
            }
        }
    }
}
