package com.example.rawatkasih.feature.galeri.notification

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
import com.example.rawatkasih.feature.galeri.model.FotoBaru
import com.example.rawatkasih.feature.galeri.model.FotoFisik

/**
 * Fungsi 4 · notifikasi lokal (flowchart 2.2.4):
 * - Pengelola panti: "Kirim alert darurat" saat ada dokumentasi baru berstatus perlu dokter.
 * - Keluarga: "Notifikasi portal keluarga" saat ada dokumentasi fisik baru untuk lansianya.
 * Notifikasi muncul selama aplikasi berjalan (paket Spark tidak memakai Cloud Functions/FCM).
 */
internal class AndroidNotifikasiFisik(context: Context) {
    private val appContext = context.applicationContext
    private val manager = NotificationManagerCompat.from(appContext)
    private var peranTerakhir: String? = null
    private var dilihat: Set<String>? = null

    /** [daftar] = alert perlu dokter (admin) atau semua dokumentasi (keluarga). Pemeriksaan pertama hanya mencatat. */
    fun periksa(peran: String?, daftar: List<FotoFisik>, namaLansia: (String) -> String) {
        if (peran != peranTerakhir) {
            peranTerakhir = peran
            dilihat = null
        }
        val baru = FotoBaru.cari(dilihat, daftar)
        dilihat = dilihat.orEmpty() + daftar.map { it.id }
        if (baru.isEmpty() || !izinAktif()) return

        val pertama = baru.first()
        val nama = namaLansia(pertama.lansiaId)
        val (judul, pesan) = when (peran) {
            "ADMIN" -> (if (baru.size == 1) "Alert perlu dokter · $nama" else "${baru.size} alert perlu dokter") to
                (pertama.alasanDokter.firstOrNull() ?: "${pertama.kategori.label} di ${pertama.lokasi.lowercase()}.")
            else -> (if (baru.size == 1) "Kabar kondisi fisik · $nama" else "${baru.size} dokumentasi fisik baru") to
                "${pertama.kategori.label} di ${pertama.lokasi.lowercase()}" +
                if (pertama.perluDokter) ". Petugas sudah menghubungi perawat/dokter." else ". Sudah ditangani sesuai SOP panti."
        }
        siapkanChannel()
        val notifikasi = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(judul)
            .setContentText(pesan)
            .setStyle(NotificationCompat.BigTextStyle().bigText(pesan))
            .setPriority(if (peran == "ADMIN") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(ID_NOTIF, notifikasi)
        } catch (_: SecurityException) {
            // Izin notifikasi dicabut saat runtime.
        }
    }

    private fun izinAktif(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun siapkanChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val sistem = appContext.getSystemService(NotificationManager::class.java) ?: return
        if (sistem.getNotificationChannel(CHANNEL_ID) != null) return
        sistem.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Galeri fisik & P3K", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alert dokumentasi fisik yang perlu dokter dan kabar kondisi fisik lansia untuk keluarga."
            }
        )
    }

    private companion object {
        const val CHANNEL_ID = "rawat_kasih_galeri_fisik"
        const val ID_NOTIF = 1004
    }
}
