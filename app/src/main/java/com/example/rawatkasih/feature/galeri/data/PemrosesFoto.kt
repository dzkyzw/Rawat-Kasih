package com.example.rawatkasih.feature.galeri.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import com.example.rawatkasih.feature.galeri.model.FotoDraf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/** Mengubah foto kamera/galeri HP menjadi JPEG penuh + thumbnail (Base64) yang muat di dokumen Firestore. */
internal interface PemrosesFoto {
    suspend fun proses(uri: Uri): FotoDraf
}

internal class FotoTerlaluBesar : IllegalStateException()

internal class AndroidPemrosesFoto(private val context: Context) : PemrosesFoto {
    override suspend fun proses(uri: Uri): FotoDraf = withContext(Dispatchers.IO) {
        val asli = baca(uri)
        try {
            FotoDraf(
                penuh = jpeg(asli, SISI_PENUH, BYTE_PENUH),
                thumbnail = jpeg(asli, SISI_THUMBNAIL, BYTE_THUMBNAIL),
                waktu = System.currentTimeMillis()
            )
        } finally {
            asli.recycle()
        }
    }

    /** Diperkecil ke [sisi] px lalu kualitas diturunkan bertahap sampai di bawah [batas] byte. */
    private fun jpeg(sumber: Bitmap, sisi: Int, batas: Int): String {
        val skala = sisi.toFloat() / max(sumber.width, sumber.height)
        val kecil = if (skala < 1f) {
            Bitmap.createScaledBitmap(sumber, (sumber.width * skala).roundToInt(), (sumber.height * skala).roundToInt(), true)
        } else sumber
        var kualitas = 75
        var hasil: ByteArray
        do {
            hasil = ByteArrayOutputStream().use { out ->
                kecil.compress(Bitmap.CompressFormat.JPEG, kualitas, out)
                out.toByteArray()
            }
            kualitas -= 10
        } while (hasil.size > batas && kualitas >= 25)
        if (kecil !== sumber) kecil.recycle()
        if (hasil.size > batas) throw FotoTerlaluBesar()
        return Base64.encodeToString(hasil, Base64.NO_WRAP)
    }

    private fun baca(uri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder sudah memutar foto sesuai orientasi EXIF kamera.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val s = max(info.size.width, info.size.height)
                if (s > SISI_PENUH * 2) {
                    val bagi = s / (SISI_PENUH * 2f)
                    decoder.setTargetSize((info.size.width / bagi).roundToInt(), (info.size.height / bagi).roundToInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val ukuran = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, ukuran) }
            var sampel = 1
            while (max(ukuran.outWidth, ukuran.outHeight) / (sampel * 2) >= SISI_PENUH) sampel *= 2
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampel })
            } ?: throw IllegalStateException("Foto tidak dapat dibaca")
            // Android 7–8: BitmapFactory tidak membaca orientasi EXIF, jadi foto kamera diputar manual.
            val derajat = runCatching {
                context.contentResolver.openInputStream(uri)?.use {
                    when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                }
            }.getOrNull() ?: 0f
            if (derajat == 0f) bitmap else Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(derajat) }, true
            ).also { if (it !== bitmap) bitmap.recycle() }
        }

    companion object {
        const val SISI_PENUH = 1280
        const val BYTE_PENUH = 450_000
        const val SISI_THUMBNAIL = 360
        const val BYTE_THUMBNAIL = 40_000

        /** File sementara untuk hasil kamera; dibagikan ke aplikasi kamera lewat FileProvider. */
        fun fileKamera(context: Context): File =
            File(context.cacheDir, "foto_fisik").apply { mkdirs() }.resolve("kamera-${UUID.randomUUID()}.jpg")
    }
}
