
package com.example.rawatkasih.ui.admin

import com.example.rawatkasih.feature.admin.data.LansiaAdmin
import com.example.rawatkasih.feature.admin.data.PramuruktiAdmin

internal data class AdminUiState(
    val lansia: List<LansiaAdmin> = emptyList(),
    val pramurukti: List<PramuruktiAdmin> = emptyList(),
    val sedangMemuat: Boolean = false,
    val sedangMenyimpan: Boolean = false,
    val gagalMemuat: String? = null,
    val pesanError: String? = null,
    val pesanBerhasil: String? = null,
    val lansiaTersimpan: String? = null,
    val emailPramuruktiTersimpan: String? = null
)
