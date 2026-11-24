package com.example.rawatkasih.feature.obat.data

import com.example.rawatkasih.feature.obat.model.RencanaObat
import com.example.rawatkasih.feature.obat.model.StatusJadwal
import kotlinx.coroutines.flow.Flow

internal interface SumberObat {
    fun amatiRencana(): Flow<RencanaObat>
    suspend fun catat(jam: String, status: StatusJadwal)
}
