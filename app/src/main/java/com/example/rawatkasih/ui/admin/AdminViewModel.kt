package com.example.rawatkasih.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rawatkasih.feature.admin.data.AksesAdminDitolak
import com.example.rawatkasih.feature.admin.data.AdminRepository
import com.example.rawatkasih.feature.admin.data.KonflikAkunAdmin
import com.example.rawatkasih.feature.admin.data.PenyambunganAkunGagal
import com.example.rawatkasih.feature.admin.data.PeranAkun
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AdminViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _state = MutableStateFlow(AdminUiState())
    val state = _state.asStateFlow()
    private var pemuatan: Job? = null

    fun muat() {
        if (_state.value.sedangMemuat) return
        _state.update { it.copy(sedangMemuat = true, gagalMemuat = null) }
        pemuatan = viewModelScope.launch {
            try {
                val lansia = repository.daftarLansia()
                _state.update { it.copy(sedangMemuat = false, lansia = lansia) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(sedangMemuat = false, lansia = emptyList(), gagalMemuat = pesanGagal(error)) }
            }
        }
    }

    fun buatAkun(email: String, kataSandi: String, peran: PeranAkun, rencanaId: String) {
        if (_state.value.sedangMembuat || _state.value.lansia.none { it.id == rencanaId }) return
        _state.update { it.copy(sedangMembuat = true, pesanError = null, pesanBerhasil = null) }
        viewModelScope.launch {
            try {
                val dibuat = repository.buatAkun(email.trim(), kataSandi, peran, rencanaId)
                val pesan = if (dibuat) {
                    "Akun ${email.trim()} sudah aktif sebagai ${peran.label}."
                } else {
                    "Akun ${email.trim()} sudah aktif. Gunakan kata sandi yang dipakai saat akun dibuat."
                }
                _state.update { it.copy(sedangMembuat = false, pesanBerhasil = pesan) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(sedangMembuat = false, pesanError = pesanGagal(error)) }
            }
        }
    }

    fun reset() {
        pemuatan?.cancel()
        _state.value = AdminUiState()
    }

    private fun pesanGagal(error: Exception): String =
        when {
            error is KonflikAkunAdmin -> error.pesan
            error is PenyambunganAkunGagal ->
                "Akun sudah terdaftar, tetapi akses lansia belum tersimpan. Coba lagi dengan email dan kata sandi yang sama."
            error is AksesAdminDitolak || error is FirebaseFirestoreException &&
                error.code in setOf(FirebaseFirestoreException.Code.PERMISSION_DENIED, FirebaseFirestoreException.Code.UNAUTHENTICATED) ->
                "Akses admin tidak tersedia. Masuk kembali sebagai admin."
            error is FirebaseAuthWeakPasswordException -> "Kata sandi belum memenuhi syarat Firebase."
            error is FirebaseAuthInvalidCredentialsException -> "Periksa format email dan kata sandi."
            error is FirebaseAuthInvalidUserException -> "Email sudah terdaftar tetapi akun itu tidak bisa digunakan."
            else -> "Firebase tidak bisa menyelesaikan permintaan. Periksa koneksi dan coba lagi."
        }
}
