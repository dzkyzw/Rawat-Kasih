
package com.example.rawatkasih.feature.admin.data

import com.example.rawatkasih.data.remote.idsPenugasan
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

internal data class LansiaAdmin(val id: String, val nama: String, val kamar: String, val emailKeluarga: String?)
internal data class PramuruktiAdmin(val uid: String, val email: String, val rencanaIds: List<String>)
internal class AksesAdminDitolak : IllegalStateException()
internal class KonflikAkunAdmin(val pesan: String) : IllegalStateException(pesan)

internal class AdminRepository(
    private val authAdmin: FirebaseAuth,
    private val authPendaftaran: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val pendaftaran = Mutex()
    private val akses get() = firestore.collection("aksesObat")
    private val lansia get() = firestore.collection("lansiaP04")

    suspend fun daftarLansia(): List<LansiaAdmin> =
        lansia.get(Source.SERVER).await().documents.mapNotNull { dokumen ->
            val nama = dokumen.getString("namaLansia") ?: return@mapNotNull null
            LansiaAdmin(dokumen.id, nama, dokumen.getString("kamar").orEmpty(), dokumen.getString("emailKeluarga"))
        }.sortedBy { it.nama }

    suspend fun daftarPramurukti(): List<PramuruktiAdmin> =
        akses.whereEqualTo("peran", "PRAMURUKTI").get(Source.SERVER).await().documents.map { dokumen ->
            PramuruktiAdmin(
                dokumen.id,
                dokumen.getString("email") ?: "Akun lama (${dokumen.id.take(8)})",
                idsPenugasan(dokumen.get("rencanaIds"), dokumen.getString("rencanaId"))
            )
        }.sortedBy { it.email }

    suspend fun simpanLansia(id: String, nama: String, kamar: String, email: String, kataSandi: String) =
        pendaftaran.withLock {
            val admin = uidAdmin()
            val uidKeluarga = if (email.isBlank()) null else siapkanAkun(admin, email, kataSandi)
            val profil = lansia.document(id)
            val rencana = firestore.collection("rencanaObat").document(id)
            val akun = uidKeluarga?.let(akses::document)
            firestore.runTransaction { transaksi ->
                val profilLama = transaksi.get(profil)
                val rencanaLama = transaksi.get(rencana)
                val akunLama = akun?.let(transaksi::get)
                if (akunLama?.exists() == true &&
                    (akunLama.getString("peran") != "KELUARGA" || akunLama.getString("rencanaId") != id)
                ) throw KonflikAkunAdmin("Email ini sudah digunakan untuk peran atau lansia lain.")

                val keluargaUid = uidKeluarga ?: profilLama.getString("keluargaUid")
                val emailKeluarga = email.ifBlank { profilLama.getString("emailKeluarga").orEmpty() }
                val data = mutableMapOf<String, Any>("namaLansia" to nama, "kamar" to kamar)
                if (keluargaUid != null) {
                    data["keluargaUid"] = keluargaUid
                    data["emailKeluarga"] = emailKeluarga
                }
                transaksi.set(profil, data)
                if (rencanaLama.exists()) {
                    if (rencanaLama.getString("namaLansia") != nama) {
                        transaksi.update(rencana, "namaLansia", nama)
                    }
                } else {
                    transaksi.set(rencana, mapOf(
                        "namaLansia" to nama,
                        "stok" to 0,
                        "batasStok" to 4,
                        "aksi" to mapOf("jenis" to "AWAL", "jam" to ""),
                        "jadwal" to emptyMap<String, Any>()
                    ))
                }
                if (akun != null) {
                    transaksi.set(akun, mapOf("peran" to "KELUARGA", "rencanaId" to id, "email" to email))
                }
            }.await()
        }

    suspend fun buatPramurukti(email: String, kataSandi: String) = pendaftaran.withLock {
        val uid = siapkanAkun(uidAdmin(), email, kataSandi)
        val akun = akses.document(uid)
        firestore.runTransaction { transaksi ->
            val lama = transaksi.get(akun)
            if (lama.exists() && lama.getString("peran") != "PRAMURUKTI") {
                throw KonflikAkunAdmin("Email ini sudah digunakan untuk peran lain.")
            }
            val ids = idsPenugasan(lama.get("rencanaIds"), lama.getString("rencanaId"))
            transaksi.set(akun, mapOf("peran" to "PRAMURUKTI", "email" to email, "rencanaIds" to ids))
        }.await()
    }

    suspend fun ubahPenugasan(uid: String, rencanaId: String, tugaskan: Boolean) {
        uidAdmin()
        val akun = akses.document(uid)
        val profil = lansia.document(rencanaId)
        firestore.runTransaction { transaksi ->
            val lama = transaksi.get(akun)
            if (lama.getString("peran") != "PRAMURUKTI" || !transaksi.get(profil).exists()) {
                throw KonflikAkunAdmin("Akun pramurukti atau profil lansia tidak tersedia.")
            }
            val ids = idsPenugasan(lama.get("rencanaIds"), lama.getString("rencanaId")).toMutableSet()
            if (tugaskan) ids.add(rencanaId) else ids.remove(rencanaId)
            if (ids.size > 50) throw KonflikAkunAdmin("Maksimal 50 penugasan per akun.")
            val data = mutableMapOf<String, Any>("peran" to "PRAMURUKTI", "rencanaIds" to ids.sorted())
            lama.getString("email")?.let { data["email"] = it }
            transaksi.set(akun, data)
        }.await()
    }

    suspend fun kirimTautanSandi(email: String) {
        uidAdmin()
        authAdmin.sendPasswordResetEmail(email).await()
    }

    private suspend fun uidAdmin(): String {
        val uid = authAdmin.currentUser?.uid ?: throw AksesAdminDitolak()
        if (akses.document(uid).get(Source.SERVER).await().getString("peran") != "ADMIN") {
            throw AksesAdminDitolak()
        }
        return uid
    }

    private suspend fun siapkanAkun(admin: String, email: String, kataSandi: String): String {
        authPendaftaran.signOut()
        try {
            val hasil = try {
                authPendaftaran.createUserWithEmailAndPassword(email, kataSandi).await()
            } catch (_: FirebaseAuthUserCollisionException) {
                try {
                    authPendaftaran.signInWithEmailAndPassword(email, kataSandi).await()
                } catch (_: FirebaseAuthInvalidCredentialsException) {
                    throw KonflikAkunAdmin("Email sudah terdaftar. Untuk melanjutkan percobaan sebelumnya, gunakan kata sandi yang sama.")
                }
            }
            val uid = checkNotNull(hasil.user).uid
            if (uid == admin) throw KonflikAkunAdmin("Gunakan email yang berbeda dari akun admin.")
            return uid
        } finally {
            authPendaftaran.signOut()
        }
    }
}
