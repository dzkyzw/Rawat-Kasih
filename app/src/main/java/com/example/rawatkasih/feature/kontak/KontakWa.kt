package com.example.rawatkasih.feature.kontak

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** 0812-3456-789 / +62 812… / 62812… → 62812… (format wa.me). */
internal fun normalTelepon(raw: String): String {
    val d = raw.filter(Char::isDigit)
    return when {
        d.startsWith("62") -> d
        d.startsWith("0") -> "62" + d.drop(1)
        d.startsWith("8") -> "62$d"
        else -> d
    }
}

/** Nomor seluler Indonesia dalam format 62…: 10–15 digit (juga dijaga Firestore Rules). */
internal fun nomorWaValid(nomor: String) = nomor.matches(Regex("^62[0-9]{8,13}$"))

/** "6281234567890" → "+62 812-3456-7890" untuk ditampilkan. */
internal fun tampilNomor(nomor: String): String {
    if (!nomor.startsWith("62") || nomor.length < 10) return nomor
    val sisa = nomor.drop(2)
    return "+62 ${sisa.take(3)}-" + sisa.drop(3).chunked(4).joinToString("-")
}

/** Buka percakapan WhatsApp; wa.me membuka aplikasi WhatsApp bila terpasang, atau WhatsApp Web bila tidak. */
internal fun bukaWhatsApp(context: Context, nomor: String, pesan: String): Boolean {
    if (!nomorWaValid(nomor)) return false
    val uri = Uri.parse("https://wa.me/$nomor?text=${Uri.encode(pesan)}")
    return runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
}

/**
 * Nomor WhatsApp, semuanya opsional sehingga akun lama tetap berjalan tanpa nomor:
 * - pengaturanPanti/kontak: noWa panti, diisi pengelola.
 * - kontakKeluarga/{lansiaId}: noWa, email, uid, diisi akun keluarga lansia itu sendiri.
 */
internal class KontakRepository(private val auth: FirebaseAuth, private val firestore: FirebaseFirestore) {
    private val dokPanti get() = firestore.collection("pengaturanPanti").document("kontak")
    private val keluarga get() = firestore.collection("kontakKeluarga")

    fun amatiNomorPanti(): Flow<String?> = callbackFlow {
        val r = dokPanti.addSnapshotListener { snap, error ->
            if (error != null) close(error) else trySend(snap?.getString("noWa"))
        }
        awaitClose { r.remove() }
    }

    fun amatiNomorKeluarga(lansiaId: String): Flow<String?> = callbackFlow {
        val r = keluarga.document(lansiaId).addSnapshotListener { snap, error ->
            if (error != null) close(error) else trySend(snap?.getString("noWa"))
        }
        awaitClose { r.remove() }
    }

    /** Pengelola: nomor keluarga per lansiaId. */
    fun amatiSemuaNomorKeluarga(): Flow<Map<String, String>> = callbackFlow {
        val r = keluarga.addSnapshotListener { snap, error ->
            if (error != null) close(error)
            else if (snap != null) trySend(snap.documents.mapNotNull { d -> d.getString("noWa")?.let { d.id to it } }.toMap())
        }
        awaitClose { r.remove() }
    }

    suspend fun simpanNomorPanti(nomor: String) {
        dokPanti.set(mapOf("noWa" to nomor)).await()
    }

    suspend fun simpanNomorKeluarga(lansiaId: String, nomor: String) {
        val akun = checkNotNull(auth.currentUser) { "Sesi berakhir" }
        keluarga.document(lansiaId).set(
            mapOf("noWa" to nomor, "email" to akun.email.orEmpty(), "uid" to akun.uid), SetOptions.merge()
        ).await()
    }
}
