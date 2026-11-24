package com.example.rawatkasih.ui.obat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rawatkasih.feature.obat.data.SumberObat
import com.example.rawatkasih.feature.obat.data.remote.AksesObatDitolak
import com.example.rawatkasih.feature.obat.data.remote.FirebaseObatRepository
import com.example.rawatkasih.feature.obat.data.remote.KonflikObat
import com.example.rawatkasih.feature.obat.data.remote.SesiFirebaseBerakhir
import com.example.rawatkasih.feature.obat.model.StatusJadwal
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ObatViewModel(private val repository: SumberObat) : ViewModel() {
    private val online = repository as? FirebaseObatRepository
    val modeOnline = online != null
    private val _state = MutableStateFlow(ObatUiState(perluMasuk = online?.sudahMasuk == false))
    val state = _state.asStateFlow()
    private var pekerjaanMuat: Job? = null
    private var pekerjaanSegarkan: Job? = null

    init {
        muat()
    }

    fun masuk(email: String, password: String) {
        val sumber = online ?: return
        if (!_state.value.perluMasuk || _state.value.sedangMasuk) return
        _state.update { it.copy(sedangMasuk = true, pesanMasuk = null) }
        viewModelScope.launch {
            try {
                sumber.masuk(email.trim(), password)
                _state.value = ObatUiState()
                muat()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val pesan = if (error is FirebaseAuthInvalidCredentialsException || error is FirebaseAuthInvalidUserException) {
                    "Email atau kata sandi tidak cocok."
                } else {
                    "Gagal masuk ke Firebase. Periksa koneksi lalu coba lagi."
                }
                _state.update { it.copy(sedangMasuk = false, pesanMasuk = pesan) }
            }
        }
    }

    fun keluar() {
        val sumber = online ?: return
        if (_state.value.statusSimpan == StatusSimpanObat.MENYIMPAN) return
        pekerjaanMuat?.cancel()
        pekerjaanSegarkan?.cancel()
        pekerjaanMuat = null
        _state.value = ObatUiState(perluMasuk = true)
        viewModelScope.launch { sumber.keluar() }
    }

    fun muat() {
        if (_state.value.perluMasuk || pekerjaanMuat?.isActive == true) return

        _state.update { it.copy(gagalMemuat = false, pesanMuat = null) }
        pekerjaanMuat = viewModelScope.launch {
            flow {
                if (online?.adalahAdmin() == true) {
                    _state.update { it.copy(adalahAdmin = true) }
                } else {
                    emitAll(repository.amatiRencana())
                }
            }
                .catch { error ->
                    if (error is SesiFirebaseBerakhir) {
                        tampilkanSesiBerakhir()
                    } else {
                        Log.e("ObatViewModel", "Gagal memuat jadwal obat", error)
                        _state.update { it.copy(rencana = null, gagalMemuat = true, pesanMuat = pesanMuat(error)) }
                    }
                }
                .collect { rencana -> _state.update { it.copy(rencana = rencana) } }
        }
    }

    fun segarkan() {
        val sumber = online ?: return
        if (_state.value.rencana == null || pekerjaanSegarkan?.isActive == true) return
        _state.update { it.copy(sedangMuatUlang = true, pesanMuat = null) }
        pekerjaanSegarkan = viewModelScope.launch {
            try {
                sumber.segarkan()
            } catch (error: CancellationException) {
                throw error
            } catch (error: SesiFirebaseBerakhir) {
                tampilkanSesiBerakhir()
            } catch (error: Exception) {
                if (aksesDitolak(error)) {
                    pekerjaanMuat?.cancel()
                    pekerjaanMuat = null
                    _state.update { it.copy(rencana = null, gagalMemuat = true, pesanMuat = pesanMuat(error)) }
                } else {
                    _state.update { it.copy(pesanMuat = pesanMuat(error)) }
                }
            } finally {
                _state.update { it.copy(sedangMuatUlang = false) }
            }
        }
    }

    fun catat(jam: String, status: StatusJadwal) = simpan(StatusSimpanObat.SIAP) {
        repository.catat(jam, status)
    }

    fun simpanPengaturan(stokAwal: Int, stokBaru: Int?, jam: String?, waktu: String?) {
        val sumber = online ?: return
        simpan(StatusSimpanObat.TERSIMPAN) { sumber.simpanPengaturan(stokAwal, stokBaru, jam, waktu) }
    }

    private fun simpan(statusSukses: StatusSimpanObat, aksi: suspend () -> Unit) {
        val saatIni = _state.value
        if (saatIni.rencana?.bisaMengubah != true || saatIni.statusSimpan == StatusSimpanObat.MENYIMPAN) return

        ubahStatusSimpan(StatusSimpanObat.MENYIMPAN)
        viewModelScope.launch {
            try {
                aksi()
                ubahStatusSimpan(statusSukses)
            } catch (error: CancellationException) {
                throw error
            } catch (error: SesiFirebaseBerakhir) {
                tampilkanSesiBerakhir()
            } catch (error: Exception) {
                Log.e("ObatViewModel", "Gagal menyimpan konfirmasi obat", error)
                val pesan = when {
                    error is KonflikObat -> error.pesan
                    aksesDitolak(error) -> "Akun ini tidak memiliki izin mengubah rencana obat."
                    else -> "Gagal menyimpan. Periksa koneksi dan muat ulang sebelum mencoba lagi."
                }
                ubahStatusSimpan(StatusSimpanObat.GAGAL, pesan)
            }
        }
    }

    fun hapusKesalahanSimpan() {
        _state.update {
            if (it.statusSimpan != StatusSimpanObat.MENYIMPAN) {
                it.copy(statusSimpan = StatusSimpanObat.SIAP, pesanSimpan = null)
            } else {
                it
            }
        }
    }

    private fun ubahStatusSimpan(status: StatusSimpanObat, pesan: String? = null) {
        _state.update { it.copy(statusSimpan = status, pesanSimpan = pesan) }
    }

    private fun tampilkanSesiBerakhir() {
        pekerjaanMuat?.cancel()
        pekerjaanMuat = null
        _state.value = ObatUiState(perluMasuk = true, pesanMasuk = "Sesi berakhir. Silakan masuk kembali.")
    }

    private fun pesanMuat(error: Throwable) =
        if (aksesDitolak(error)) {
            "Akun ini belum terhubung ke rencana obat atau izinnya sudah dicabut."
        } else {
            "Jadwal obat gagal dimuat."
        }

    private fun aksesDitolak(error: Throwable) =
        error is AksesObatDitolak ||
            (error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
}
