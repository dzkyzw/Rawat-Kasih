package com.example.rawatkasih.feature.obat.data.remote

import com.example.rawatkasih.data.remote.idsPenugasan
import com.example.rawatkasih.feature.obat.data.SumberObat
import com.example.rawatkasih.feature.obat.model.JadwalObat
import com.example.rawatkasih.feature.obat.model.RencanaObat
import com.example.rawatkasih.feature.obat.model.StatusJadwal
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

internal data class PilihanLansiaObat(val id: String, val nama: String)

internal class SesiFirebaseBerakhir : IllegalStateException()
internal class AksesObatDitolak : IllegalStateException()
internal class KonflikObat(val pesan: String) : IllegalStateException(pesan)

internal class FirebaseObatRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : SumberObat {
    private val mutex = Mutex()
    private val rencana = MutableStateFlow<RencanaObat?>(null)
    private var rencanaDipilih: String? = null
    val emailAktif: String?
        get() = auth.currentUser?.email

    val sudahMasuk: Boolean
        get() = auth.currentUser != null

    suspend fun peran(): String? = dokumenAkses().getString("peran")

    suspend fun daftarPenugasan(): List<PilihanLansiaObat> =
        daftarId(dokumenAkses()).map { id ->
            val nama = firestore.collection("rencanaObat").document(id)
                .get(Source.SERVER).await().getString("namaLansia") ?: throw AksesObatDitolak()
            PilihanLansiaObat(id, nama)
        }.sortedBy { it.nama }

    suspend fun pilihLansia(id: String) {
        mutex.withLock {
            rencanaDipilih = id
            rencana.value = null
        }
    }

    private suspend fun dokumenAkses(): DocumentSnapshot =
        firestore.collection("aksesObat").document(uidAktif()).get(Source.SERVER).await()

    private fun daftarId(akses: DocumentSnapshot) =
        idsPenugasan(akses.get("rencanaIds"), akses.getString("rencanaId"))

    suspend fun masuk(email: String, password: String) {
        mutex.withLock { auth.signInWithEmailAndPassword(email, password).await() }
    }

    suspend fun gantiKataSandi(lama: String, baru: String) {
        mutex.withLock {
            val pengguna = auth.currentUser ?: throw SesiFirebaseBerakhir()
            val email = pengguna.email ?: throw SesiFirebaseBerakhir()
            pengguna.reauthenticate(EmailAuthProvider.getCredential(email, lama)).await()
            pengguna.updatePassword(baru).await()
        }
    }

    suspend fun keluar() {
        mutex.withLock {
            auth.signOut()
            rencanaDipilih = null
            rencana.value = null
        }
    }

    override fun amatiRencana() = flow {
        segarkan()
        emitAll(rencana.filterNotNull())
    }

    suspend fun segarkan() {
        mutex.withLock {
            val akses = bacaAkses(dokumenAkses())
            val dokumen = akses.rencana.get(Source.SERVER).await()
            rencana.value = bacaRencana(dokumen, akses.bisaMengubah)
        }
    }

    override suspend fun catat(jam: String, status: StatusJadwal, alasan: String) = transaksi { transaksi, dokumen, ref, uid ->
        val lama = bacaRencana(dokumen, bisaMengubah = true)
        val baru = lama.catat(jam, status, alasan)
        if (baru == lama) {
            val tercatat = lama.jadwal.firstOrNull { it.jam == jam }
            if (tercatat == null || tercatat.status == StatusJadwal.BELUM_DIKONFIRMASI) {
                throw KonflikObat("Jadwal belum bisa dikonfirmasi. Muat ulang untuk melihat kondisi terbaru.")
            }
            lama
        } else {
            val catatan = checkNotNull(dokumen.get("jadwal") as? Map<*, *>)
            val jadwal = catatan.entries.associate { (kunci, nilai) -> kunci as String to nilai }.toMutableMap()
            val dosis = baru.jadwal.first { it.jam == jam }
            val dataJadwal = mutableMapOf<String, Any>(
                "waktu" to dosis.waktu,
                "status" to dosis.status.name,
                "dicatatOleh" to uid,
                "dicatatPada" to FieldValue.serverTimestamp()
            )
            if (dosis.alasanPenolakan.isNotEmpty()) {
                dataJadwal["alasanPenolakan"] = dosis.alasanPenolakan
            }
            jadwal[jam] = dataJadwal
            transaksi.update(
                ref,
                mapOf("stok" to baru.stok, "jadwal" to jadwal, "aksi" to mapOf("jenis" to "KONFIRMASI", "jam" to jam))
            )
            baru
        }
    }

