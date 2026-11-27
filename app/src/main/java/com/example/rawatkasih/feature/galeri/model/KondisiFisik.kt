package com.example.rawatkasih.feature.galeri.model

import java.util.Locale

/**
 * Fungsi 4 · Dokumentasi galeri fisik & SOP pertolongan pertama (desain Figma "F · F4", layar 50–57 & 52x).
 * Dokumentasi tidak pernah dihapus: perubahan dicatat sebagai koreksi berversi beserta alasan, nama, dan waktu.
 */
internal enum class KategoriKeluhan(val label: String) {
    LUKA_KULIT("Luka kulit"),
    MEMAR("Memar"),
    BENGKAK("Bengkak"),
    LAINNYA("Lainnya")
}

/**
 * Tingkat keparahan (flowchart laporan 2.2.4): Ringan/Sedang → SOP penanganan awal mandiri;
 * Parah/Darurat → butuh penanganan dokter + SOP pertolongan darurat sementara.
 */
internal enum class Keparahan(val label: String, val keterangan: String) {
    RINGAN("Ringan", "Kulit utuh atau kemerahan, area kecil, lansia tetap nyaman."),
    SEDANG("Sedang", "Kulit terbuka dangkal, terasa nyeri, atau area cukup luas."),
    PARAH("Parah", "Luka dalam atau luas, bernanah, berbau, atau disertai demam."),
    DARURAT("Darurat", "Perdarahan tidak berhenti, sesak napas, atau penurunan kesadaran.");

    val butuhDokter get() = this >= PARAH
}

internal enum class StatusTindak(val label: String) {
    SELESAI("Selesai"),
    TERJADWAL("Terjadwal")
}

internal enum class AlasanKoreksi(val label: String) {
    SALAH_PILIH("Salah pilih"),
    SALAH_KETIK("Salah ketik"),
    INFO_TAMBAHAN("Info tambahan"),
    LAINNYA("Lainnya")
}

/** Status yang tampil sebagai pil di galeri dan detail. */
internal enum class StatusDokumentasi(val label: String) {
    PERLU_DOKTER("Perlu dokter"),
    DIKOREKSI("Dikoreksi"),
    DITANGANI("Ditangani"),
    BARU("Baru")
}

/** Satu dokumentasi kondisi fisik. [thumbnail] JPEG kecil Base64 untuk grid; foto penuh dimuat saat detail dibuka. */
internal data class FotoFisik(
    val id: String,
    val lansiaId: String,
    val kategori: KategoriKeluhan,
    val lokasi: String,
    val observasi: String,
    val thumbnail: String,
    val jumlahFoto: Int,
    val sopId: String?,
    val tandaBahaya: Set<TandaBahaya>,
    val perluDokter: Boolean,
    val alasanDokter: List<String>,
    val ditangani: Boolean,
    val versi: Int,
    val pengunggahId: String,
    val pengunggahEmail: String,
    val waktu: Long,
    /** null untuk dokumentasi lama yang dibuat sebelum ada pilihan keparahan. */
    val keparahan: Keparahan? = null
) {
    val status: StatusDokumentasi
        get() = when {
            perluDokter && !ditangani -> StatusDokumentasi.PERLU_DOKTER
            versi > 1 -> StatusDokumentasi.DIKOREKSI
            ditangani -> StatusDokumentasi.DITANGANI
            else -> StatusDokumentasi.BARU
        }
}

internal data class TindakLanjut(
    val id: String,
    val tindakan: String,
    val status: StatusTindak,
    val waktu: Long,
    val petugasEmail: String
)

/** Satu entri riwayat perubahan; [perubahan] berisi baris seperti "Lokasi: kiri → kanan". */
internal data class KoreksiDokumentasi(
    val id: String,
    val versi: Int,
    val perubahan: List<String>,
    val alasan: AlasanKoreksi,
    val olehEmail: String,
    val waktu: Long
)

/** Satu foto yang sudah dikompres: versi penuh untuk detail dan thumbnail untuk grid. */
internal data class FotoDraf(val penuh: String, val thumbnail: String, val waktu: Long)

internal data class DrafDokumentasi(
    val lansiaId: String? = null,
    val foto: List<FotoDraf> = emptyList(),
    val kategori: KategoriKeluhan? = null,
    val lokasi: String = "",
    val observasi: String = "",
    val tandaBahaya: Set<TandaBahaya> = emptySet(),
    val keparahan: Keparahan? = null
)

/**
 * Dokumentasi yang baru muncul sejak pemeriksaan terakhir, untuk notifikasi (alert pengelola & kabar keluarga).
 * [dilihat] = null berarti pemeriksaan pertama: semua dianggap sudah diketahui supaya tidak ada notifikasi massal.
 */
internal object FotoBaru {
    fun cari(dilihat: Set<String>?, daftar: List<FotoFisik>): List<FotoFisik> =
        if (dilihat == null) emptyList() else daftar.filter { it.id !in dilihat }
}

/** "sinta.dewi@panti.id" → "Sinta": nama petugas yang ditampilkan dari email akun. */
internal fun namaPetugas(email: String): String =
    email.substringBefore('@').split('.', '_', '-').firstOrNull { it.isNotBlank() }
        ?.replaceFirstChar { it.titlecase(Locale.ROOT) } ?: email

/** "Opa Herman" → "OH". */
internal fun inisial(nama: String): String =
    nama.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
