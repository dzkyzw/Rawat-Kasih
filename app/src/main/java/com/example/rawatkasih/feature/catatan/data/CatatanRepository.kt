package com.example.rawatkasih.feature.catatan.data

import com.example.rawatkasih.feature.catatan.model.CatatanShift
import com.example.rawatkasih.feature.catatan.model.Mood
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** catatanShift/{lansiaId}/catatan/{id}: catatan shift per lansia beserta siapa yang sudah membacanya. */
internal class CatatanRepository(private val firestore: FirebaseFirestore) {
    private fun koleksi(lansiaId: String) = firestore.collection("catatanShift").document(lansiaId).collection("catatan")

    fun amatiCatatan(lansiaId: String): Flow<List<CatatanShift>> = callbackFlow {
        val r = koleksi(lansiaId).orderBy("waktu", Query.Direction.DESCENDING).limit(100)
            .addSnapshotListener { snap, error ->
                if (error != null) close(error) else if (snap != null) trySend(snap.documents.mapNotNull { baca(lansiaId, it) })
            }
        awaitClose { r.remove() }
    }

    suspend fun simpan(lansiaId: String, uid: String, email: String, shift: String, mood: Mood, isi: String) {
        koleksi(lansiaId).add(
            mapOf(
                "penulisUid" to uid, "penulisEmail" to email, "shift" to shift, "mood" to mood.name,
                "isi" to isi.trim(), "waktu" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun tandaiDibaca(daftar: List<CatatanShift>, uid: String, email: String) {
        val batch = firestore.batch()
        daftar.forEach {
            batch.update(
                koleksi(it.lansiaId).document(it.id),
                mapOf("dibacaOlehUid" to uid, "dibacaOlehEmail" to email, "dibacaPada" to FieldValue.serverTimestamp())
            )
        }
        batch.commit().await()
    }

    private fun baca(lansiaId: String, d: DocumentSnapshot): CatatanShift? {
        // Waktu server yang belum terkonfirmasi diperkirakan dari jam perangkat supaya catatan baru langsung tampil.
        fun waktu(kolom: String) = d.getTimestamp(kolom, ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
        return CatatanShift(
            id = d.id,
            lansiaId = lansiaId,
            penulisId = d.getString("penulisUid") ?: return null,
            penulisNama = d.getString("penulisEmail").orEmpty(),
            shift = d.getString("shift").orEmpty(),
            mood = runCatching { Mood.valueOf(d.getString("mood").orEmpty()) }.getOrNull() ?: return null,
            isi = d.getString("isi").orEmpty(),
            waktu = waktu("waktu") ?: return null,
            dibacaOlehId = d.getString("dibacaOlehUid"),
            dibacaOlehNama = d.getString("dibacaOlehEmail"),
            dibacaPada = waktu("dibacaPada")
        )
    }
}
