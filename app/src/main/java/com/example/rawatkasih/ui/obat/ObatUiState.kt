package com.example.rawatkasih.ui.obat

import com.example.rawatkasih.feature.obat.data.remote.PilihanLansiaObat
import com.example.rawatkasih.feature.obat.model.RencanaObat

internal enum class StatusSimpanObat { SIAP, MENYIMPAN, TERSIMPAN, GAGAL }

internal data class ObatUiState(
    val rencana: RencanaObat? = null,
    val gagalMemuat: Boolean = false,
    val pesanMuat: String? = null,
    val sedangMuatUlang: Boolean = false,
    val perluMasuk: Boolean = false,
    val sedangGantiSandi: Boolean = false,
    val sandiDiubah: Boolean = false,
    val pesanGantiSandi: String? = null,
    val sedangMasuk: Boolean = false,
    val pesanMasuk: String? = null,
    val peranAkun: String? = null,
    val daftarLansia: List<PilihanLansiaObat>? = null,
    val lansiaId: String? = null,
    val statusSimpan: StatusSimpanObat = StatusSimpanObat.SIAP,
    val pesanSimpan: String? = null
)
