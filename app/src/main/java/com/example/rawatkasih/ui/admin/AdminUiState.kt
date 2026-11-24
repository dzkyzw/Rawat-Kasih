package com.example.rawatkasih.ui.admin

import com.example.rawatkasih.feature.admin.data.LansiaAdmin

internal data class AdminUiState(
    val lansia: List<LansiaAdmin> = emptyList(),
    val sedangMemuat: Boolean = false,
    val sedangMembuat: Boolean = false,
    val gagalMemuat: String? = null,
    val pesanError: String? = null,
    val pesanBerhasil: String? = null
)
