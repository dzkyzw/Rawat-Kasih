package com.example.rawatkasih.ui.vitalsign

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rawatkasih.feature.vitalsign.data.VitalSignRepository
import com.example.rawatkasih.feature.vitalsign.model.AnalisisVitalSign
import com.example.rawatkasih.feature.vitalsign.model.CatatanVitalSign
import com.example.rawatkasih.feature.vitalsign.model.RingkasanTrenVital
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

internal data class RekapVitalLansia(
    val lansiaId: String,
    val riwayat: List<CatatanVitalSign>,
    val ringkasan: RingkasanTrenVital
)

/**
 * Pengelola Panti (Admin): memantau rekapitulasi dan statistik vital sign seluruh lansia secara kolektif.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class VitalAdminViewModel(
    private val repository: VitalSignRepository
) : ViewModel() {
    private val daftarLansiaIds = MutableStateFlow<List<String>>(emptyList())
    private val lansiaDipilih = MutableStateFlow<String?>(null)
    val lansiaAktif: StateFlow<String?> = lansiaDipilih.asStateFlow()

    val rekap: StateFlow<Map<String, RekapVitalLansia>> = daftarLansiaIds
        .flatMapLatest { ids ->
            if (ids.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(
                    ids.map { id ->
                        repository.amatiVitalSign(id)
                            .catch { error ->
                                Log.e("VitalAdminViewModel", "Gagal memuat vital sign lansia $id", error)
                                emit(emptyList())
                            }
                            .map { id to it }
                    }
                ) { hasil ->
                    withContext(Dispatchers.Default) {
                        val sekarang = System.currentTimeMillis()
                        hasil.associate { (id, riwayat) ->
                            id to RekapVitalLansia(id, riwayat, AnalisisVitalSign.analisisTren7Hari(riwayat, sekarang))
                        }
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun sinkronLansia(ids: List<String>) {
        val unik = ids.distinct().sorted()
        if (daftarLansiaIds.value != unik) {
            daftarLansiaIds.value = unik
        }
    }

    fun pilihLansia(id: String?) {
        lansiaDipilih.value = id
    }
}
