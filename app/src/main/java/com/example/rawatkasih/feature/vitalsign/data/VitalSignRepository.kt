package com.example.rawatkasih.feature.vitalsign.data

import com.example.rawatkasih.feature.vitalsign.model.AnalisisVitalSign
import com.example.rawatkasih.feature.vitalsign.model.CatatanVitalSign
import com.example.rawatkasih.feature.vitalsign.model.StatusRisiko
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * vitalSign/{lansiaId}/catatan/{id}: riwayat pemeriksaan vital sign medis (Sistol, Diastol, Gula Darah)
 * per lansia, diamati real-time oleh Pramurukti, Keluarga, dan Pengelola Panti.
 */
internal class VitalSignRepository(private val firestore: FirebaseFirestore) {
    private fun koleksi(lansiaId: String) =
        firestore.collection("vitalSign").document(lansiaId).collection("catatan")

    fun amatiVitalSign(lansiaId: String): Flow<List<CatatanVitalSign>> = callbackFlow {
        val listener = koleksi(lansiaId)
            .orderBy("waktu", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                } else if (snap != null) {
                    trySend(snap.documents.mapNotNull { baca(lansiaId, it) })
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun simpan(
        lansiaId: String,
        uid: String,
        email: String,
        sistol: Int,
        diastol: Int,
        gulaDarah: Int,
        statusRisiko: StatusRisiko
    ) {
        koleksi(lansiaId).add(
            mapOf(
                "sistol" to sistol,
                "diastol" to diastol,
                "gulaDarah" to gulaDarah,
                "statusRisiko" to statusRisiko.name,
                "penulisUid" to uid,
                "penulisEmail" to email,
                "waktu" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private fun baca(lansiaId: String, d: DocumentSnapshot): CatatanVitalSign? {
        val sistol = d.getLong("sistol")?.toInt() ?: return null
        val diastol = d.getLong("diastol")?.toInt() ?: return null
        val gulaDarah = d.getLong("gulaDarah")?.toInt() ?: return null
        val waktu = d.getTimestamp("waktu", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: return null
        val status = runCatching { StatusRisiko.valueOf(d.getString("statusRisiko").orEmpty()) }
            .getOrNull() ?: AnalisisVitalSign.klasifikasiRisiko(sistol, diastol, gulaDarah)
        return CatatanVitalSign(
            id = d.id,
            lansiaId = lansiaId,
            sistol = sistol,
            diastol = diastol,
            gulaDarah = gulaDarah,
            statusRisiko = status,
            penulisId = d.getString("penulisUid").orEmpty(),
            penulisNama = d.getString("penulisEmail").orEmpty(),
            waktu = waktu
        )
    }
}
