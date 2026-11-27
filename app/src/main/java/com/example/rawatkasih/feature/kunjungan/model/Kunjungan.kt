package com.example.rawatkasih.feature.kunjungan.model

internal enum class StatusKunjungan(val label: String) {
    MENUNGGU("Menunggu persetujuan"),
    DISETUJUI("Disetujui"),
    DITOLAK("Ditolak"),
    DIBATALKAN("Dibatalkan");

    companion object {
        fun dari(nilai: String?): StatusKunjungan? = entries.firstOrNull { it.name == nilai }
    }
}

/**
 * Permintaan kunjungan keluarga ke satu lansia. [tanggal] "yyyy-MM-dd" dan [jam] "HH.mm" disimpan sebagai teks
 * supaya urutan teks sama dengan urutan waktu dan tidak bergeser karena zona waktu.
 */
internal data class Kunjungan(
    val id: String,
    val lansiaId: String,
    val tanggal: String,
    val jam: String,
    val jumlahOrang: Int,
    val catatan: String,
    val status: StatusKunjungan,
    val pengajuUid: String,
    val pengajuEmail: String,
    val alasan: String = "",
    val ditanganiOleh: String? = null,
) {
    val aktif get() = status == StatusKunjungan.MENUNGGU || status == StatusKunjungan.DISETUJUI
}

/** Aturan kunjungan (juga dijaga Firestore Rules): jam besuk tetap, 1–5 orang, paling jauh 30 hari ke depan. */
internal object AturanKunjungan {
    val JAM_BESUK = listOf("09.00", "10.00", "11.00", "13.00", "14.00", "15.00", "16.00")
    const val MAKS_ORANG = 5
    const val MAKS_HARI = 30
    const val MAKS_CATATAN = 200

    /** null = valid. [hariIni] dan [batasAkhir] berformat "yyyy-MM-dd". */
    fun validasi(tanggal: String?, jam: String?, jumlahOrang: Int, hariIni: String, batasAkhir: String): String? = when {
        tanggal == null -> "Pilih tanggal kunjungan."
        tanggal < hariIni -> "Tanggal itu sudah lewat."
        tanggal > batasAkhir -> "Kunjungan bisa diajukan paling jauh $MAKS_HARI hari ke depan."
        jam == null || jam !in JAM_BESUK -> "Pilih jam kunjungan."
        jumlahOrang !in 1..MAKS_ORANG -> "Jumlah pengunjung 1–$MAKS_ORANG orang."
        else -> null
    }

    /** Kunjungan yang masih berlaku dari hari ini, paling dekat lebih dulu. */
    fun akanDatang(daftar: List<Kunjungan>, hariIni: String): List<Kunjungan> =
        daftar.filter { it.aktif && it.tanggal >= hariIni }.sortedWith(compareBy({ it.tanggal }, { it.jam }))

    /** Riwayat: yang sudah lewat, ditolak, atau dibatalkan; terbaru lebih dulu. */
    fun riwayat(daftar: List<Kunjungan>, hariIni: String): List<Kunjungan> =
        (daftar - akanDatang(daftar, hariIni).toSet()).sortedWith(compareByDescending<Kunjungan> { it.tanggal }.thenByDescending { it.jam })

    /** Permintaan yang perlu ditanggapi pengelola (yang tanggalnya sudah lewat tidak perlu lagi). */
    fun menunggu(daftar: List<Kunjungan>, hariIni: String): List<Kunjungan> =
        akanDatang(daftar, hariIni).filter { it.status == StatusKunjungan.MENUNGGU }
}
