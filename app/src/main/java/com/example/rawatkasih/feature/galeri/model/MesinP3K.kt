package com.example.rawatkasih.feature.galeri.model

import java.util.concurrent.TimeUnit

/** Hasil mesin aturan untuk satu dokumentasi. */
internal data class Rekomendasi(
    /** SOP utama yang ditautkan (banner "Ikuti SOP …" di layar 51), atau null bila tidak ada yang cocok. */
    val sop: SopP3K?,
    /** SOP tambahan yang ikut disarankan, mis. memar setelah jatuh → SOP lansia terjatuh. */
    val sopLain: SopP3K?,
    /** Tanda bahaya yang ditemukan dari teks observasi/lokasi (selain yang dicentang). */
    val tandaTerdeteksi: Set<TandaBahaya>,
    val perluDokter: Boolean,
    val alasan: List<String>,
    /** Keparahan Parah/Darurat: tampilkan SOP pertolongan darurat sementara ([PustakaSop.DARURAT_SEMENTARA]). */
    val darurat: Boolean = false
)

internal enum class BidangForm { LANSIA, FOTO, KATEGORI, KEPARAHAN, LOKASI, OBSERVASI }

/**
 * Rule-based engine Fungsi 4 (flowchart laporan 2.2.4). Aturannya:
 * 1. Kategori + kata kunci memilih SOP: luka kulit di area tumpuan (bokong, tumit, …) atau menyebut "tekan" →
 *    SOP luka tekan, selain itu luka lecet; memar → memar; bengkak → bengkak; "lainnya" → jatuh/suhu bila teks
 *    menyebutnya.
 * 2. Keparahan Ringan/Sedang → SOP penanganan awal mandiri; Parah/Darurat → "Perlu dokter" + SOP darurat sementara.
 *    Tanda bahaya yang dicentang ATAU disebut di observasi (tanpa negasi) juga → "Perlu dokter".
 * 3. Kondisi yang sama (lansia, kategori, lokasi) sudah tercatat 2 kali dalam 7 hari dan belum ditangani → "Perlu dokter".
 * 4. Memar/bengkak yang terjadi setelah jatuh → SOP lansia terjatuh ikut disarankan.
 * "Perlu dokter" membuat dokumentasi tampil paling atas di panel pemantauan pengelola panti sampai ditangani.
 */
internal object MesinP3K {
    private val JENDELA = TimeUnit.DAYS.toMillis(7)
    const val BATAS_MENETAP = 3
    const val LOKASI_MAKS = 60
    const val OBSERVASI_MAKS = 500
    const val TINDAKAN_MAKS = 300
    const val FOTO_MAKS = 3

    private val AREA_TUMPUAN = listOf("tekan", "dekubitus", "bokong", "tumit", "punggung", "pinggul", "panggul", "sakrum", "tulang ekor")
    private val KATA_JATUH = TandaBahaya.SETELAH_JATUH.kataKunci
    private val KATA_SUHU = listOf("suhu", "demam", "panas", "menggigil", "hipotermia", "kedinginan")
    private val NEGASI = setOf("tidak", "tak", "tanpa", "bukan", "belum")

    fun pilihSop(kategori: KategoriKeluhan, lokasi: String, observasi: String): SopP3K? {
        val teks = "$lokasi $observasi".lowercase()
        return when (kategori) {
            KategoriKeluhan.LUKA_KULIT -> if (sebut(teks, AREA_TUMPUAN)) PustakaSop.LUKA_TEKAN else PustakaSop.LECET
            KategoriKeluhan.MEMAR -> PustakaSop.MEMAR
            KategoriKeluhan.BENGKAK -> PustakaSop.BENGKAK
            KategoriKeluhan.LAINNYA -> when {
                sebut(teks, KATA_JATUH) -> PustakaSop.JATUH
                sebut(teks, KATA_SUHU) -> PustakaSop.SUHU
                else -> null
            }
        }
    }

    /** Tanda bahaya yang disebut di teks, mengabaikan yang didahului kata negasi ("tidak berdarah terus"). */
    fun deteksiTanda(teks: String): Set<TandaBahaya> {
        val t = teks.lowercase()
        return TandaBahaya.entries.filter { sebut(t, it.kataKunci) }.toSet()
    }

    fun nilai(draf: DrafDokumentasi, riwayat: List<FotoFisik>, sekarang: Long = System.currentTimeMillis()): Rekomendasi? {
        val kategori = draf.kategori ?: return null
        return nilai(draf.lansiaId, kategori, draf.lokasi, draf.observasi, draf.tandaBahaya, riwayat, sekarang, draf.keparahan)
    }

