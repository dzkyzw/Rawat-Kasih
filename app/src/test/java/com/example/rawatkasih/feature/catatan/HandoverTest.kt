package com.example.rawatkasih.feature.catatan

import com.example.rawatkasih.feature.catatan.model.CatatanShift
import com.example.rawatkasih.feature.catatan.model.Handover
import com.example.rawatkasih.feature.catatan.model.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HandoverTest {
    private val jam = 60 * 60 * 1000L
    private val sekarang = 100 * jam
    private val rina = "uid-rina"
    private val dewi = "uid-dewi"
    private val budi = "uid-budi"
    private val omaSiti = "oma-siti"
    private val opaHadi = "opa-hadi"

    private fun catatan(penulis: String, lansia: String, waktu: Long, dibacaOleh: String? = null) = CatatanShift(
        lansiaId = lansia, penulisId = penulis, penulisNama = "P$penulis", shift = "Malam", mood = Mood.TENANG,
        isi = "Tidur nyenyak semalam", waktu = waktu, dibacaOlehId = dibacaOleh
    )

    @Test
    fun hanyaCatatanOrangLainUntukLansiaDampinganDalam24JamYangMengunci() {
        val daftar = listOf(
            catatan(dewi, omaSiti, sekarang - 2 * jam),
            catatan(rina, omaSiti, sekarang - 1 * jam),
            catatan(dewi, omaSiti, sekarang - 3 * jam),
            catatan(budi, omaSiti, sekarang - 4 * jam, dibacaOleh = rina),
            catatan(budi, omaSiti, sekarang - 30 * jam),
            catatan(dewi, opaHadi, sekarang - 1 * jam)
        )

        val kunci = Handover.belumDibaca(daftar, rina, setOf(omaSiti), sekarang)

        assertEquals(listOf(sekarang - 3 * jam, sekarang - 2 * jam), kunci.map { it.waktu })
    }

    @Test
    fun pramuruktiPenuhWaktuTidakTerkunciCatatannyaSendiri() {
        val daftar = listOf(catatan(rina, omaSiti, sekarang - jam), catatan(rina, omaSiti, sekarang - 9 * jam))
        assertTrue(Handover.belumDibaca(daftar, rina, setOf(omaSiti), sekarang).isEmpty())
    }

    @Test
    fun formWajibMoodDanCatatanBermakna() {
        assertNotNull(Handover.validasi(null, "Tidur nyenyak semalam"))
        assertNotNull(Handover.validasi(Mood.TENANG, "  ok   "))
        assertNull(Handover.validasi(Mood.TENANG, "Tidur nyenyak semalam"))
    }
}
