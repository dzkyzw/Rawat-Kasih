package com.example.rawatkasih.feature.obat.model

internal enum class StatusJadwal {
    BELUM_DIKONFIRMASI,
    DIMINUM,
    DILEWATI
}

internal fun statusHariIni(status: StatusJadwal, dicatatPada: Long?, sekarang: Long = System.currentTimeMillis()): StatusJadwal {
    if (status == StatusJadwal.BELUM_DIKONFIRMASI || dicatatPada == null) return status
    val tengahMalam = java.util.Calendar.getInstance().apply {
        timeInMillis = sekarang
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    return if (dicatatPada < tengahMalam) StatusJadwal.BELUM_DIKONFIRMASI else status
}

internal enum class StatusStok {
    AMAN,
    MENIPIS,
    HABIS
}

internal fun rencanaObatContoh() = RencanaObat(
    namaLansia = "Oma Siti",
    stok = 4,
    batasStok = 3,
    jadwal = listOf(
        JadwalObat("06.00", "Dini hari", StatusJadwal.DIMINUM),
        JadwalObat("08.00", "Pagi"),
        JadwalObat("12.00", "Siang")
    )
)

internal data class JadwalObat(
    val jam: String,
    val waktu: String,
    val status: StatusJadwal = StatusJadwal.BELUM_DIKONFIRMASI,
    val alasanPenolakan: String = ""
)

internal data class RencanaObat(
    val namaLansia: String,
    val stok: Int,
    val batasStok: Int,
    val jadwal: List<JadwalObat>,
    val bisaMengubah: Boolean = true
) {
    val jadwalBerikutnya: JadwalObat?
        get() = jadwal.firstOrNull { it.status == StatusJadwal.BELUM_DIKONFIRMASI }

    val riwayat: List<JadwalObat>
        get() = jadwal.filter { it.status != StatusJadwal.BELUM_DIKONFIRMASI }
            .asReversed()

    val statusStok: StatusStok
        get() = when {
            stok <= 0 -> StatusStok.HABIS
            stok <= batasStok -> StatusStok.MENIPIS
            else -> StatusStok.AMAN
        }

    val stokMenipis: Boolean
        get() = statusStok != StatusStok.AMAN

    fun catat(jam: String, status: StatusJadwal, alasan: String = ""): RencanaObat {
        val dosis = jadwalBerikutnya
        if (dosis == null || dosis.jam != jam || status == StatusJadwal.BELUM_DIKONFIRMASI) {
            return this
        }

        if (status == StatusJadwal.DIMINUM && stok <= 0) return this

        val catatanAlasan = if (status == StatusJadwal.DILEWATI) alasan.trim() else ""
        return copy(
            stok = if (status == StatusJadwal.DIMINUM) stok - 1 else stok,
            jadwal = jadwal.map {
                if (it.jam == jam) it.copy(status = status, alasanPenolakan = catatanAlasan) else it
            }
        )
    }
}
