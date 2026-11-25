package com.example.rawatkasih.feature.penugasan.data

import com.example.rawatkasih.data.remote.idsPenugasan
import com.example.rawatkasih.feature.penugasan.model.Penugasan
import com.example.rawatkasih.feature.penugasan.model.PolaShift
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** Akun yang sedang masuk. [lansiaIds] berasal dari aksesObat (penugasan yang juga dipakai Fungsi 1). */
internal data class Sesi(val uid: String, val email: String, val peran: String?, val lansiaIds: List<String>)

internal class PenugasanGagal(val pesan: String) : IllegalStateException(pesan)

/**
 * Pola shift panti dan rincian shift tiap penugasan (Fungsi 2), di atas penugasan Firebase milik Fungsi 1.
 *
 * - pengaturanPanti/shift: jamMulai (daftar jam mulai tiap shift).
 * - penugasanShift/{lansiaId}: tugas.{uid} = {email, shift: [nomor shift]}; daftar shift kosong = penuh waktu.
 * - aksesObat/{uid}.rencanaIds tetap menjadi sumber "siapa ditugaskan ke lansia mana" dan ikut diperbarui.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class PenugasanRepository(private val auth: FirebaseAuth, private val firestore: FirebaseFirestore) {
    private val dokumenPola get() = firestore.collection("pengaturanPanti").document("shift")
    private val tugas get() = firestore.collection("penugasanShift")
    private val akses get() = firestore.collection("aksesObat")

    fun amatiSesi(): Flow<Sesi?> = callbackFlow {
        val pendengar = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(pendengar)
        awaitClose { auth.removeAuthStateListener(pendengar) }
    }.distinctUntilChanged().flatMapLatest { uid ->
        if (uid == null) flowOf(null)
        else akses.document(uid).amati()
            .map<DocumentSnapshot, Sesi?> { d -> Sesi(uid, auth.currentUser?.email.orEmpty(), d.getString("peran"), idsPenugasan(d.get("rencanaIds"), d.getString("rencanaId"))) }
            // Saat keluar, pendengar dokumen bisa ditolak sebelum status masuk berubah.
            .catch { error -> if (auth.currentUser?.uid != uid) emit(null) else throw error }
    }

    fun amatiPola(): Flow<PolaShift> = dokumenPola.amati().map { bacaPola(it) }

    fun amatiPenugasan(lansiaId: String): Flow<List<Penugasan>> = tugas.document(lansiaId).amati().map { bacaTugas(lansiaId, it) }

    suspend fun namaLansia(id: String): String =
        firestore.collection("rencanaObat").document(id).get().await().getString("namaLansia") ?: "Lansia"

    /** Menyimpan pola baru; pendamping pada shift yang dihapus ikut dilepas dari lansia itu. */
    suspend fun simpanPola(baru: PolaShift) {
        val semua = tugas.get(Source.SERVER).await().documents.map { it.reference }
        firestore.runTransaction { t ->
            val daftar = semua.map { t.get(it) }
            val perubahan = daftar.associate { d ->
                d.id to bacaTugas(d.id, d).filter { it.shiftKe == null || it.shiftKe < baru.jumlah }
            }
            val lepas = daftar.flatMap { d ->
                val tetap = perubahan.getValue(d.id).map { it.pramuruktiId }.toSet()
                bacaTugas(d.id, d).map { it.pramuruktiId }.filter { it !in tetap }.distinct().map { d.id to it }
            }
            val aksesLama = lepas.map { it.second }.distinct().associateWith { t.get(akses.document(it)) }
            t.set(dokumenPola, mapOf("jamMulai" to baru.jamMulai))
            daftar.forEach { d -> t.set(d.reference, dataTugas(perubahan.getValue(d.id))) }
            aksesLama.forEach { (uid, dok) ->
                aturRencana(t, dok, uid, tambah = emptySet(), hapus = lepas.filter { it.second == uid }.map { it.first }.toSet())
            }
        }.await()
    }

    /**
     * Penugasan satu lansia: penuh waktu ([penuhWaktu] = uid) atau bergantian ([perShift] nomor shift → uid).
     * [ditugaskanSekarang] = pramurukti yang saat ini punya lansia ini di rencanaIds, supaya yang tidak dipilih lagi dilepas.
     */
    suspend fun simpanPenugasan(
        lansiaId: String,
        penuhWaktu: String?,
        perShift: Map<Int, String>,
        email: Map<String, String>,
        ditugaskanSekarang: Set<String>
    ) {
        val baru = if (penuhWaktu != null) listOf(Penugasan(lansiaId, penuhWaktu, null, email[penuhWaktu].orEmpty()))
        else perShift.map { (ke, uid) -> Penugasan(lansiaId, uid, ke, email[uid].orEmpty()) }
        val dipilih = baru.map { it.pramuruktiId }.toSet()
        firestore.runTransaction { t ->
            val ref = tugas.document(lansiaId)
            val lama = bacaTugas(lansiaId, t.get(ref)).map { it.pramuruktiId }
            val semua = (dipilih + ditugaskanSekarang + lama).associateWith { t.get(akses.document(it)) }
            t.set(ref, dataTugas(baru))
            semua.forEach { (uid, dok) ->
                if (uid in dipilih) aturRencana(t, dok, uid, tambah = setOf(lansiaId), hapus = emptySet())
                else aturRencana(t, dok, uid, tambah = emptySet(), hapus = setOf(lansiaId))
            }
        }.await()
    }

    private fun aturRencana(t: Transaction, dok: DocumentSnapshot, uid: String, tambah: Set<String>, hapus: Set<String>) {
        if (dok.getString("peran") != "PRAMURUKTI") throw PenugasanGagal("Akun pramurukti tidak tersedia.")
        val lama = idsPenugasan(dok.get("rencanaIds"), dok.getString("rencanaId"))
        val baru = (lama + tambah - hapus).distinct().sorted()
        if (baru.size > 50) throw PenugasanGagal("Maksimal 50 penugasan per akun.")
        if (baru == lama.sorted() && dok.contains("rencanaIds")) return
        val data = mutableMapOf<String, Any>("peran" to "PRAMURUKTI", "rencanaIds" to baru)
        dok.getString("email")?.let { data["email"] = it }
        t.set(akses.document(uid), data)
    }

    companion object {
        fun bacaPola(d: DocumentSnapshot): PolaShift =
            (d.get("jamMulai") as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }
                ?.let { runCatching { PolaShift(it) }.getOrNull() } ?: PolaShift(listOf(7))

        fun bacaTugas(lansiaId: String, d: DocumentSnapshot): List<Penugasan> =
            (d.get("tugas") as? Map<*, *>).orEmpty().flatMap { (uid, isi) ->
                val data = isi as? Map<*, *> ?: return@flatMap emptyList()
                val email = data["email"] as? String ?: ""
                val shift = (data["shift"] as? List<*>).orEmpty().mapNotNull { (it as? Number)?.toInt() }
                if (shift.isEmpty()) listOf(Penugasan(lansiaId, uid as String, null, email))
                else shift.map { Penugasan(lansiaId, uid as String, it, email) }
            }

        fun dataTugas(daftar: List<Penugasan>) = mapOf("tugas" to daftar.groupBy { it.pramuruktiId }.mapValues { (_, p) ->
            mapOf("email" to p.first().email, "shift" to p.mapNotNull { it.shiftKe }.sorted())
        })
    }
}

internal fun DocumentReference.amati(): Flow<DocumentSnapshot> = callbackFlow {
    val r = addSnapshotListener { snap, error ->
        if (error != null) close(error) else if (snap != null) trySend(snap)
    }
    awaitClose { r.remove() }
}

/** Dipakai AdminRepository (Fungsi 1) saat pramurukti dicentang/dilepas tanpa memilih shift. */
internal fun ubahTugasLewatCentang(t: Transaction, tugasLansia: DocumentReference, lama: DocumentSnapshot, uid: String, email: String?, tugaskan: Boolean) {
    val sudahAda = (lama.get("tugas") as? Map<*, *>)?.containsKey(uid) == true
    when {
        tugaskan && !sudahAda -> t.set(tugasLansia, mapOf("tugas" to mapOf(uid to mapOf("email" to email.orEmpty(), "shift" to emptyList<Int>()))), com.google.firebase.firestore.SetOptions.merge())
        !tugaskan && sudahAda -> t.update(tugasLansia, "tugas.$uid", FieldValue.delete())
    }
}
