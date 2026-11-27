package com.example.rawatkasih.feature.galeri.model

/** Kelompok di layar 53 "SOP pertolongan pertama · Per kategori keluhan". */
internal enum class KelompokSop(val label: String) {
    LUKA_KULIT("Luka kulit"),
    MEMAR_BENGKAK("Memar dan bengkak"),
    JATUH("Jatuh"),
    SUHU("Suhu tubuh di luar rentang")
}

/**
 * Tanda bahaya dari banner "Segera hubungi perawat atau dokter" tiap SOP. [kataKunci] dipakai mesin aturan untuk
 * mendeteksi tanda dari teks observasi (dengan pengecekan negasi: "tidak berdarah" tidak dihitung).
 */
internal enum class TandaBahaya(val label: String, val kataKunci: List<String>) {
    LUKA_DALAM("Luka dalam atau menganga", listOf("luka dalam", "menganga", "robek lebar", "sobek lebar")),
    BERDARAH_TERUS("Berdarah terus", listOf("berdarah terus", "darah tidak berhenti", "perdarahan")),
    TIDAK_SEHAT("Lansia tampak tidak sehat atau lemas", listOf("lemas", "pucat", "lesu", "tampak tidak sehat")),
    KULIT_TERBUKA("Kulit terbuka, kehitaman, atau melepuh", listOf("kulit terbuka", "kehitaman", "melepuh", "lepuh")),
    INFEKSI("Bernanah, berbau, atau makin merah", listOf("nanah", "bernanah", "berbau", "makin merah")),
    DEMAM("Demam", listOf("demam", "menggigil", "panas tinggi")),
    MEMBESAR_CEPAT("Membesar cepat", listOf("membesar", "makin besar", "bertambah besar")),
    SANGAT_NYERI("Sangat nyeri", listOf("sangat nyeri", "nyeri hebat", "kesakitan")),
    SETELAH_JATUH("Terjadi setelah jatuh", listOf("jatuh", "terjatuh", "terpeleset", "terbentur")),
    PINGSAN("Pingsan atau tidak sadar", listOf("pingsan", "tidak sadar", "tak sadar")),
    TIDAK_BISA_BERGERAK("Tidak bisa bergerak", listOf("tidak bisa bergerak", "tidak bisa berdiri", "tidak bisa bangun")),
    SESAK("Sesak napas", listOf("sesak")),
    SATU_TUNGKAI("Hanya satu tungkai, hangat dan nyeri", listOf("satu tungkai", "sebelah kaki", "satu kaki")),
    SUHU_MENETAP("Suhu tetap di luar rentang", listOf("suhu tetap", "masih demam", "tetap tinggi", "tetap rendah"))
}

internal data class SopP3K(
    val id: String,
    val kelompok: KelompokSop,
    /** Label kategori pendek untuk top bar "SOP · Luka kulit" dan overline. */
    val labelKategori: String,
    val judul: String,
    val langkah: List<String>,
    /** Isi banner "Segera hubungi perawat atau dokter". */
    val peringatan: String,
    val tandaBahaya: List<TandaBahaya>
)

/**
 * Basis data SOP panti. Empat SOP (lecet, memar, jatuh, suhu) mengikuti isi layar 54–57 di Figma; luka tekan dan
 * bengkak melengkapi kelompok yang di layar 53 tertulis "2 SOP". Isinya contoh referensi yang harus divalidasi
 * tenaga kesehatan panti, sesuai banner di layar 53.
 */
internal object PustakaSop {
    private const val FOTO = "Foto dan catat di Galeri fisik."
    private const val LAPOR = "Laporkan ke perawat penanggung jawab."

