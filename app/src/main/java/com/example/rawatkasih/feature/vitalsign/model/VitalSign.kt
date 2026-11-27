package com.example.rawatkasih.feature.vitalsign.model

import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Fungsi 3 · Monitoring & Analisis Tren Vital Sign Medis.
 * Menentukan status risiko klinis, validasi format medis, dan agregasi tren 7 hari.
 */
internal enum class StatusRisiko(val label: String) {
    NORMAL("Normal"),
    WASPADA("Waspada"),
    BAHAYA("Bahaya")
}

internal data class CatatanVitalSign(
    val id: String = "",
    val lansiaId: String,
    val sistol: Int,
    val diastol: Int,
    val gulaDarah: Int,
    val statusRisiko: StatusRisiko,
    val penulisId: String,
    val penulisNama: String,
    val waktu: Long
)

internal data class RingkasanTrenVital(
    val terbaru: CatatanVitalSign?,
    /** Titik pengukuran 7 hari terakhir, urut kronologis (terlama ke terbaru) untuk grafik. */
    val titik7Hari: List<CatatanVitalSign>,
    val adaHistori7Hari: Boolean,
    val rataSistol: Int?,
    val rataDiastol: Int?,
    val rataGulaDarah: Int?,
    val deltaSistol: Int?,
    val deltaDiastol: Int?,
    val deltaGulaDarah: Int?,
    val adaPerubahanMendadak: Boolean,
    val statusEfektif: StatusRisiko,
    val daftarPeringatan: List<String>
) {
    val perluAlertKeluarga: Boolean
        get() = terbaru != null && statusEfektif != StatusRisiko.NORMAL
}

internal object AnalisisVitalSign {
    // Rentang input medis yang masih masuk akal secara fisiologis.
    const val SISTOL_MIN = 50
    const val SISTOL_MAX = 300
    const val DIASTOL_MIN = 30
    const val DIASTOL_MAX = 200
    const val GULA_DARAH_MIN = 20
    const val GULA_DARAH_MAX = 600

    // Clinical threshold sesuai rancangan (Normal: TD 90/60–140/90, GD 70–200).
    const val SISTOL_NORMAL_MIN = 90
    const val SISTOL_NORMAL_MAX = 140
    const val DIASTOL_NORMAL_MIN = 60
    const val DIASTOL_NORMAL_MAX = 90
    const val GULA_NORMAL_MIN = 70
    const val GULA_NORMAL_MAX = 200

    // Ambang kondisi kritis (Bahaya).
    const val SISTOL_BAHAYA_TINGGI = 160
    const val SISTOL_BAHAYA_RENDAH = 80
    const val DIASTOL_BAHAYA_TINGGI = 100
    const val DIASTOL_BAHAYA_RENDAH = 50
    const val GULA_BAHAYA_TINGGI = 250
    const val GULA_BAHAYA_RENDAH = 60

    // Ambang perubahan mendadak dibanding pengukuran sebelumnya.
    const val LONJAKAN_SISTOL = 20
    const val LONJAKAN_DIASTOL = 15
    const val LONJAKAN_GULA = 50

    /** Mengembalikan pesan kesalahan jika input teks tidak valid, atau null jika siap disimpan. */
    fun validasiInput(sistolTeks: String, diastolTeks: String, gulaDarahTeks: String): String? {
        val sistol = sistolTeks.trim().toIntOrNull()
            ?: return "Nilai tidak valid: isi tekanan sistol dengan angka bulat."
        val diastol = diastolTeks.trim().toIntOrNull()
            ?: return "Nilai tidak valid: isi tekanan diastol dengan angka bulat."
        val gulaDarah = gulaDarahTeks.trim().toIntOrNull()
            ?: return "Nilai tidak valid: isi gula darah dengan angka bulat."
        return validasiNilai(sistol, diastol, gulaDarah)
    }

