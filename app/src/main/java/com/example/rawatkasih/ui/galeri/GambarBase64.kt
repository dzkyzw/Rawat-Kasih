package com.example.rawatkasih.ui.galeri

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Foto JPEG Base64 dari Firestore, didekode di luar main thread. Selama didekode (atau bila gagal) yang tampil
 * adalah P3 / Photo slot dengan [label].
 */
@Composable
internal fun GambarBase64(data: String?, label: String, modifier: Modifier, deskripsi: String = "Foto kondisi fisik", potong: Boolean = true) {
    val gambar by produceState<ImageBitmap?>(null, data) {
        value = if (data.isNullOrEmpty()) null else withContext(Dispatchers.Default) {
            runCatching {
                val byte = Base64.decode(data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(byte, 0, byte.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
    val g = gambar
    if (g != null) {
        Image(g, contentDescription = deskripsi, modifier = modifier, contentScale = if (potong) ContentScale.Crop else ContentScale.Fit)
    } else {
        P3SlotKosong(label, modifier)
    }
}
