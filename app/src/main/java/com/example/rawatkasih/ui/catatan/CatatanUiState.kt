package com.example.rawatkasih.ui.catatan

import com.example.rawatkasih.feature.catatan.model.CatatanShift
import com.example.rawatkasih.feature.obat.model.RencanaObat
import com.example.rawatkasih.feature.penugasan.model.Lansia
import com.example.rawatkasih.feature.penugasan.model.Penugasan
import com.example.rawatkasih.feature.penugasan.model.PolaShift

internal enum class StatusSimpanCatatan { SIAP, MENYIMPAN, GAGAL }

/** Lansia yang ditugaskan ke pramurukti yang sedang masuk, beserta jenis tugasnya. */
internal data class TugasLansia(val lansia: Lansia, val tugas: Penugasan)

internal data class CatatanUiState(
    val dimuat: Boolean = false,
    val gagalMemuat: Boolean = false,
    val email: String = "",
    val semua: List<CatatanShift> = emptyList(),
    val belumDibaca: List<CatatanShift> = emptyList(),
    val tugasSaya: List<TugasLansia> = emptyList(),
    val lansia: List<Lansia> = emptyList(),
    val penugasan: List<Penugasan> = emptyList(),
    val pola: PolaShift = PolaShift(listOf(7)),
    val shiftSekarang: Int = 0,
    /** Ringkasan obat tiap lansia dampingan, untuk layar Tugas saya. */
    val obat: Map<String, RencanaObat> = emptyMap(),
    /** Lansia yang sedang dibuka di layar catatan. */
    val lansiaDipilih: String? = null,
    val statusSimpan: StatusSimpanCatatan = StatusSimpanCatatan.SIAP,
    /** Bertambah setiap catatan baru tersimpan; layar memakainya untuk mengosongkan form. */
    val jumlahTersimpan: Int = 0
) {
    fun namaLansia(id: String) = lansia.firstOrNull { it.id == id }?.nama ?: "Lansia"
}