    /** Memvalidasi rentang medis dan memastikan Sistol > Diastol. */
    fun validasiNilai(sistol: Int, diastol: Int, gulaDarah: Int): String? = when {
        sistol !in SISTOL_MIN..SISTOL_MAX ->
            "Nilai tidak valid: sistol harus berada di rentang $SISTOL_MIN–$SISTOL_MAX mmHg."
        diastol !in DIASTOL_MIN..DIASTOL_MAX ->
            "Nilai tidak valid: diastol harus berada di rentang $DIASTOL_MIN–$DIASTOL_MAX mmHg."
        sistol <= diastol ->
            "Nilai tidak valid: tekanan sistol harus lebih besar dari diastol."
        gulaDarah !in GULA_DARAH_MIN..GULA_DARAH_MAX ->
            "Nilai tidak valid: gula darah harus berada di rentang $GULA_DARAH_MIN–$GULA_DARAH_MAX mg/dL."
        else -> null
    }

    /** Menentukan tingkat risiko klinis dari satu pengukuran vital sign. */
    fun klasifikasiRisiko(sistol: Int, diastol: Int, gulaDarah: Int): StatusRisiko = when {
        sistol >= SISTOL_BAHAYA_TINGGI || sistol < SISTOL_BAHAYA_RENDAH ||
            diastol >= DIASTOL_BAHAYA_TINGGI || diastol < DIASTOL_BAHAYA_RENDAH ||
            gulaDarah >= GULA_BAHAYA_TINGGI || gulaDarah < GULA_BAHAYA_RENDAH -> StatusRisiko.BAHAYA
        sistol > SISTOL_NORMAL_MAX || sistol < SISTOL_NORMAL_MIN ||
            diastol > DIASTOL_NORMAL_MAX || diastol < DIASTOL_NORMAL_MIN ||
            gulaDarah > GULA_NORMAL_MAX || gulaDarah < GULA_NORMAL_MIN -> StatusRisiko.WASPADA
        else -> StatusRisiko.NORMAL
    }

    /** Menguraikan parameter mana saja yang melewati ambang batas medis. */
    fun rincianAbnormal(sistol: Int, diastol: Int, gulaDarah: Int): List<String> = buildList {
        when {
            sistol >= SISTOL_BAHAYA_TINGGI || diastol >= DIASTOL_BAHAYA_TINGGI ->
                add("Tekanan darah sangat tinggi ($sistol/$diastol mmHg). Perlu perhatian medis segera.")
            sistol > SISTOL_NORMAL_MAX || diastol > DIASTOL_NORMAL_MAX ->
                add("Tekanan darah di atas batas normal ($sistol/$diastol mmHg > 140/90).")
            sistol < SISTOL_BAHAYA_RENDAH || diastol < DIASTOL_BAHAYA_RENDAH ->
                add("Tekanan darah sangat rendah ($sistol/$diastol mmHg). Perlu pemeriksaan segera.")
            sistol < SISTOL_NORMAL_MIN || diastol < DIASTOL_NORMAL_MIN ->
                add("Tekanan darah di bawah batas normal ($sistol/$diastol mmHg < 90/60).")
        }
        when {
            gulaDarah >= GULA_BAHAYA_TINGGI ->
                add("Gula darah sangat tinggi ($gulaDarah mg/dL). Waspadai hiperglikemia.")
            gulaDarah > GULA_NORMAL_MAX ->
                add("Gula darah di atas batas normal ($gulaDarah mg/dL > 200).")
            gulaDarah < GULA_BAHAYA_RENDAH ->
                add("Gula darah sangat rendah ($gulaDarah mg/dL). Waspadai hipoglikemia segera.")
            gulaDarah < GULA_NORMAL_MIN ->
                add("Gula darah di bawah batas normal ($gulaDarah mg/dL < 70).")
        }
    }

