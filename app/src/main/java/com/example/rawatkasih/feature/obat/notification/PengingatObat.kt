package com.example.rawatkasih.feature.obat.notification

import com.example.rawatkasih.feature.obat.model.RencanaObat

/**
 * Kontrak OOP untuk layanan pengingat jadwal dan peringatan stok obat.
 */
internal interface PengingatObat {
    fun periksaDanIngatkan(rencana: RencanaObat)
}

internal object PengingatObatKosong : PengingatObat {
    override fun periksaDanIngatkan(rencana: RencanaObat) = Unit
}
