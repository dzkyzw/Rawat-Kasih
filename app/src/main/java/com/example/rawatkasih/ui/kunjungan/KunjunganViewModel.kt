package com.example.rawatkasih.ui.kunjungan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rawatkasih.feature.kunjungan.data.KunjunganRepository
import com.example.rawatkasih.feature.kunjungan.model.AturanKunjungan
import com.example.rawatkasih.feature.kunjungan.model.Kunjungan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal data class KunjunganUiState(
    val lansiaId: String? = null,
    val dimuat: Boolean = false,
    val gagalMemuat: Boolean = false,
    val daftar: List<Kunjungan> = emptyList(),
    val menyimpan: Boolean = false,
    /** Bertambah setiap permintaan baru tersimpan; form memakainya untuk menutup diri. */
    val jumlahTersimpan: Int = 0,
)

/** Kunjungan keluarga: keluarga mengajukan, pengelola menyetujui/menolak, pramurukti melihat jadwalnya. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class KunjunganViewModel(
    private val repository: KunjunganRepository,
    private val sekarang: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val lansiaId = MutableStateFlow<String?>(null)
    private val ulang = MutableStateFlow(0)
    private val _state = MutableStateFlow(KunjunganUiState())
    val state = _state.asStateFlow()
    private val _pesan = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Pesan singkat untuk snackbar (berhasil atau gagal). */
    val pesan = _pesan.asSharedFlow()

    /** Pengelola: semua permintaan di panti; null = sedang dimuat. Hanya berjalan selama layar pengelola terbuka. */
    val semua: StateFlow<List<Kunjungan>?> = repository.amatiSemua()
        .catch { Log.e("Kunjungan", "Gagal memuat semua kunjungan", it); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            combine(lansiaId.filterNotNull(), ulang) { id, _ -> id }.flatMapLatest { id ->
                repository.amati(id)
                    .map { KunjunganUiState(lansiaId = id, dimuat = true, daftar = it) }
                    .onStart { emit(KunjunganUiState(lansiaId = id)) }
                    .catch { error ->
                        Log.e("Kunjungan", "Gagal memuat kunjungan", error)
                        emit(KunjunganUiState(lansiaId = id, gagalMemuat = true))
                    }
            }.collect { baru -> _state.update { lama -> baru.copy(menyimpan = lama.menyimpan, jumlahTersimpan = lama.jumlahTersimpan) } }
        }
    }

    fun pilihLansia(id: String) {
        // Gagal sebelumnya (mis. sesi berganti atau koneksi putus): membuka halaman lagi berarti memuat ulang.
        if (lansiaId.value == id && _state.value.gagalMemuat) ulang.value++
        if (lansiaId.value != id) _state.value = KunjunganUiState(lansiaId = id)
        lansiaId.value = id
    }

    fun hariIni(): String = format(sekarang(), 0)
    fun batasAkhir(): String = format(sekarang(), AturanKunjungan.MAKS_HARI)

    /** Mengembalikan pesan galat; null berarti permintaan mulai dikirim. */
    fun ajukan(tanggal: String?, jam: String?, jumlahOrang: Int, catatan: String): String? {
        val galat = AturanKunjungan.validasi(tanggal, jam, jumlahOrang, hariIni(), batasAkhir())
            ?: if (catatan.trim().length > AturanKunjungan.MAKS_CATATAN) "Catatan maksimal ${AturanKunjungan.MAKS_CATATAN} huruf." else null
        val id = lansiaId.value
        if (galat != null || id == null || tanggal == null || jam == null) return galat
        if (_state.value.menyimpan) return null
        jalankan("Permintaan kunjungan terkirim. Pengelola akan meninjaunya.", "Permintaan belum terkirim. Periksa koneksi lalu coba lagi.", tersimpan = true) {
            repository.ajukan(id, tanggal, jam, jumlahOrang, catatan)
        }
        return null
    }

    fun tanggapi(k: Kunjungan, setuju: Boolean, alasan: String = "") =
        jalankan(if (setuju) "Kunjungan disetujui." else "Kunjungan ditolak.", "Tanggapan belum tersimpan. Coba lagi.") {
            repository.tanggapi(k, setuju, alasan)
        }

    fun batalkan(k: Kunjungan) = jalankan("Kunjungan dibatalkan.", "Pembatalan belum tersimpan. Coba lagi.") { repository.batalkan(k) }

    private fun jalankan(berhasil: String, gagal: String, tersimpan: Boolean = false, aksi: suspend () -> Unit) {
        if (_state.value.menyimpan) return
        _state.update { it.copy(menyimpan = true) }
        viewModelScope.launch {
            try {
                aksi()
                _state.update { it.copy(menyimpan = false, jumlahTersimpan = it.jumlahTersimpan + if (tersimpan) 1 else 0) }
                _pesan.emit(berhasil)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("Kunjungan", "Aksi kunjungan gagal", error)
                _state.update { it.copy(menyimpan = false) }
                _pesan.emit(gagal)
            }
        }
    }

    private fun format(waktu: Long, tambahHari: Int): String {
        val kal = Calendar.getInstance().apply { timeInMillis = waktu; add(Calendar.DAY_OF_YEAR, tambahHari) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(kal.time)
    }

    companion object {
        /** "2026-11-29" → "Sabtu, 29 November". */
        fun tanggalPanjang(tanggal: String): String = runCatching {
            val d: Date = checkNotNull(SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(tanggal))
            SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("id")).format(d)
        }.getOrDefault(tanggal)
    }
}
