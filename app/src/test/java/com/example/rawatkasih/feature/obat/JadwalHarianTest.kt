package com.example.rawatkasih.feature.obat

import com.example.rawatkasih.feature.obat.model.StatusJadwal
import com.example.rawatkasih.feature.obat.model.statusHariIni
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class JadwalHarianTest {
    private fun waktu(hari: Int, jam: Int, menit: Int = 0) =
        Calendar.getInstance().apply { set(2026, 10, hari, jam, menit, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis

    @Test
    fun konfirmasiKemarinKembaliBelumHariIni() {
        val sekarang = waktu(27, 8)
        // Diminum kemarin pukul 14.05 → hari ini jadwal 14.00 harus dikonfirmasi lagi.
        assertEquals(StatusJadwal.BELUM_DIKONFIRMASI, statusHariIni(StatusJadwal.DIMINUM, waktu(26, 14, 5), sekarang))
        assertEquals(StatusJadwal.BELUM_DIKONFIRMASI, statusHariIni(StatusJadwal.DILEWATI, waktu(26, 23, 59), sekarang))
        // Dikonfirmasi hari ini (sesudah tengah malam) tetap tercatat.
        assertEquals(StatusJadwal.DIMINUM, statusHariIni(StatusJadwal.DIMINUM, waktu(27, 0, 1), sekarang))
        // Stempel server belum tiba atau data lama tanpa waktu: biarkan.
        assertEquals(StatusJadwal.DIMINUM, statusHariIni(StatusJadwal.DIMINUM, null, sekarang))
        assertEquals(StatusJadwal.BELUM_DIKONFIRMASI, statusHariIni(StatusJadwal.BELUM_DIKONFIRMASI, waktu(20, 9), sekarang))
    }
}
