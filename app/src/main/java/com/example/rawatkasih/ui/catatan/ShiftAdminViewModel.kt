package com.example.rawatkasih.ui.catatan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rawatkasih.feature.catatan.data.CatatanRepository
import com.example.rawatkasih.feature.catatan.model.CatatanShift
import com.example.rawatkasih.feature.penugasan.data.PenugasanGagal
import com.example.rawatkasih.feature.penugasan.data.PenugasanRepository
import com.example.rawatkasih.feature.penugasan.model.AturanPenugasan
import com.example.rawatkasih.feature.penugasan.model.Penugasan
import com.example.rawatkasih.feature.penugasan.model.PolaShift
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** Pengelola: pola shift panti, penugasan per shift/penuh waktu, dan riwayat handover tiap lansia. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ShiftAdminViewModel(
    private val penugasan: PenugasanRepository,
    private val catatan: CatatanRepository
) : ViewModel() {
    private val lansiaId = MutableStateFlow<String?>(null)
    val lansiaDipilih = lansiaId.asStateFlow()

    val pola: StateFlow<PolaShift?> = penugasan.amatiPola()
        .catch { Log.e("ShiftAdminViewModel", "Gagal memuat pola shift", it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** null = sedang dimuat. */
    val tugas: StateFlow<List<Penugasan>?> = lansiaId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else penugasan.amatiPenugasan(id).catch { emit(emptyList()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val riwayat: StateFlow<List<CatatanShift>> = lansiaId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else catatan.amatiCatatan(id).catch { emit(emptyList()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun pilihLansia(id: String) {
        lansiaId.value = id
    }

    /** Mengembalikan pesan kesalahan, atau null bila tersimpan. */
    suspend fun simpanPola(baru: PolaShift): String? = coba { penugasan.simpanPola(baru) }

    suspend fun simpanPenugasan(
        lansiaId: String,
        penuhWaktu: Boolean,
        pramuruktiPenuh: String?,
        perShift: Map<Int, String>,
        email: Map<String, String>,
        ditugaskanSekarang: Set<String>
    ): String? {
        AturanPenugasan.validasi(penuhWaktu, pramuruktiPenuh, perShift)?.let { return it }
        return coba {
            penugasan.simpanPenugasan(lansiaId, pramuruktiPenuh.takeIf { penuhWaktu }, if (penuhWaktu) emptyMap() else perShift, email, ditugaskanSekarang)
        }
    }

    private suspend fun coba(aksi: suspend () -> Unit): String? = try {
        aksi()
        null
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Log.e("ShiftAdminViewModel", "Gagal menyimpan", error)
        when {
            error is PenugasanGagal -> error.pesan
            error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Firebase menolak akses. Periksa aturan Firestore dan sesi admin."
            else -> "Belum tersimpan. Periksa koneksi lalu coba lagi."
        }
    }
}
