package com.example.rawatkasih.ui.vitalsign

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rawatkasih.feature.penugasan.data.PenugasanRepository
import com.example.rawatkasih.feature.penugasan.data.Sesi
import com.example.rawatkasih.feature.penugasan.model.Lansia
import com.example.rawatkasih.feature.vitalsign.data.VitalSignRepository
import com.example.rawatkasih.feature.vitalsign.model.AnalisisVitalSign
import com.example.rawatkasih.feature.vitalsign.model.CatatanVitalSign
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlinx.coroutines.withContext

/**
 * Fungsi 3 · Monitoring & Analisis Tren Vital Sign Medis.
 * Mengamati data vital sign real-time untuk Pramurukti (input & grafik) dan Keluarga (grafik & alert).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class VitalSignViewModel(
    private val repository: VitalSignRepository,
    private val penugasanRepository: PenugasanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(VitalSignUiState())
    val state = _state.asStateFlow()
    private var sesi: Sesi? = null
    private val ulang = MutableStateFlow(0)

    private class DataVitalLansia(val lansia: Lansia, val riwayat: List<CatatanVitalSign>)

    init {
        viewModelScope.launch {
            combine(penugasanRepository.amatiSesi(), ulang) { s, _ -> s }
                .flatMapLatest { s ->
                    sesi = s
                    if (s == null || (s.peran != "PRAMURUKTI" && s.peran != "KELUARGA")) {
                        flowOf(VitalSignUiState(dimuat = true, peran = s?.peran, email = s?.email.orEmpty()))
                    } else {
                        muatData(s).catch { error ->
                            Log.e("VitalSignViewModel", "Gagal memuat data vital sign", error)
                            val pesan = if (error is FirebaseFirestoreException &&
                                error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                            ) {
                                "Akses data vital sign ditolak oleh aturan Firestore. Pastikan aturan terbaru sudah terpasang."
                            } else {
                                "Data vital sign gagal dimuat. Periksa koneksi lalu coba lagi."
                            }
                            emit(VitalSignUiState(gagalMemuat = true, pesanMuat = pesan, peran = s.peran, email = s.email))
                        }
                    }
                }
                .collect { baru ->
                    _state.update { lama ->
                        baru.copy(
                            lansiaDipilih = lama.lansiaDipilih,
                            statusSimpan = lama.statusSimpan,
                            pesanSimpan = lama.pesanSimpan,
                            jumlahTersimpan = lama.jumlahTersimpan
                        )
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

    private fun muatData(s: Sesi): Flow<VitalSignUiState> {
        val perLansia: Flow<List<DataVitalLansia>> = if (s.lansiaIds.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(
                s.lansiaIds.map { id ->
                    combine(
                        penugasanRepository.amatiRencanaObat(id),
                        repository.amatiVitalSign(id)
                    ) { obat, riwayat ->
                        DataVitalLansia(Lansia(id, obat?.namaLansia ?: "Lansia"), riwayat)
                    }
                }
            ) { it.toList() }
        }

        // Kalkulasi agregat tren 7 hari dijalankan di Dispatchers.Default agar tidak membebani Main Thread.
        return perLansia.map { daftar ->
            val ringkasan = withContext(Dispatchers.Default) {
                val sekarang = System.currentTimeMillis()
                daftar.associate { it.lansia.id to AnalisisVitalSign.analisisTren7Hari(it.riwayat, sekarang) }
            }
            VitalSignUiState(
                dimuat = true,
                email = s.email,
                peran = s.peran,
                lansia = daftar.map { it.lansia }.sortedBy { it.nama },
                riwayatPerLansia = daftar.associate { it.lansia.id to it.riwayat },
                ringkasanPerLansia = ringkasan
            )
        }
    }

    /**
     * Memvalidasi input medis lalu menyimpan ke Firestore.
     * Mengembalikan pesan validasi form jika tidak valid, atau null jika mulai disimpan.
     */
    fun simpan(lansiaId: String?, sistolTeks: String, diastolTeks: String, gulaDarahTeks: String): String? {
        val akun = sesi ?: return "Sesi berakhir. Silakan masuk lagi."
        if (akun.peran != "PRAMURUKTI") return "Hanya pramurukti yang dapat mencatat vital sign."
        if (lansiaId == null) return "Pilih lansia terlebih dahulu."
        AnalisisVitalSign.validasiInput(sistolTeks, diastolTeks, gulaDarahTeks)?.let { return it }
        if (_state.value.statusSimpan == StatusSimpanVital.MENYIMPAN) return null

        val sistol = sistolTeks.trim().toInt()
        val diastol = diastolTeks.trim().toInt()
        val gulaDarah = gulaDarahTeks.trim().toInt()
        val statusRisiko = AnalisisVitalSign.klasifikasiRisiko(sistol, diastol, gulaDarah)

        _state.update { it.copy(statusSimpan = StatusSimpanVital.MENYIMPAN, pesanSimpan = null) }
        viewModelScope.launch {
            try {
                repository.simpan(lansiaId, akun.uid, akun.email, sistol, diastol, gulaDarah, statusRisiko)
                _state.update {
                    it.copy(
                        statusSimpan = StatusSimpanVital.SIAP,
                        pesanSimpan = null,
                        jumlahTersimpan = it.jumlahTersimpan + 1
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("VitalSignViewModel", "Gagal menyimpan vital sign", error)
                val pesan = if (error is FirebaseFirestoreException &&
                    error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                ) {
                    "Firebase menolak penyimpanan. Pastikan aturan Firestore untuk vitalSign sudah di-deploy."
                } else {
                    "Data vital sign gagal disimpan. Periksa koneksi lalu coba lagi."
                }
                _state.update { it.copy(statusSimpan = StatusSimpanVital.GAGAL, pesanSimpan = pesan) }
            }
        }
        return null
    }

    fun hapusKesalahanSimpan() {
        _state.update {
            if (it.statusSimpan == StatusSimpanVital.GAGAL) {
                it.copy(statusSimpan = StatusSimpanVital.SIAP, pesanSimpan = null)
            } else {
                it
            }
        }
    }
}