    val LECET = SopP3K(
        "lecet", KelompokSop.LUKA_KULIT, "Luka kulit", "Luka lecet ringan",
        listOf("Cuci tangan dan kenakan sarung tangan.", "Bilas luka dengan air bersih mengalir.", "Tutup dengan kasa bersih.", FOTO, LAPOR),
        "Jika luka dalam, berdarah terus, atau lansia tampak tidak sehat.",
        listOf(TandaBahaya.LUKA_DALAM, TandaBahaya.BERDARAH_TERUS, TandaBahaya.TIDAK_SEHAT)
    )
    val LUKA_TEKAN = SopP3K(
        "luka-tekan", KelompokSop.LUKA_KULIT, "Luka kulit", "Luka tekan awal",
        listOf("Ubah posisi lansia setiap 2 jam.", "Bebaskan area yang merah dari tekanan dengan bantal.", "Jaga kulit tetap bersih dan kering.", FOTO, LAPOR),
        "Jika kulit terbuka atau kehitaman, bernanah, atau lansia demam.",
        listOf(TandaBahaya.KULIT_TERBUKA, TandaBahaya.INFEKSI, TandaBahaya.DEMAM)
    )
    val MEMAR = SopP3K(
        "memar", KelompokSop.MEMAR_BENGKAK, "Memar", "Memar ringan",
        listOf("Bantu lansia duduk atau berbaring nyaman.", "Kompres dingin berlapis kain 10–15 menit.", "Tinggikan bagian yang memar bila nyaman.", FOTO, LAPOR),
        "Jika bengkak membesar cepat, sangat nyeri, atau terjadi setelah jatuh.",
        listOf(TandaBahaya.MEMBESAR_CEPAT, TandaBahaya.SANGAT_NYERI, TandaBahaya.SETELAH_JATUH)
    )
    val BENGKAK = SopP3K(
        "bengkak", KelompokSop.MEMAR_BENGKAK, "Bengkak", "Bengkak ringan",
        listOf("Bantu lansia duduk atau berbaring nyaman.", "Tinggikan bagian yang bengkak bila nyaman.", "Bandingkan dengan sisi tubuh yang lain.", FOTO, LAPOR),
        "Jika hanya satu tungkai yang hangat dan nyeri, membesar cepat, atau lansia sesak.",
        listOf(TandaBahaya.SATU_TUNGKAI, TandaBahaya.MEMBESAR_CEPAT, TandaBahaya.SESAK)
    )
    val JATUH = SopP3K(
        "jatuh", KelompokSop.JATUH, "Jatuh", "Lansia terjatuh",
        listOf(
            "Jangan langsung diangkat. Tanyakan kondisinya.", "Panggil perawat penanggung jawab.",
            "Periksa kesadaran, napas, dan perdarahan.", "Bantu duduk perlahan hanya jika aman.", "Catat waktu, lokasi, dan foto kondisi fisik."
        ),
        "Jika lansia pingsan, nyeri hebat, atau tidak bisa bergerak.",
        listOf(TandaBahaya.PINGSAN, TandaBahaya.SANGAT_NYERI, TandaBahaya.TIDAK_BISA_BERGERAK)
    )
    val SUHU = SopP3K(
        "suhu", KelompokSop.SUHU, "Suhu", "Suhu di luar rentang",
        listOf(
            "Ukur ulang suhu setelah 10–15 menit.", "Pastikan lansia nyaman dan tidak kepanasan.", "Catat nilai dan waktu di Pantau vital.",
            LAPOR, "Pantau ulang sesuai arahan perawat."
        ),
        "Jika suhu tetap di luar rentang atau lansia tampak lemas.",
        listOf(TandaBahaya.SUHU_MENETAP, TandaBahaya.TIDAK_SEHAT)
    )

    val semua = listOf(LECET, LUKA_TEKAN, MEMAR, BENGKAK, JATUH, SUHU)

    fun cari(id: String?): SopP3K? = semua.firstOrNull { it.id == id }

    fun dalam(kelompok: KelompokSop) = semua.filter { it.kelompok == kelompok }

    /** SOP pertolongan darurat sementara (flowchart 2.2.4) untuk keparahan Parah/Darurat, sambil menunggu dokter. */
    val DARURAT_SEMENTARA = listOf(
        "Tetap dampingi lansia, jangan ditinggal sendirian.",
        "Hubungi perawat jaga atau dokter. Pengelola panti menerima alert dokumentasi ini.",
        "Bila lansia tidak sadar, sesak berat, atau perdarahan hebat: hubungi ambulans 119.",
        "Jangan pindahkan lansia bila curiga patah tulang atau cedera kepala.",
        "Catat perubahan kondisi sampai dokter atau perawat datang."
    )

    /** Pencarian layar 53: cocok dengan judul, kategori, langkah, atau tanda bahaya. */
    fun cariTeks(kueri: String): List<SopP3K> {
        val q = kueri.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return semua.filter { sop ->
            listOf(sop.judul, sop.kelompok.label, sop.labelKategori, sop.peringatan).plus(sop.langkah)
                .any { it.lowercase().contains(q) }
        }
    }
}