    suspend fun simpanPengaturan(stokAwal: Int, stokBaru: Int?, jam: String?, waktu: String?) =
        transaksi { transaksi, dokumen, ref, _ ->
            val lama = bacaRencana(dokumen, bisaMengubah = true)
            if (stokBaru != null && stokAwal != lama.stok) {
                throw KonflikObat("Stok sudah berubah. Tutup dialog, lalu pilih Muat ulang.")
            }
            if (jam != null && lama.jadwal.any { it.jam == jam }) {
                throw KonflikObat("Jam itu sudah ada di jadwal.")
            }
            val perubahan = mutableMapOf<String, Any>(
                "stok" to (stokBaru ?: lama.stok),
                "aksi" to mapOf("jenis" to "PENGATURAN", "jam" to jam.orEmpty())
            )
            val jadwal = jikaJadwalBaru(dokumen, jam, waktu)
            if (jadwal != null) perubahan["jadwal"] = jadwal
            transaksi.update(ref, perubahan)
            val jadwalBaru = if (jam == null) lama.jadwal else {
                (lama.jadwal + JadwalObat(jam, checkNotNull(waktu))).sortedBy { it.jam }
            }
            lama.copy(stok = stokBaru ?: lama.stok, jadwal = jadwalBaru)
        }

    private suspend fun transaksi(
        ubah: (Transaction, DocumentSnapshot, DocumentReference, String) -> RencanaObat
    ) {
        mutex.withLock {
            val uid = uidAktif()
            val hasil = firestore.runTransaction { transaksi ->
                val akses = bacaAkses(transaksi.get(firestore.collection("aksesObat").document(uid)))
                if (!akses.bisaMengubah) throw AksesObatDitolak()
                ubah(transaksi, transaksi.get(akses.rencana), akses.rencana, uid)
            }.await()
            rencana.value = hasil
        }
    }

    private fun uidAktif(): String = auth.currentUser?.uid ?: throw SesiFirebaseBerakhir()

    private data class Akses(val rencana: DocumentReference, val bisaMengubah: Boolean)

    private fun bacaAkses(dokumen: DocumentSnapshot): Akses {
        val peran = dokumen.getString("peran")
        val ids = daftarId(dokumen)
        val rencanaId = when (peran) {
            "KELUARGA" -> ids.singleOrNull()
            "PRAMURUKTI" -> rencanaDipilih?.takeIf { it in ids } ?: if (rencanaDipilih == null) ids.singleOrNull() else null
            else -> null
        } ?: throw AksesObatDitolak()
        return Akses(firestore.collection("rencanaObat").document(rencanaId), peran == "PRAMURUKTI")
    }

    private fun bacaRencana(dokumen: DocumentSnapshot, bisaMengubah: Boolean): RencanaObat {
        val jadwal = checkNotNull(dokumen.get("jadwal") as? Map<*, *>)
        return RencanaObat(
            namaLansia = checkNotNull(dokumen.getString("namaLansia")),
            stok = checkNotNull(dokumen.getLong("stok")).toInt(),
            batasStok = checkNotNull(dokumen.getLong("batasStok")).toInt(),
            jadwal = jadwal.entries.map { (jam, data) ->
                val dosis = data as Map<*, *>
                JadwalObat(
                    jam = jam as String,
                    waktu = dosis["waktu"] as String,
                    status = StatusJadwal.valueOf(dosis["status"] as String),
                    alasanPenolakan = dosis["alasanPenolakan"] as? String ?: ""
                )
            }.sortedBy { it.jam },
            bisaMengubah = bisaMengubah
        )
    }

    private fun jikaJadwalBaru(dokumen: DocumentSnapshot, jam: String?, waktu: String?): Map<String, Any?>? {
        if (jam == null) return null
        val jadwal = checkNotNull(dokumen.get("jadwal") as? Map<*, *>)
        return jadwal.entries.associate { (kunci, nilai) -> kunci as String to nilai } +
            (jam to mapOf("waktu" to checkNotNull(waktu), "status" to StatusJadwal.BELUM_DIKONFIRMASI.name))
    }
}
