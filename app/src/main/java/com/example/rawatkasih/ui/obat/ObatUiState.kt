package com.example.rawatkasih.ui.obat

import com.example.rawatkasih.feature.obat.model.RencanaObat

internal enum class StatusSimpanObat { SIAP, MENYIMPAN, GAGAL }

internal data class ObatUiState(
    val rencana: RencanaObat? = null,
    val gagalMemuat: Boolean = false,
    val statusSimpan: StatusSimpanObat = StatusSimpanObat.SIAP
)
