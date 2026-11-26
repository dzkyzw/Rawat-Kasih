package com.example.rawatkasih.feature.catatan.model

import java.util.concurrent.TimeUnit

internal enum class Mood(val label: String) {
    TENANG("Tenang"),
    SENANG("Senang"),
    SEDIH("Sedih"),
    GELISAH("Gelisah")
}

internal data class CatatanShift(
    val id: String = "",
    val lansiaId: String,
    val penulisId: String,
    val penulisNama: String,
    /** Nama shift saat ditulis (mis. "Pagi") atau "Penuh waktu". */
    val shift: String,
    val mood: Mood,
    val isi: String,
    val waktu: Long,
    val dibacaOlehId: String? = null,
    val dibacaOlehNama: String? = null,
    val dibacaPada: Long? = null
) {
    val sudahDibaca: Boolean
        get() = dibacaOlehId != null
}

internal object Handover {
    /**
     * Catatan yang wajib dibaca pramurukti [sayaId] sebelum mulai bekerja: untuk lansia yang ditugaskan
     * kepadanya, ditulis orang lain (shift sebelumnya), belum dibaca, dan dari 24 jam terakhir supaya catatan
     * lama tidak mengunci selamanya. Pramurukti penuh waktu hanya menulis untuk dirinya, jadi tidak terkunci
     * oleh catatannya sendiri.
     */
    fun belumDibaca(
        catatan: List<CatatanShift>,
        sayaId: String,
        lansiaSaya: Set<String>,
        sekarang: Long = System.currentTimeMillis()
    ): List<CatatanShift> = catatan
        .filter {
            it.lansiaId in lansiaSaya && !it.sudahDibaca && it.penulisId != sayaId &&
                sekarang - it.waktu < TimeUnit.HOURS.toMillis(24)
        }
        .sortedBy { it.waktu }

    /** Mengembalikan pesan kesalahan, atau null bila catatan boleh disimpan. */
    fun validasi(mood: Mood?, isi: String): String? = when {
        mood == null -> "Pilih suasana hati lansia."
        isi.trim().length < 10 -> "Tulis catatan minimal 10 huruf agar shift berikutnya paham kondisinya."
        else -> null
    }
}
