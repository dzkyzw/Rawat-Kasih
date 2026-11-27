package com.example.rawatkasih.ui.vitalsign

import com.example.rawatkasih.feature.penugasan.model.Lansia
import com.example.rawatkasih.feature.vitalsign.model.CatatanVitalSign
import com.example.rawatkasih.feature.vitalsign.model.RingkasanTrenVital

internal enum class StatusSimpanVital { SIAP, MENYIMPAN, GAGAL }

internal data class VitalSignUiState(
    val dimuat: Boolean = false,
    val gagalMemuat: Boolean = false,
    val pesanMuat: String? = null,
    val email: String = "",
    val peran: String? = null,
    val lansia: List<Lansia> = emptyList(),
    val lansiaDipilih: String? = null,
    val riwayatPerLansia: Map<String, List<CatatanVitalSign>> = emptyMap(),
    val ringkasanPerLansia: Map<String, RingkasanTrenVital> = emptyMap(),
    val statusSimpan: StatusSimpanVital = StatusSimpanVital.SIAP,
    val pesanSimpan: String? = null,
    val jumlahTersimpan: Int = 0
) {
    val bisaInput: Boolean
        get() = peran == "PRAMURUKTI"

    fun lansiaAktifId(): String? =
        lansiaDipilih?.takeIf { id -> lansia.any { it.id == id } } ?: lansia.firstOrNull()?.id

    fun namaLansiaAktif(): String =
        lansiaAktifId()?.let { id -> lansia.firstOrNull { it.id == id }?.nama } ?: "Lansia"

    fun riwayatAktif(): List<CatatanVitalSign> =
        lansiaAktifId()?.let { riwayatPerLansia[it] }.orEmpty()

    fun ringkasanAktif(): RingkasanTrenVital? =
        lansiaAktifId()?.let { ringkasanPerLansia[it] }
}
