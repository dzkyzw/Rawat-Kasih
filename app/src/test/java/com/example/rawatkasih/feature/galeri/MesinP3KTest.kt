package com.example.rawatkasih.feature.galeri

import com.example.rawatkasih.feature.galeri.model.AlasanKoreksi
import com.example.rawatkasih.feature.galeri.model.BidangForm
import com.example.rawatkasih.feature.galeri.model.DrafDokumentasi
import com.example.rawatkasih.feature.galeri.model.FotoDraf
import com.example.rawatkasih.feature.galeri.model.FotoFisik
import com.example.rawatkasih.feature.galeri.model.FotoBaru
import com.example.rawatkasih.feature.galeri.model.KategoriKeluhan
import com.example.rawatkasih.feature.galeri.model.Keparahan
import com.example.rawatkasih.feature.galeri.model.KelompokSop
import com.example.rawatkasih.feature.galeri.model.MesinP3K
import com.example.rawatkasih.feature.galeri.model.PustakaSop
import com.example.rawatkasih.feature.galeri.model.StatusDokumentasi
import com.example.rawatkasih.feature.galeri.model.StatusTindak
import com.example.rawatkasih.feature.galeri.model.TandaBahaya
import com.example.rawatkasih.feature.galeri.model.inisial
import com.example.rawatkasih.feature.galeri.model.namaPetugas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MesinP3KTest {
    private val hari = 24 * 60 * 60 * 1000L
    private val sekarang = 100 * hari

    private fun foto(
        kategori: KategoriKeluhan = KategoriKeluhan.LUKA_KULIT,
        lokasi: String = "Tungkai bawah kiri",
        waktu: Long = sekarang - hari,
        ditangani: Boolean = false,
        lansiaId: String = "opa-herman",
        versi: Int = 1,
        perluDokter: Boolean = false
    ) = FotoFisik(
        id = "f$waktu", lansiaId = lansiaId, kategori = kategori, lokasi = lokasi, observasi = "", thumbnail = "x",
        jumlahFoto = 1, sopId = null, tandaBahaya = emptySet(), perluDokter = perluDokter, alasanDokter = emptyList(),
        ditangani = ditangani, versi = versi, pengunggahId = "uid-sinta", pengunggahEmail = "sinta@panti.id", waktu = waktu
    )

    private fun nilai(
        kategori: KategoriKeluhan = KategoriKeluhan.LUKA_KULIT,
        lokasi: String = "Tungkai bawah kiri",
        observasi: String = "Lecet sekitar 2 cm, tidak berdarah. Sudah dibersihkan, dilaporkan ke perawat.",
        dicentang: Set<TandaBahaya> = emptySet(),
        riwayat: List<FotoFisik> = emptyList()
    ) = MesinP3K.nilai("opa-herman", kategori, lokasi, observasi, dicentang, riwayat, sekarang)

    @Test
    fun contohFigmaLecetRinganTanpaAlarm() {
        val hasil = nilai()
        assertEquals(PustakaSop.LECET, hasil.sop)
        assertFalse(hasil.perluDokter)
        assertTrue(hasil.tandaTerdeteksi.isEmpty())
    }

    @Test
    fun lukaKulitDiAreaTumpuanMemakaiSopLukaTekan() {
        assertEquals(PustakaSop.LUKA_TEKAN, nilai(lokasi = "Bokong kanan", observasi = "Kemerahan, kulit utuh.").sop)
        assertEquals(PustakaSop.LUKA_TEKAN, nilai(lokasi = "Punggung", observasi = "Merah karena tekanan kasur.").sop)
    }

    @Test
    fun kategoriLainnyaMembacaKataKunci() {
        assertEquals(PustakaSop.JATUH, nilai(KategoriKeluhan.LAINNYA, "Pinggul", "Terpeleset di kamar mandi.").sop)
        assertEquals(PustakaSop.SUHU, nilai(KategoriKeluhan.LAINNYA, "Dahi", "Badan terasa panas, suhu 38,2.").sop)
        assertNull(nilai(KategoriKeluhan.LAINNYA, "Lengan", "Kulit kering dan bersisik.").sop)
        assertEquals(PustakaSop.MEMAR, nilai(KategoriKeluhan.MEMAR).sop)
        assertEquals(PustakaSop.BENGKAK, nilai(KategoriKeluhan.BENGKAK).sop)
    }

    @Test
    fun tandaBahayaDariObservasiDenganNegasi() {
        assertTrue(MesinP3K.deteksiTanda("Lecet, tidak berdarah terus, tidak ada nanah.").isEmpty())
        assertEquals(setOf(TandaBahaya.INFEKSI), MesinP3K.deteksiTanda("Luka mulai bernanah."))
        assertEquals(setOf(TandaBahaya.PINGSAN), MesinP3K.deteksiTanda("Lansia sempat tidak sadar."))
        assertTrue(MesinP3K.deteksiTanda("Tidak demam, tidak sesak.").isEmpty())
        assertTrue(MesinP3K.deteksiTanda("Terasa kepanasan").isEmpty())
    }

    @Test
    fun tandaDicentangAtauTerdeteksiMenaikkanStatus() {
        val dicentang = nilai(dicentang = setOf(TandaBahaya.BERDARAH_TERUS))
        assertTrue(dicentang.perluDokter)
        assertEquals(listOf("Tanda bahaya: berdarah terus."), dicentang.alasan)

        val terdeteksi = nilai(observasi = "Luka bernanah dan berbau.")
        assertTrue(terdeteksi.perluDokter)
        assertEquals(setOf(TandaBahaya.INFEKSI), terdeteksi.tandaTerdeteksi)
    }

    @Test
    fun memarSetelahJatuhMenyarankanSopJatuh() {
        val hasil = nilai(KategoriKeluhan.MEMAR, "Lengan kanan", "Memar setelah terjatuh dari kursi.")
        assertEquals(PustakaSop.MEMAR, hasil.sop)
        assertEquals(PustakaSop.JATUH, hasil.sopLain)
        assertTrue(hasil.perluDokter)
    }

    @Test
    fun menetapTigaKaliDalamSeminggu() {
        val dua = listOf(foto(waktu = sekarang - 2 * hari), foto(lokasi = " tungkai  bawah KIRI ", waktu = sekarang - hari))
        val hasil = nilai(riwayat = dua)
        assertTrue(hasil.perluDokter)
        assertTrue(hasil.alasan.single().contains("3 kali"))
        assertFalse(nilai(riwayat = dua.take(1)).perluDokter)
        assertFalse(nilai(riwayat = dua.map { it.copy(ditangani = true) }).perluDokter)
        assertFalse(nilai(riwayat = dua.map { it.copy(lansiaId = "oma-ratna") }).perluDokter)
        assertFalse(nilai(riwayat = dua.map { it.copy(waktu = sekarang - 8 * hari) }).perluDokter)
    }

    @Test
    fun validasiPerBidang() {
        val kosong = MesinP3K.validasi(DrafDokumentasi())
        assertEquals(
            setOf(BidangForm.LANSIA, BidangForm.FOTO, BidangForm.KATEGORI, BidangForm.KEPARAHAN, BidangForm.LOKASI, BidangForm.OBSERVASI),
            kosong.keys
        )
        val lengkap = DrafDokumentasi(
            "opa-herman", listOf(FotoDraf("a", "b", sekarang)), KategoriKeluhan.LUKA_KULIT, "Tungkai bawah kiri", "Lecet 2 cm.",
            keparahan = Keparahan.RINGAN
        )
        assertTrue(MesinP3K.validasi(lengkap).isEmpty())
        assertNotNull(MesinP3K.nilai(lengkap, emptyList(), sekarang))
        assertNull(MesinP3K.nilai(DrafDokumentasi(), emptyList(), sekarang))
    }

    @Test
    fun keparahanParahDanDaruratButuhDokterRinganSedangMandiri() {
        fun dengan(p: Keparahan) = MesinP3K.nilai("opa-herman", KategoriKeluhan.LUKA_KULIT, "Tungkai bawah kiri",
            "Lecet sekitar 2 cm, sudah dibersihkan.", emptySet(), emptyList(), sekarang, p)
        listOf(Keparahan.RINGAN, Keparahan.SEDANG).forEach { p ->
            assertFalse(dengan(p).perluDokter)
            assertFalse(dengan(p).darurat)
            assertEquals(PustakaSop.LECET, dengan(p).sop)
        }
        listOf(Keparahan.PARAH, Keparahan.DARURAT).forEach { p ->
            assertTrue(dengan(p).perluDokter)
            assertTrue(dengan(p).darurat)
            assertTrue(dengan(p).alasan.first().startsWith("Keparahan ${p.label.lowercase()}"))
        }
        assertTrue(PustakaSop.DARURAT_SEMENTARA.any { it.contains("119") })
    }

    @Test
    fun notifikasiHanyaUntukDokumentasiBaruSetelahPemeriksaanPertama() {
        val a = foto(waktu = sekarang - 2 * hari)
        val b = foto(waktu = sekarang - hari)
        assertTrue(FotoBaru.cari(null, listOf(a, b)).isEmpty())
        assertEquals(listOf(b), FotoBaru.cari(setOf(a.id), listOf(a, b)))
        assertTrue(FotoBaru.cari(setOf(a.id, b.id), listOf(a, b)).isEmpty())
    }

    @Test
    fun koreksiMencatatSelisihSepertiFigma() {
        val lama = foto()
        assertEquals(listOf("Lokasi: kiri → kanan"), MesinP3K.perubahan(lama, KategoriKeluhan.LUKA_KULIT, "Tungkai bawah kanan"))
        assertEquals(listOf("Kategori: Luka kulit → Memar"), MesinP3K.perubahan(lama, KategoriKeluhan.MEMAR, "tungkai bawah kiri"))
        assertEquals("Belum ada yang diubah.", MesinP3K.validasiKoreksi(lama, KategoriKeluhan.LUKA_KULIT, "Tungkai bawah kiri", AlasanKoreksi.SALAH_PILIH))
        assertEquals("Pilih alasan koreksi.", MesinP3K.validasiKoreksi(lama, KategoriKeluhan.MEMAR, "Tungkai bawah kiri", null))
        assertNull(MesinP3K.validasiKoreksi(lama, KategoriKeluhan.MEMAR, "Tungkai bawah kiri", AlasanKoreksi.SALAH_PILIH))
        assertEquals("lengan" to "tungkai", MesinP3K.selisih("Lengan", "Tungkai"))
    }

    @Test
    fun tindakLanjutTerjadwalHarusDiMasaDepan() {
        assertNull(MesinP3K.validasiTindakLanjut("Luka dicek ulang", StatusTindak.SELESAI, sekarang, sekarang))
        assertNotNull(MesinP3K.validasiTindakLanjut("Foto ulang", StatusTindak.TERJADWAL, sekarang - 1, sekarang))
        assertNull(MesinP3K.validasiTindakLanjut("Foto ulang", StatusTindak.TERJADWAL, sekarang + hari, sekarang))
        assertNotNull(MesinP3K.validasiTindakLanjut("ok", StatusTindak.SELESAI, sekarang, sekarang))
    }

    @Test
    fun statusDanPustaka() {
        assertEquals(StatusDokumentasi.BARU, foto().status)
        assertEquals(StatusDokumentasi.DITANGANI, foto(ditangani = true).status)
        assertEquals(StatusDokumentasi.DIKOREKSI, foto(ditangani = true, versi = 2).status)
        assertEquals(StatusDokumentasi.PERLU_DOKTER, foto(perluDokter = true, versi = 2).status)
        assertEquals(2, PustakaSop.dalam(KelompokSop.LUKA_KULIT).size)
        assertEquals(2, PustakaSop.dalam(KelompokSop.MEMAR_BENGKAK).size)
        assertTrue(PustakaSop.semua.all { it.langkah.size == 5 && it.tandaBahaya.isNotEmpty() })
        assertEquals(listOf(PustakaSop.JATUH), PustakaSop.cariTeks("kesadaran"))
        assertEquals("Sinta", namaPetugas("sinta.dewi@panti.id"))
        assertEquals("OH", inisial("Opa Herman"))
    }
}
