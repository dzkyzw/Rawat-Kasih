
package com.example.rawatkasih.ui.admin

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rawatkasih.feature.admin.data.AksesAdminDitolak
import com.example.rawatkasih.feature.admin.data.AdminRepository
import com.example.rawatkasih.feature.admin.data.KonflikAkunAdmin
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AdminViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _state = MutableStateFlow(AdminUiState())
    val state = _state.asStateFlow()
    private var pekerjaan: Job? = null

    fun muat() {
        if (pekerjaan?.isActive == true) return
        pekerjaan = viewModelScope.launch { muatDaftar() }
    }

    fun simpanLansia(id: String, nama: String, kamar: String, email: String, kataSandi: String) {
        if (nama.trim().length !in 1..100 || kamar.trim().length > 40) return
        if ((email.isNotBlank() || kataSandi.isNotEmpty()) && !akunValid(email, kataSandi)) {
            _state.update { it.copy(pesanError = "Isi email keluarga yang valid dan kata sandi minimal 8 karakter.") }
            return
        }
        kerjakan("Profil lansia tersimpan.", id) {
            repository.simpanLansia(id, nama.trim(), kamar.trim(), email.trim(), kataSandi)
        }
    }

    fun buatPramurukti(email: String, kataSandi: String) {
        if (!akunValid(email, kataSandi)) {
            _state.update { it.copy(pesanError = "Isi email pramurukti yang valid dan kata sandi minimal 8 karakter.") }
            return
        }
        kerjakan("Akun pramurukti siap ditugaskan.", emailPramurukti = email.trim()) {
            repository.buatPramurukti(email.trim(), kataSandi)
        }
    }

    fun ubahPenugasan(uid: String, lansiaId: String, tugaskan: Boolean) =
        kerjakan(if (tugaskan) "Penugasan ditambahkan." else "Penugasan dilepas.") {
            repository.ubahPenugasan(uid, lansiaId, tugaskan)
        }

    fun kirimTautanSandi(email: String) =
        kerjakan("Tautan atur kata sandi dikirim ke ${email}.", muatUlang = false) {
            repository.kirimTautanSandi(email)
        }

    fun reset() {
        pekerjaan?.cancel()
        pekerjaan = null
        _state.value = AdminUiState()
    }

    private fun kerjakan(
        pesan: String,
        lansiaId: String? = null,
        emailPramurukti: String? = null,
        muatUlang: Boolean = true,
        aksi: suspend () -> Unit
    ) {
        if (pekerjaan?.isActive == true) return
        _state.update {
            it.copy(
                sedangMenyimpan = true,
                pesanError = null,
                pesanBerhasil = null,
                lansiaTersimpan = null,
                emailPramuruktiTersimpan = null
            )
        }
        pekerjaan = viewModelScope.launch {
            try {
                aksi()
                if (muatUlang) muatDaftar()
                _state.update {
                    it.copy(
                        sedangMenyimpan = false,
                        pesanBerhasil = pesan,
                        lansiaTersimpan = lansiaId,
                        emailPramuruktiTersimpan = emailPramurukti
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(sedangMenyimpan = false, pesanError = pesanGagal(error)) }
            }
        }
    }

    private suspend fun muatDaftar() {
        _state.update { it.copy(sedangMemuat = true, gagalMemuat = null) }
        try {
            coroutineScope {
                val lansia = async { repository.daftarLansia() }
                val pramurukti = async { repository.daftarPramurukti() }
                val daftarLansia = lansia.await()
                val daftarPramurukti = pramurukti.await()
                _state.update { it.copy(sedangMemuat = false, lansia = daftarLansia, pramurukti = daftarPramurukti) }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _state.update { it.copy(sedangMemuat = false, gagalMemuat = pesanGagal(error)) }
        }
    }

    private fun akunValid(email: String, kataSandi: String): Boolean =
        email.trim().length <= 254 && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() && kataSandi.length in 8..128

    private fun pesanGagal(error: Exception): String =
        when (error) {
            is KonflikAkunAdmin -> error.pesan
            is AksesAdminDitolak -> "Akses admin tidak tersedia. Masuk kembali sebagai admin."
            is FirebaseFirestoreException -> when (error.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED, FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "Firebase menolak akses. Periksa aturan Firestore dan sesi admin."
                else -> "Data belum tersimpan. Coba lagi dengan email dan kata sandi yang sama."
            }
            is FirebaseAuthWeakPasswordException -> "Kata sandi belum memenuhi syarat Firebase."
            is FirebaseAuthInvalidCredentialsException -> "Periksa format email dan kata sandi."
            is FirebaseAuthInvalidUserException -> "Akun email ini tidak bisa digunakan."
            else -> "Permintaan gagal. Periksa koneksi dan coba lagi."
        }
}
