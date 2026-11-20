package com.example.rawatkasih.feature.obat

import com.example.rawatkasih.feature.obat.model.StatusJadwal
import com.example.rawatkasih.feature.obat.model.rencanaObatContoh
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RencanaObatTest {
    @Test
    fun diminumMengurangiStokSekaliDanMemicuPeringatanPadaBatas() {
        val awal = rencanaObatContoh()
        assertFalse(awal.stokMenipis)

        val state = awal.catat("08.00", StatusJadwal.DIMINUM)
            .catat("08.00", StatusJadwal.DIMINUM)
        assertEquals(3, state.stok)
        assertTrue(state.stokMenipis)
        assertEquals(listOf("08.00", "06.00"), state.riwayat.map { it.jam })
        assertEquals(StatusJadwal.DIMINUM, state.riwayat.first().status)
        assertEquals("12.00", state.jadwalBerikutnya?.jam)
    }

    @Test
    fun dilewatiMasukRiwayatTanpaMengurangiStok() {
        val state = rencanaObatContoh().catat("08.00", StatusJadwal.DILEWATI)
            .catat("12.00", StatusJadwal.DILEWATI)
        assertEquals(4, state.stok)
        assertFalse(state.stokMenipis)
        assertEquals(listOf("12.00", "08.00", "06.00"), state.riwayat.map { it.jam })
        assertEquals(StatusJadwal.DILEWATI, state.riwayat.first().status)
        assertNull(state.jadwalBerikutnya)
    }

    @Test
    fun stokHabisMencegahKonfirmasiDiminum() {
        val state = rencanaObatContoh().copy(stok = 0)
        assertEquals(state, state.catat("08.00", StatusJadwal.DIMINUM))
    }
}
