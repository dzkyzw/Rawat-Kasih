package com.example.rawatkasih.feature.obat.notification

import com.example.rawatkasih.feature.obat.model.RencanaObat

internal interface PengingatObat {
    fun periksaDanIngatkan(rencana: RencanaObat)
}

internal object PengingatObatKosong : PengingatObat {
    override fun periksaDanIngatkan(rencana: RencanaObat) = Unit
}
