package com.example.rawatkasih.feature.penugasan.model

internal data class Lansia(val id: String, val nama: String)

/**
 * Pola shift panti: jam mulai (0–23) tiap shift, berurutan melingkar mulai dari shift pertama hari itu
 * (mis. 07, 15, 23). Shift ke-i berlangsung sampai jam mulai shift berikutnya; shift terakhir menyambung ke
 * shift pertama keesokan harinya. Jumlah shift bebas (1, 2, 3, atau lebih) sesuai kebijakan panti.
 */
internal data class PolaShift(val jamMulai: List<Int>) {
    init {
        require(jamMulai.isNotEmpty()) { "Minimal satu shift." }
        require(jamMulai.all { it in 0..23 }) { "Jam harus 0–23." }
        require(jamMulai.size == 1 || durasi().sum() == 24) { "Jam mulai harus berurutan melingkar dan tidak kembar." }
    }

    /** Lama tiap shift dalam jam. */
    fun durasi(): List<Int> = jamMulai.indices.map { i ->
        if (jamMulai.size == 1) 24 else (jamMulai[(i + 1) % jamMulai.size] - jamMulai[i] + 24) % 24
    }

    val jumlah get() = jamMulai.size

    fun nama(ke: Int): String = when (jumlah) {
        1 -> "Sepanjang hari"
        2 -> listOf("Pagi", "Malam")[ke]
        3 -> listOf("Pagi", "Siang", "Malam")[ke]
        4 -> listOf("Pagi", "Siang", "Sore", "Malam")[ke]
        else -> "Shift ${ke + 1}"
    }

    fun jamSelesai(ke: Int) = jamMulai[(ke + 1) % jumlah]

    // Satu shift menutup 24 jam penuh; "07.00–07.00" terbaca seperti salah.
    fun rentang(ke: Int) = if (jumlah == 1) "24 jam" else "${jam(jamMulai[ke])}–${jam(jamSelesai(ke))}"

    /** Shift yang sedang berjalan pada [jam] (0–23), termasuk shift yang melewati tengah malam. */
    fun shiftPada(jam: Int): Int {
        val d = durasi()
        return jamMulai.indices.first { i -> (jam - jamMulai[i] + 24) % 24 < d[i] }
    }

    fun berikutnya(ke: Int) = (ke + 1) % jumlah

    companion object {
        /** Membagi 24 jam sama rata mulai [mulai]; sisa jam masuk ke shift-shift awal. */
        fun bagiRata(jumlah: Int, mulai: Int = 7): PolaShift {
            require(jumlah in 1..8) { "Jumlah shift 1–8." }
            var jam = 0
            return PolaShift((0 until jumlah).map { i ->
                val awal = (mulai + jam) % 24
                jam += 24 / jumlah + if (i < 24 % jumlah) 1 else 0
                awal
            })
        }

        fun jam(j: Int) = "%02d.00".format(j)
    }
}

/**
 * Penugasan pramurukti (uid Firebase) ke lansia. [shiftKe] = null berarti penuh waktu (tidak bergantian shift).
 * [email] dipakai sebagai nama tampilan karena akun pramurukti hanya punya email.
 */
internal data class Penugasan(val lansiaId: String, val pramuruktiId: String, val shiftKe: Int?, val email: String = "") {
    val penuhWaktu get() = shiftKe == null
}

internal object AturanPenugasan {
    /** Pramurukti bertugas untuk lansia ini pada [shiftSekarang]: penuh waktu selalu, shift hanya pada jamnya. */
    fun sedangBertugas(p: Penugasan, shiftSekarang: Int) = p.penuhWaktu || p.shiftKe == shiftSekarang

    /**
     * Awal tugas yang sedang berjalan atau terakhir berjalan (epoch ms) untuk menilai apakah catatannya sudah
     * ditulis: jam mulai shift terakhir, atau tengah malam hari ini untuk penuh waktu.
     */
    fun awalTugas(tugas: Penugasan, pola: PolaShift, sekarang: java.util.Calendar): Long {
        val awal = sekarang.clone() as java.util.Calendar
        awal.set(java.util.Calendar.MINUTE, 0)
        awal.set(java.util.Calendar.SECOND, 0)
        awal.set(java.util.Calendar.MILLISECOND, 0)
        awal.set(java.util.Calendar.HOUR_OF_DAY, tugas.shiftKe?.let { pola.jamMulai[it] } ?: 0)
        if (awal.after(sekarang)) awal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        return awal.timeInMillis
    }

    /** Label shift untuk catatan yang ditulis pramurukti ini sekarang. */
    fun labelShift(tugas: Penugasan?, pola: PolaShift, shiftSekarang: Int): String =
        if (tugas == null || tugas.penuhWaktu) "Penuh waktu" else pola.nama(tugas.shiftKe ?: shiftSekarang)

    /** Siapa yang menerima catatan berikutnya untuk lansia ini, atau null bila dirawat penuh waktu/tidak ada. */
    fun penerusan(semua: List<Penugasan>, lansiaId: String, shiftSaya: Int?, pola: PolaShift): Penugasan? {
        val milikLansia = semua.filter { it.lansiaId == lansiaId }
        if (shiftSaya == null || milikLansia.any { it.penuhWaktu }) return null
        var ke = pola.berikutnya(shiftSaya)
        repeat(pola.jumlah - 1) {
            milikLansia.firstOrNull { it.shiftKe == ke }?.let { return it }
            ke = pola.berikutnya(ke)
        }
        return null
    }

    /**
     * Penugasan baru untuk satu lansia. Penuh waktu: tepat satu pramurukti. Shift: boleh kosong di sebagian
     * shift, tetapi minimal satu shift terisi.
     */
    fun validasi(penuhWaktu: Boolean, pramuruktiPenuh: String?, perShift: Map<Int, String>): String? = when {
        penuhWaktu && pramuruktiPenuh == null -> "Pilih pramurukti penuh waktu."
        !penuhWaktu && perShift.isEmpty() -> "Isi minimal satu shift."
        else -> null
    }
}