    /**
     * Menghitung grafik tren 7 hari, rata-rata & delta tren (jika histori >= 7 hari atau minimal 7 pengukuran),
     * deteksi perubahan mendadak, dan status risiko efektif.
     */
    fun analisisTren7Hari(
        riwayat: List<CatatanVitalSign>,
        sekarang: Long = System.currentTimeMillis()
    ): RingkasanTrenVital {
        if (riwayat.isEmpty()) {
            return RingkasanTrenVital(
                terbaru = null,
                titik7Hari = emptyList(),
                adaHistori7Hari = false,
                rataSistol = null,
                rataDiastol = null,
                rataGulaDarah = null,
                deltaSistol = null,
                deltaDiastol = null,
                deltaGulaDarah = null,
                adaPerubahanMendadak = false,
                statusEfektif = StatusRisiko.NORMAL,
                daftarPeringatan = emptyList()
            )
        }

        val urut = riwayat.sortedBy { it.waktu }
        val terbaru = urut.last()
        val batas7Hari = sekarang - TimeUnit.DAYS.toMillis(7)
        val dalam7Hari = urut.filter { it.waktu >= batas7Hari }
        val titikGrafik = if (dalam7Hari.isNotEmpty()) dalam7Hari else listOf(terbaru)

        // Histori dianggap memenuhi analisis tren 7 hari jika ada >= 7 catatan dalam 7 hari
        // atau rentang pengukuran menjangkau minimal 6 hari (hari ke-1 s.d. hari ke-7).
        val rentangCukup = titikGrafik.size >= 2 &&
            (titikGrafik.last().waktu - titikGrafik.first().waktu) >= TimeUnit.DAYS.toMillis(6)
        val adaHistori7Hari = titikGrafik.size >= 7 || rentangCukup

        val rataSistol: Int?
        val rataDiastol: Int?
        val rataGulaDarah: Int?
        val deltaSistol: Int?
        val deltaDiastol: Int?
        val deltaGulaDarah: Int?

        if (adaHistori7Hari) {
            val avgS = titikGrafik.map { it.sistol }.average().roundToInt()
            val avgD = titikGrafik.map { it.diastol }.average().roundToInt()
            val avgG = titikGrafik.map { it.gulaDarah }.average().roundToInt()
            rataSistol = avgS
            rataDiastol = avgD
            rataGulaDarah = avgG
            deltaSistol = terbaru.sistol - avgS
            deltaDiastol = terbaru.diastol - avgD
            deltaGulaDarah = terbaru.gulaDarah - avgG
        } else {
            rataSistol = null
            rataDiastol = null
            rataGulaDarah = null
            deltaSistol = null
            deltaDiastol = null
            deltaGulaDarah = null
        }

        // Deteksi perubahan mendadak dibanding pengukuran tepat sebelumnya.
        val sebelumnya = if (urut.size >= 2) urut[urut.size - 2] else null
        val lonjakanSistol = sebelumnya != null && abs(terbaru.sistol - sebelumnya.sistol) >= LONJAKAN_SISTOL
        val lonjakanDiastol = sebelumnya != null && abs(terbaru.diastol - sebelumnya.diastol) >= LONJAKAN_DIASTOL
        val lonjakanGula = sebelumnya != null && abs(terbaru.gulaDarah - sebelumnya.gulaDarah) >= LONJAKAN_GULA
        val adaPerubahanMendadak = lonjakanSistol || lonjakanDiastol || lonjakanGula

        val risikoKlinis = klasifikasiRisiko(terbaru.sistol, terbaru.diastol, terbaru.gulaDarah)
        val statusEfektif = if (risikoKlinis == StatusRisiko.NORMAL && adaPerubahanMendadak) {
            StatusRisiko.WASPADA
        } else {
            risikoKlinis
        }

        val peringatan = buildList {
            addAll(rincianAbnormal(terbaru.sistol, terbaru.diastol, terbaru.gulaDarah))
            if (sebelumnya != null) {
                if (lonjakanSistol || lonjakanDiastol) {
                    val bedaS = terbaru.sistol - sebelumnya.sistol
                    val arah = if (bedaS >= 0) "naik" else "turun"
                    add("Perubahan mendadak tekanan darah ($arah ${abs(bedaS)} mmHg dibanding pengukuran sebelumnya).")
                }
                if (lonjakanGula) {
                    val bedaG = terbaru.gulaDarah - sebelumnya.gulaDarah
                    val arah = if (bedaG >= 0) "naik" else "turun"
                    add("Perubahan mendadak gula darah ($arah ${abs(bedaG)} mg/dL dibanding pengukuran sebelumnya).")
                }
            }
        }

        return RingkasanTrenVital(
            terbaru = terbaru,
            titik7Hari = titikGrafik,
            adaHistori7Hari = adaHistori7Hari,
            rataSistol = rataSistol,
            rataDiastol = rataDiastol,
            rataGulaDarah = rataGulaDarah,
            deltaSistol = deltaSistol,
            deltaDiastol = deltaDiastol,
            deltaGulaDarah = deltaGulaDarah,
            adaPerubahanMendadak = adaPerubahanMendadak,
            statusEfektif = statusEfektif,
            daftarPeringatan = peringatan
        )
    }
}
