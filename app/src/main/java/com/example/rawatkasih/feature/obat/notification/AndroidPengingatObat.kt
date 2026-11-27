package com.example.rawatkasih.feature.obat.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.rawatkasih.R
import com.example.rawatkasih.feature.obat.model.RencanaObat
import com.example.rawatkasih.feature.obat.model.StatusStok

internal class AndroidPengingatObat(context: Context) : PengingatObat {
    private val appContext = context.applicationContext
    private val manager = NotificationManagerCompat.from(appContext)
    private var kunciJadwalTerakhir: String? = null
    private var kunciStokTerakhir: String? = null

    override fun periksaDanIngatkan(rencana: RencanaObat) {
        if (!rencana.bisaMengubah || !izinNotifikasiAktif()) return
        siapkanChannel()

        val jadwal = rencana.jadwalBerikutnya
        if (jadwal != null) {
            val kunciJadwal = "${rencana.namaLansia}:${jadwal.jam}"
            if (kunciJadwal != kunciJadwalTerakhir) {
                kunciJadwalTerakhir = kunciJadwal
                kirim(
                    id = ID_NOTIF_JADWAL,
                    judul = "Pengingat Obat · ${rencana.namaLansia}",
                    pesan = "Jadwal ${jadwal.jam} (${jadwal.waktu}) belum dikonfirmasi.",
                    prioritas = NotificationCompat.PRIORITY_DEFAULT
                )
            }
        } else {
            kunciJadwalTerakhir = null
            manager.cancel(ID_NOTIF_JADWAL)
        }

        val kunciStok = "${rencana.namaLansia}:${rencana.statusStok}:${rencana.stok}"
        // Lansia tanpa jadwal obat belum membutuhkan stok, jadi tidak perlu peringatan restock.
        when (rencana.statusStok.takeIf { rencana.jadwal.isNotEmpty() } ?: StatusStok.AMAN) {
            StatusStok.HABIS -> if (kunciStok != kunciStokTerakhir) {
                kunciStokTerakhir = kunciStok
                kirim(
                    id = ID_NOTIF_STOK,
                    judul = "Darurat Restock · ${rencana.namaLansia}",
                    pesan = "Stok obat habis (0 dosis). Segera lakukan pengisian ulang.",
                    prioritas = NotificationCompat.PRIORITY_HIGH
                )
            }
            StatusStok.MENIPIS -> if (kunciStok != kunciStokTerakhir) {
                kunciStokTerakhir = kunciStok
                kirim(
                    id = ID_NOTIF_STOK,
                    judul = "Peringatan Stok Menipis · ${rencana.namaLansia}",
                    pesan = "Sisa ${rencana.stok} dosis (batas minimum ${rencana.batasStok}). Ajukan restock.",
                    prioritas = NotificationCompat.PRIORITY_DEFAULT
                )
            }
            StatusStok.AMAN -> {
                kunciStokTerakhir = null
                manager.cancel(ID_NOTIF_STOK)
            }
        }
    }

    private fun kirim(id: Int, judul: String, pesan: String, prioritas: Int) {
        val notifikasi = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(judul)
            .setContentText(pesan)
            .setStyle(NotificationCompat.BigTextStyle().bigText(pesan))
            .setPriority(prioritas)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(id, notifikasi)
        } catch (_: SecurityException) {
            // Izin notifikasi dicabut saat runtime.
        }
    }

    private fun izinNotifikasiAktif(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun siapkanChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val systemManager = appContext.getSystemService(NotificationManager::class.java) ?: return
        if (systemManager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pengingat & Stok Obat",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Pengingat jadwal minum obat lansia dan peringatan restock."
        }
        systemManager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "rawat_kasih_obat"
        const val ID_NOTIF_JADWAL = 1001
        const val ID_NOTIF_STOK = 1002
    }
}
