package com.example.rawatkasih.ui.obat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rawatkasih.feature.obat.data.ObatRepository
import com.example.rawatkasih.feature.obat.model.StatusJadwal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ObatViewModel(private val repository: ObatRepository) : ViewModel() {
    private val _state = MutableStateFlow(ObatUiState())
    val state = _state.asStateFlow()
    private var pekerjaanMuat: Job? = null

    init {
        muat()
    }

    fun muat() {
        if (pekerjaanMuat?.isActive == true) return

        _state.update { it.copy(gagalMemuat = false) }
        pekerjaanMuat = viewModelScope.launch {
            repository.amatiRencana()
                .catch { error ->
                    Log.e("ObatViewModel", "Gagal memuat jadwal obat", error)
                    _state.update { it.copy(rencana = null, gagalMemuat = true) }
                }
                .collect { rencana -> _state.update { it.copy(rencana = rencana) } }
        }
    }

    fun catat(jam: String, status: StatusJadwal) {
        val saatIni = _state.value
        if (saatIni.rencana == null || saatIni.statusSimpan == StatusSimpanObat.MENYIMPAN) return

        ubahStatusSimpan(StatusSimpanObat.MENYIMPAN)
        viewModelScope.launch {
            try {
                repository.catat(jam, status)
                ubahStatusSimpan(StatusSimpanObat.SIAP)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("ObatViewModel", "Gagal menyimpan konfirmasi obat", error)
                ubahStatusSimpan(StatusSimpanObat.GAGAL)
            }
        }
    }

    fun hapusKesalahanSimpan() {
        _state.update {
            if (it.statusSimpan == StatusSimpanObat.GAGAL) {
                it.copy(statusSimpan = StatusSimpanObat.SIAP)
            } else {
                it
            }
        }
    }

    private fun ubahStatusSimpan(status: StatusSimpanObat) {
        _state.update { it.copy(statusSimpan = status) }
    }
}
