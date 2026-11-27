package com.example.rawatkasih.ui.keluarga

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rawatkasih.feature.kontak.KontakRepository
import com.example.rawatkasih.feature.kontak.nomorWaValid
import com.example.rawatkasih.feature.kontak.normalTelepon
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Nomor WhatsApp panti dan keluarga. Semua opsional: akun lama tanpa nomor tetap berjalan seperti biasa. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class KontakViewModel(private val repository: KontakRepository) : ViewModel() {
    private val lansiaId = MutableStateFlow<String?>(null)
    private val _menyimpan = MutableStateFlow(false)
    val menyimpan = _menyimpan.asStateFlow()
    private val _pesan = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val pesan = _pesan.asSharedFlow()

    val nomorPanti: StateFlow<String?> = repository.amatiNomorPanti()
        .catch { Log.e("Kontak", "Gagal memuat nomor panti", it); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val nomorKeluarga: StateFlow<String?> = lansiaId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repository.amatiNomorKeluarga(id).catch { Log.e("Kontak", "Gagal memuat nomor keluarga", it); emit(null) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Pengelola: lansiaId → nomor keluarga. */
    val semuaNomorKeluarga: StateFlow<Map<String, String>> = repository.amatiSemuaNomorKeluarga()
        .catch { Log.e("Kontak", "Gagal memuat nomor keluarga", it); emit(emptyMap()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun pilihLansia(id: String) {
        lansiaId.value = id
    }

    /** null = mulai disimpan; selain itu pesan galat untuk ditampilkan di bawah isian. */
    fun simpanNomorPanti(masukan: String): String? = simpan(masukan) { repository.simpanNomorPanti(it) }

    fun simpanNomorKeluarga(masukan: String): String? {
        val id = lansiaId.value ?: return "Data lansia belum termuat. Coba lagi sebentar."
        return simpan(masukan) { repository.simpanNomorKeluarga(id, it) }
    }

    private fun simpan(masukan: String, aksi: suspend (String) -> Unit): String? {
        val nomor = normalTelepon(masukan)
        if (!nomorWaValid(nomor)) return "Tulis nomor WhatsApp yang aktif, mis. 0812 3456 7890."
        if (_menyimpan.value) return null
        _menyimpan.value = true
        viewModelScope.launch {
            try {
                aksi(nomor)
                _pesan.emit("Nomor WhatsApp tersimpan.")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("Kontak", "Gagal menyimpan nomor", error)
                _pesan.emit("Nomor belum tersimpan. Periksa koneksi lalu coba lagi.")
            } finally {
                _menyimpan.value = false
            }
        }
        return null
    }
}
