package com.example.rawatkasih.feature.obat.model

internal enum class StatusJadwal {
    BELUM_DIKONFIRMASI,
    DIMINUM,
    DILEWATI
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
    val status: StatusJadwal = StatusJadwal.BELUM_DIKONFIRMASI
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

    val stokMenipis: Boolean
        get() = stok <= batasStok

    fun catat(jam: String, status: StatusJadwal): RencanaObat {
        val dosis = jadwalBerikutnya
        if (dosis == null || dosis.jam != jam || status == StatusJadwal.BELUM_DIKONFIRMASI) {
            return this
        }

        if (status == StatusJadwal.DIMINUM && stok == 0) return this

        return copy(
            stok = if (status == StatusJadwal.DIMINUM) stok - 1 else stok,
            jadwal = jadwal.map { if (it.jam == jam) it.copy(status = status) else it }
        )
    }
}
