package com.example.rawatkasih.feature.kunjungan.data

import com.example.rawatkasih.feature.kunjungan.model.Kunjungan
import com.example.rawatkasih.feature.kunjungan.model.StatusKunjungan
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * kunjungan/{lansiaId}/permintaan/{id}: tanggal, jam, jumlahOrang, catatan, status, pengaju, dibuatPada,
 * lalu alasan/ditanganiOlehEmail/ditanganiPada yang diisi pengelola saat menyetujui atau menolak.
 */
internal class KunjunganRepository(private val auth: FirebaseAuth, private val firestore: FirebaseFirestore) {
    private fun koleksi(lansiaId: String) = firestore.collection("kunjungan").document(lansiaId).collection("permintaan")

    fun amati(lansiaId: String): Flow<List<Kunjungan>> = callbackFlow {
        val r = koleksi(lansiaId).orderBy("tanggal", Query.Direction.DESCENDING).limit(60)
            .addSnapshotListener { snap, error ->
                if (error != null) close(error) else if (snap != null) trySend(snap.documents.mapNotNull(::baca))
            }
        awaitClose { r.remove() }
    }

    /**
     * Pengelola: semua permintaan di panti lewat collection group, tanpa filter supaya tidak butuh indeks.
     * shortcut: membaca seluruh riwayat; bila datanya sudah ribuan, tambahkan indeks collection group pada tanggal.
     */
    fun amatiSemua(): Flow<List<Kunjungan>> = callbackFlow {
        val r = firestore.collectionGroup("permintaan").addSnapshotListener { snap, error ->
            if (error != null) close(error) else if (snap != null) trySend(snap.documents.mapNotNull(::baca))
        }
        awaitClose { r.remove() }
    }

    suspend fun ajukan(lansiaId: String, tanggal: String, jam: String, jumlahOrang: Int, catatan: String) {
        val akun = checkNotNull(auth.currentUser) { "Sesi berakhir" }
        koleksi(lansiaId).add(
            mapOf(
                "tanggal" to tanggal, "jam" to jam, "jumlahOrang" to jumlahOrang, "catatan" to catatan.trim(),
                "status" to StatusKunjungan.MENUNGGU.name, "pengajuUid" to akun.uid, "pengajuEmail" to akun.email.orEmpty(),
                "dibuatPada" to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    /** Pengelola menyetujui atau menolak permintaan yang masih menunggu. */
    suspend fun tanggapi(k: Kunjungan, setuju: Boolean, alasan: String) {
        val akun = checkNotNull(auth.currentUser) { "Sesi berakhir" }
        koleksi(k.lansiaId).document(k.id).update(
            mapOf(
                "status" to (if (setuju) StatusKunjungan.DISETUJUI else StatusKunjungan.DITOLAK).name,
                "alasan" to alasan.trim(), "ditanganiOlehEmail" to akun.email.orEmpty(), "ditanganiPada" to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    /** Keluarga membatalkan permintaannya sendiri. */
    suspend fun batalkan(k: Kunjungan) {
        koleksi(k.lansiaId).document(k.id).update("status", StatusKunjungan.DIBATALKAN.name).await()
    }

    private fun baca(d: DocumentSnapshot): Kunjungan? {
        return Kunjungan(
            id = d.id,
            lansiaId = d.reference.parent.parent?.id ?: return null,
            tanggal = d.getString("tanggal") ?: return null,
            jam = d.getString("jam") ?: return null,
            jumlahOrang = d.getLong("jumlahOrang")?.toInt() ?: return null,
            catatan = d.getString("catatan").orEmpty(),
            status = StatusKunjungan.dari(d.getString("status")) ?: return null,
            pengajuUid = d.getString("pengajuUid").orEmpty(),
            pengajuEmail = d.getString("pengajuEmail").orEmpty(),
            alasan = d.getString("alasan").orEmpty(),
            ditanganiOleh = d.getString("ditanganiOlehEmail"),
        )
    }
}
