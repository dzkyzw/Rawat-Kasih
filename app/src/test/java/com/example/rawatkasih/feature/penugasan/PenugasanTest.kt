package com.example.rawatkasih.feature.penugasan

import com.example.rawatkasih.feature.penugasan.model.AturanPenugasan
import com.example.rawatkasih.feature.penugasan.model.Penugasan
import com.example.rawatkasih.feature.penugasan.model.PolaShift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PenugasanTest {
    @Test
    fun polaBisaDuaTigaAtauBerapaPunShiftDanMenutup24Jam() {
        assertEquals(listOf(7, 19), PolaShift.bagiRata(2).jamMulai)
        assertEquals(listOf(7, 15, 23), PolaShift.bagiRata(3).jamMulai)
        assertEquals(listOf("Pagi", "Siang", "Sore", "Malam"), (0 until 4).map { PolaShift.bagiRata(4).nama(it) })
        for (n in 1..6) assertEquals(24, PolaShift.bagiRata(n).durasi().sum())
    }

    @Test
    fun shiftMalamMelewatiTengahMalam() {
        val pola = PolaShift.bagiRata(3) // 07–15, 15–23, 23–07
        assertEquals(0, pola.shiftPada(7))
        assertEquals(1, pola.shiftPada(22))
        assertEquals(2, pola.shiftPada(23))
        assertEquals(2, pola.shiftPada(3))
        assertEquals("23.00–07.00", pola.rentang(2))
        assertEquals(0, PolaShift(listOf(7)).shiftPada(2)) // satu shift = sepanjang hari
    }

    @Test
    fun jamMulaiKembarAtauTidakMelingkarDitolak() {
        assertThrows(IllegalArgumentException::class.java) { PolaShift(listOf(7, 7)) }
        assertThrows(IllegalArgumentException::class.java) { PolaShift(listOf(7, 15, 10)) }
    }

    @Test
    fun catatanDiteruskanKeShiftBerikutnyaYangTerisiKecualiPenuhWaktu() {
        val pola = PolaShift.bagiRata(3)
        val rina = Penugasan(lansiaId = "oma", pramuruktiId = "rina", shiftKe = 0)
        val dewi = Penugasan(lansiaId = "oma", pramuruktiId = "dewi", shiftKe = 2)
        // Siang kosong, jadi catatan pagi diteruskan ke shift malam.
        assertEquals(dewi, AturanPenugasan.penerusan(listOf(rina, dewi), "oma", 0, pola))
        assertEquals(rina, AturanPenugasan.penerusan(listOf(rina, dewi), "oma", 2, pola))
        val penuh = Penugasan(lansiaId = "opa", pramuruktiId = "rina", shiftKe = null)
        assertNull(AturanPenugasan.penerusan(listOf(penuh), "opa", null, pola))
        assertTrue(AturanPenugasan.sedangBertugas(penuh, 1))
        assertFalse(AturanPenugasan.sedangBertugas(rina, 1))
        assertEquals("Penuh waktu", AturanPenugasan.labelShift(penuh, pola, 1))
        assertEquals("Pagi", AturanPenugasan.labelShift(rina, pola, 1))
    }

    @Test
    fun penugasanWajibTerisi() {
        assertNotNull(AturanPenugasan.validasi(penuhWaktu = true, pramuruktiPenuh = null, perShift = emptyMap()))
        assertNotNull(AturanPenugasan.validasi(penuhWaktu = false, pramuruktiPenuh = null, perShift = emptyMap()))
        assertNull(AturanPenugasan.validasi(penuhWaktu = false, pramuruktiPenuh = null, perShift = mapOf(1 to "rina")))
    }

    @Test
    fun awalTugasMengikutiShiftTermasukMalamKemarin() {
        val pola = PolaShift.bagiRata(2) // 07–19, 19–07
        fun jam(h: Int) = java.util.Calendar.getInstance().apply { set(2026, 10, 25, h, 30, 0); set(java.util.Calendar.MILLISECOND, 0) }
        fun hasil(t: Penugasan, h: Int) = java.util.Calendar.getInstance().apply { timeInMillis = AturanPenugasan.awalTugas(t, pola, jam(h)) }
        val malam = Penugasan("oma", "dewi", 1)
        // Jam 03.30 masih shift malam yang mulai kemarin 19.00.
        assertEquals(24, hasil(malam, 3).get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(19, hasil(malam, 3).get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(25, hasil(malam, 20).get(java.util.Calendar.DAY_OF_MONTH))
        val penuh = Penugasan("opa", "rina", null)
        assertEquals(0, hasil(penuh, 15).get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(25, hasil(penuh, 15).get(java.util.Calendar.DAY_OF_MONTH))
    }
}
