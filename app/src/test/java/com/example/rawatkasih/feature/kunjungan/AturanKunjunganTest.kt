package com.example.rawatkasih.feature.kunjungan

import com.example.rawatkasih.feature.kunjungan.model.AturanKunjungan
import com.example.rawatkasih.feature.kunjungan.model.Kunjungan
import com.example.rawatkasih.feature.kunjungan.model.StatusKunjungan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AturanKunjunganTest {
    private val hariIni = "2026-11-27"
    private val batas = "2026-12-27"

    private fun k(id: String, tanggal: String, jam: String, status: StatusKunjungan) =
        Kunjungan(id, "oma", tanggal, jam, 2, "", status, "uid", "anak@contoh.id")

    @Test
    fun validasiMenolakTanggalLewatTerlaluJauhJamDiLuarBesukDanJumlahOrang() {
        assertEquals("Pilih tanggal kunjungan.", AturanKunjungan.validasi(null, "10.00", 2, hariIni, batas))
        assertEquals("Tanggal itu sudah lewat.", AturanKunjungan.validasi("2026-11-26", "10.00", 2, hariIni, batas))
        assertEquals(
            "Kunjungan bisa diajukan paling jauh 30 hari ke depan.",
            AturanKunjungan.validasi("2026-12-28", "10.00", 2, hariIni, batas)
        )
        assertEquals("Pilih jam kunjungan.", AturanKunjungan.validasi(hariIni, "12.00", 2, hariIni, batas))
        assertEquals("Jumlah pengunjung 1–5 orang.", AturanKunjungan.validasi(hariIni, "10.00", 6, hariIni, batas))
        assertNull(AturanKunjungan.validasi(hariIni, "09.00", 1, hariIni, batas))
        assertNull(AturanKunjungan.validasi(batas, "16.00", 5, hariIni, batas))
    }

    @Test
    fun akanDatangHanyaYangMasihBerlakuDanTerurutWaktu() {
        val daftar = listOf(
            k("a", "2026-11-30", "14.00", StatusKunjungan.DISETUJUI),
            k("b", "2026-11-28", "10.00", StatusKunjungan.MENUNGGU),
            k("c", "2026-11-28", "09.00", StatusKunjungan.DITOLAK),     // ditolak → riwayat
            k("d", "2026-11-20", "10.00", StatusKunjungan.DISETUJUI),   // sudah lewat → riwayat
            k("e", "2026-11-29", "11.00", StatusKunjungan.DIBATALKAN),  // dibatalkan → riwayat
            k("f", hariIni, "16.00", StatusKunjungan.DISETUJUI),        // hari ini masih berlaku
        )
        assertEquals(listOf("f", "b", "a"), AturanKunjungan.akanDatang(daftar, hariIni).map { it.id })
        assertEquals(listOf("e", "c", "d"), AturanKunjungan.riwayat(daftar, hariIni).map { it.id })
        assertEquals(listOf("b"), AturanKunjungan.menunggu(daftar, hariIni).map { it.id })
    }
}