    fun nilai(
        lansiaId: String?,
        kategori: KategoriKeluhan,
        lokasi: String,
        observasi: String,
        dicentang: Set<TandaBahaya>,
        riwayat: List<FotoFisik>,
        sekarang: Long = System.currentTimeMillis(),
        keparahan: Keparahan? = null
    ): Rekomendasi {
        val sop = pilihSop(kategori, lokasi, observasi)
        val terdeteksi = deteksiTanda("$lokasi. $observasi") - dicentang
        val alasan = mutableListOf<String>()
        if (keparahan?.butuhDokter == true) alasan += "Keparahan ${keparahan.label.lowercase()}: butuh penanganan dokter."
        dicentang.sortedBy { it.ordinal }.forEach { alasan += "Tanda bahaya: ${it.label.lowercase()}." }
        terdeteksi.sortedBy { it.ordinal }.forEach { alasan += "Observasi menyebut: ${it.label.lowercase()}." }

        val kunci = normal(lokasi)
        val serupa = riwayat.count {
            it.lansiaId == lansiaId && it.kategori == kategori && normal(it.lokasi) == kunci && kunci.isNotEmpty() &&
                !it.ditangani && sekarang - it.waktu in 0 until JENDELA
        }
        if (serupa + 1 >= BATAS_MENETAP) alasan += "Tercatat ${serupa + 1} kali dalam 7 hari dan belum ditangani."

        val semuaTanda = dicentang + terdeteksi
        val sopLain = PustakaSop.JATUH.takeIf {
            TandaBahaya.SETELAH_JATUH in semuaTanda && sop != PustakaSop.JATUH
        }
        return Rekomendasi(sop, sopLain, terdeteksi, alasan.isNotEmpty(), alasan, darurat = keparahan?.butuhDokter == true)
    }

    /** Galat per bidang form layar 51 (urutan sesuai flowchart: foto valid & kategori dipilih). */
    fun validasi(draf: DrafDokumentasi): Map<BidangForm, String> = buildMap {
        if (draf.lansiaId == null) put(BidangForm.LANSIA, "Pilih lansia.")
        if (draf.foto.isEmpty()) put(BidangForm.FOTO, "Tambahkan minimal satu foto.")
        if (draf.kategori == null) put(BidangForm.KATEGORI, "Pilih kategori keluhan.")
        if (draf.keparahan == null) put(BidangForm.KEPARAHAN, "Pilih tingkat keparahan.")
        when {
            draf.lokasi.isBlank() -> put(BidangForm.LOKASI, "Isi lokasi pada tubuh.")
            draf.lokasi.trim().length > LOKASI_MAKS -> put(BidangForm.LOKASI, "Maksimal $LOKASI_MAKS huruf.")
        }
        when {
            draf.observasi.trim().length < 5 -> put(BidangForm.OBSERVASI, "Tulis apa yang terlihat dan yang sudah dilakukan.")
            draf.observasi.trim().length > OBSERVASI_MAKS -> put(BidangForm.OBSERVASI, "Maksimal $OBSERVASI_MAKS huruf.")
        }
    }

    fun validasiTindakLanjut(tindakan: String, status: StatusTindak, waktu: Long, sekarang: Long): String? = when {
        tindakan.trim().length !in 3..TINDAKAN_MAKS -> "Tulis tindakan (3–$TINDAKAN_MAKS huruf)."
        status == StatusTindak.TERJADWAL && waktu <= sekarang -> "Pilih waktu jadwal yang akan datang."
        else -> null
    }

    /** Baris riwayat perubahan, mis. "Lokasi: kiri → kanan"; kosong berarti tidak ada yang berubah. */
    fun perubahan(lama: FotoFisik, kategori: KategoriKeluhan, lokasi: String): List<String> = buildList {
        if (kategori != lama.kategori) add("Kategori: ${lama.kategori.label} → ${kategori.label}")
        if (normal(lokasi) != normal(lama.lokasi)) {
            val (a, b) = selisih(lama.lokasi.trim(), lokasi.trim())
            add("Lokasi: $a → $b")
        }
    }

    fun validasiKoreksi(lama: FotoFisik, kategori: KategoriKeluhan, lokasi: String, alasan: AlasanKoreksi?): String? = when {
        lokasi.isBlank() -> "Isi lokasi pada tubuh."
        lokasi.trim().length > LOKASI_MAKS -> "Maksimal $LOKASI_MAKS huruf."
        perubahan(lama, kategori, lokasi).isEmpty() -> "Belum ada yang diubah."
        alasan == null -> "Pilih alasan koreksi."
        else -> null
    }

    /** Membuang kata awal dan akhir yang sama: "tungkai bawah kiri" vs "tungkai bawah kanan" → "kiri", "kanan". */
    fun selisih(a: String, b: String): Pair<String, String> {
        val x = a.lowercase().split(Regex("\\s+"))
        val y = b.lowercase().split(Regex("\\s+"))
        var awal = 0
        while (awal < x.size - 1 && awal < y.size - 1 && x[awal] == y[awal]) awal++
        var akhir = 0
        while (akhir < x.size - awal - 1 && akhir < y.size - awal - 1 && x[x.size - 1 - akhir] == y[y.size - 1 - akhir]) akhir++
        return x.subList(awal, x.size - akhir).joinToString(" ") to y.subList(awal, y.size - akhir).joinToString(" ")
    }

    private fun normal(s: String) = s.trim().lowercase().replace(Regex("\\s+"), " ")

    private fun sebut(teks: String, kata: List<String>): Boolean = kata.any { k -> adaTanpaNegasi(teks, k) }

    private fun adaTanpaNegasi(teks: String, kata: String): Boolean {
        var dari = 0
        while (true) {
            val i = teks.indexOf(kata, dari)
            if (i < 0) return false
            val batasKiri = i == 0 || !teks[i - 1].isLetter()
            if (batasKiri) {
                // Dua kata sebelum kata kunci dalam klausa yang sama (berhenti di tanda baca).
                val sebelum = teks.substring(0, i).substringAfterLast(',').substringAfterLast('.').substringAfterLast(';')
                val dua = sebelum.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.takeLast(2)
                if (dua.none { it in NEGASI }) return true
            }
            dari = i + kata.length
        }
    }
}
