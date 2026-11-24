package com.example.rawatkasih.feature.admin.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

internal enum class PeranAkun(val label: String) {
    KELUARGA("Keluarga"),
    PRAMURUKTI("Pramurukti")
}

internal data class LansiaAdmin(val id: String, val nama: String)

internal class AksesAdminDitolak : IllegalStateException()
internal class KonflikAkunAdmin(val pesan: String) : IllegalStateException(pesan)
internal class PenyambunganAkunGagal(cause: Exception) : IllegalStateException(cause)

internal class AdminRepository(
    private val authAdmin: FirebaseAuth,
    private val authPendaftaran: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val pendaftaran = Mutex()

    suspend fun daftarLansia(): List<LansiaAdmin> {
        return firestore.collection("lansiaP04").get(Source.SERVER).await().documents
            .mapNotNull { dokumen ->
                dokumen.getString("namaLansia")?.takeIf(String::isNotBlank)?.let { LansiaAdmin(dokumen.id, it) }
            }
            .sortedWith(compareBy<LansiaAdmin> { it.nama }.thenBy { it.id })
    }

    suspend fun buatAkun(email: String, kataSandi: String, peran: PeranAkun, rencanaId: String): Boolean =
        pendaftaran.withLock {
            val adminUid = authAdmin.currentUser?.uid ?: throw AksesAdminDitolak()
            val akses = firestore.collection("aksesObat")
            if (akses.document(adminUid).get(Source.SERVER).await().getString("peran") != "ADMIN") {
                throw AksesAdminDitolak()
            }

            authPendaftaran.signOut()
            try {
                val uid = siapkanAkun(email, kataSandi)
                if (uid == adminUid) throw KonflikAkunAdmin("Gunakan email yang berbeda dari akun admin.")
                val target = akses.document(uid)
                val hasil = try {
                    firestore.runTransaction { transaksi ->
                        val lama = transaksi.get(target)
                        when {
                            !lama.exists() -> {
                                transaksi.set(target, mapOf("peran" to peran.name, "rencanaId" to rencanaId))
                                HasilAkses.DIBUAT
                            }
                            lama.getString("peran") == peran.name && lama.getString("rencanaId") == rencanaId ->
                                HasilAkses.SUDAH_ADA
                            else -> HasilAkses.KONFLIK
                        }
                    }.await()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    throw PenyambunganAkunGagal(error)
                }
                when (hasil) {
                    HasilAkses.DIBUAT -> true
                    HasilAkses.SUDAH_ADA -> false
                    HasilAkses.KONFLIK -> throw KonflikAkunAdmin("Email sudah terhubung ke peran atau lansia lain.")
                }
            } finally {
                authPendaftaran.signOut()
            }
        }

    private suspend fun siapkanAkun(email: String, kataSandi: String): String {
        val hasil = try {
            authPendaftaran.createUserWithEmailAndPassword(email, kataSandi).await()
        } catch (_: FirebaseAuthUserCollisionException) {
            // Resume an earlier registration only when the same credentials still work.
            try {
                authPendaftaran.signInWithEmailAndPassword(email, kataSandi).await()
            } catch (_: FirebaseAuthInvalidCredentialsException) {
                throw KonflikAkunAdmin("Email sudah terdaftar. Jika ini percobaan ulang, gunakan kata sandi awal yang sama.")
            }
        }
        return checkNotNull(hasil.user).uid
    }

    private enum class HasilAkses { DIBUAT, SUDAH_ADA, KONFLIK }
}
