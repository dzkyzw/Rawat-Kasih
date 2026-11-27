package com.example.rawatkasih.feature.vitalsign

import com.example.rawatkasih.feature.vitalsign.model.AnalisisVitalSign
import com.example.rawatkasih.feature.vitalsign.model.CatatanVitalSign
import com.example.rawatkasih.feature.vitalsign.model.StatusRisiko
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class AnalisisVitalSignTest {
    private val hari = TimeUnit.DAYS.toMillis(1)
    private val sekarang = 20 * hari

    private fun catatan(
        hariLalu: Long,
        sistol: Int,
        diastol: Int,
        gulaDarah: Int
    ) = CatatanVitalSign(
        id = "c-$hariLalu-$sistol-$gulaDarah",
        lansiaId = "oma-ratih",
        sistol = sistol,
        diastol = diastol,
        gulaDarah = gulaDarah,
        statusRisiko = AnalisisVitalSign.klasifikasiRisiko(sistol, diastol, gulaDarah),
        penulisId = "uid-pramurukti",
        penulisNama = "pramurukti@rawatkasih.test",
        waktu = sekarang - hariLalu * hari
    )

    @Test
    fun validasiMenolakSistolKurangAtauSamaDenganDiastolDanRentangEkstrem() {
        assertNotNull(AnalisisVitalSign.validasiInput("", "80", "110"))
        assertNotNull(AnalisisVitalSign.validasiInput("abc", "80", "110"))
        assertNotNull(AnalisisVitalSign.validasiNilai(sistol = 80, diastol = 80, gulaDarah = 110))
        assertNotNull(AnalisisVitalSign.validasiNilai(sistol = 75, diastol = 85, gulaDarah = 110))
        assertNotNull(AnalisisVitalSign.validasiNilai(sistol = 350, diastol = 90, gulaDarah = 110))
        assertNotNull(AnalisisVitalSign.validasiNilai(sistol = 120, diastol = 80, gulaDarah = 10))
        assertNull(AnalisisVitalSign.validasiInput("120", "80", "110"))
    }

    @Test
    fun klasifikasiRisikoMengikutiClinicalThreshold() {
        assertEquals(StatusRisiko.NORMAL, AnalisisVitalSign.klasifikasiRisiko(120, 80, 110))
        assertEquals(StatusRisiko.NORMAL, AnalisisVitalSign.klasifikasiRisiko(90, 60, 70))
        assertEquals(StatusRisiko.NORMAL, AnalisisVitalSign.klasifikasiRisiko(140, 90, 200))

        // Batas Waspada (> 140/90, < 90/60, GD > 200 atau < 70)
        assertEquals(StatusRisiko.WASPADA, AnalisisVitalSign.klasifikasiRisiko(145, 85, 120))
        assertEquals(StatusRisiko.WASPADA, AnalisisVitalSign.klasifikasiRisiko(130, 95, 120))
        assertEquals(StatusRisiko.WASPADA, AnalisisVitalSign.klasifikasiRisiko(85, 62, 110))
        assertEquals(StatusRisiko.WASPADA, AnalisisVitalSign.klasifikasiRisiko(120, 80, 215))
        assertEquals(StatusRisiko.WASPADA, AnalisisVitalSign.klasifikasiRisiko(120, 80, 65))

        // Batas Bahaya (krisis)
        assertEquals(StatusRisiko.BAHAYA, AnalisisVitalSign.klasifikasiRisiko(165, 102, 120))
        assertEquals(StatusRisiko.BAHAYA, AnalisisVitalSign.klasifikasiRisiko(78, 48, 100))
        assertEquals(StatusRisiko.BAHAYA, AnalisisVitalSign.klasifikasiRisiko(120, 80, 260))
        assertEquals(StatusRisiko.BAHAYA, AnalisisVitalSign.klasifikasiRisiko(120, 80, 55))
    }

    @Test
    fun historiKurangDari7HariMenampilkanDataTerkiniTanpaDelta() {
        val riwayat = listOf(
            catatan(hariLalu = 2, sistol = 118, diastol = 78, gulaDarah = 105),
            catatan(hariLalu = 1, sistol = 122, diastol = 80, gulaDarah = 110)
        )

        val ringkasan = AnalisisVitalSign.analisisTren7Hari(riwayat, sekarang)

        assertFalse(ringkasan.adaHistori7Hari)
        assertNull(ringkasan.rataSistol)
        assertNull(ringkasan.deltaSistol)
        assertEquals(2, ringkasan.titik7Hari.size)
        assertEquals(StatusRisiko.NORMAL, ringkasan.statusEfektif)
        assertFalse(ringkasan.perluAlertKeluarga)
    }

    @Test
    fun histori7HariMenghitungRataRataDeltaDanMengabaikanDataLebihTua() {
        val riwayat = listOf(
            catatan(hariLalu = 10, sistol = 180, diastol = 110, gulaDarah = 300), // di luar 7 hari
            catatan(hariLalu = 6, sistol = 110, diastol = 70, gulaDarah = 100),
            catatan(hariLalu = 4, sistol = 120, diastol = 80, gulaDarah = 110),
            catatan(hariLalu = 2, sistol = 130, diastol = 80, gulaDarah = 120),
            catatan(hariLalu = 0, sistol = 150, diastol = 92, gulaDarah = 130)
        )

        val ringkasan = AnalisisVitalSign.analisisTren7Hari(riwayat, sekarang)

        assertTrue(ringkasan.adaHistori7Hari)
        assertEquals(4, ringkasan.titik7Hari.size)
        assertEquals(128, ringkasan.rataSistol)
        assertEquals(81, ringkasan.rataDiastol)
        assertEquals(115, ringkasan.rataGulaDarah)
        assertEquals(22, ringkasan.deltaSistol)
        assertEquals(11, ringkasan.deltaDiastol)
        assertEquals(15, ringkasan.deltaGulaDarah)
        assertEquals(StatusRisiko.WASPADA, ringkasan.statusEfektif)
        assertTrue(ringkasan.perluAlertKeluarga)
        assertTrue(ringkasan.daftarPeringatan.isNotEmpty())
    }

    @Test
    fun lonjakanMendadakMenaikkanStatusNormalMenjadiWaspada() {
        val riwayat = listOf(
            catatan(hariLalu = 1, sistol = 110, diastol = 72, gulaDarah = 100),
            catatan(hariLalu = 0, sistol = 135, diastol = 85, gulaDarah = 105) // masih dalam 90..140, tapi naik +25 mmHg
        )

        val ringkasan = AnalisisVitalSign.analisisTren7Hari(riwayat, sekarang)

        assertTrue(ringkasan.adaPerubahanMendadak)
        assertEquals(StatusRisiko.WASPADA, ringkasan.statusEfektif)
        assertTrue(ringkasan.perluAlertKeluarga)
    }
}
