
package com.example.rawatkasih.feature.admin.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.rawatkasih.BuildConfig
import com.example.rawatkasih.data.remote.FirebaseP04
import com.example.rawatkasih.feature.obat.data.remote.AksesObatDitolak
import com.example.rawatkasih.feature.obat.data.remote.FirebaseObatRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AdminRepositoryTest {
    @Test
    fun profilKeluargaDanPenugasanPramurukti() = runBlocking {
        assumeTrue(BuildConfig.FIREBASE_EMULATOR)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val firebase = checkNotNull(FirebaseP04.ambil(context))
        val kataSandi = "RawatKasih-Demo-Lokal"
        val id = UUID.randomUUID().toString()
        val emailKeluarga = "keluarga-${id}@rawatkasih.test"
        val emailPramurukti = "staf-${id}@rawatkasih.test"

        withTimeout(90_000) {
            try {
                val adminUid = checkNotNull(
                    firebase.auth.signInWithEmailAndPassword("admin@rawatkasih.test", kataSandi).await().user
                ).uid
                val admin = AdminRepository(firebase.auth, firebase.authPendaftaran, firebase.firestore)
                admin.simpanLansia(id, "Oma Uji", "A1", "", "")
                assertNull(admin.daftarLansia().single { it.id == id }.emailKeluarga)
                admin.simpanLansia(id, "Oma Uji Baru", "A2", emailKeluarga, kataSandi)
                admin.simpanLansia(id, "Oma Uji Baru", "A2", emailKeluarga, kataSandi)
                val lansia = admin.daftarLansia().single { it.id == id }
                assertEquals("A2", lansia.kamar)
                assertEquals(emailKeluarga, lansia.emailKeluarga)

                admin.buatPramurukti(emailPramurukti, kataSandi)
                val pramurukti = admin.daftarPramurukti().single { it.email == emailPramurukti }
                assertTrue(pramurukti.rencanaIds.isEmpty())
                admin.ubahPenugasan(pramurukti.uid, id, true)
                admin.ubahPenugasan(pramurukti.uid, "opa-damar-demo", true)
                assertEquals(adminUid, firebase.auth.currentUser?.uid)
                assertNull(firebase.authPendaftaran.currentUser)

                val pengguna = FirebaseObatRepository(firebase.auth, firebase.firestore)
                pengguna.keluar()
                pengguna.masuk(emailPramurukti, kataSandi)
                assertEquals(setOf(id, "opa-damar-demo"), pengguna.daftarPenugasan().map { it.id }.toSet())
                pengguna.pilihLansia(id)
                assertTrue(pengguna.amatiRencana().first().bisaMengubah)
                pengguna.pilihLansia("opa-damar-demo")
                assertEquals("Opa Damar", pengguna.amatiRencana().first().namaLansia)
                assertAksesDitolak {
                    firebase.firestore.collection("rencanaObat").document("oma-ratih-demo").get(Source.SERVER).await()
                }
                pengguna.pilihLansia("oma-ratih-demo")
                try {
                    pengguna.amatiRencana().first()
                    fail("Rencana yang tidak ditugaskan harus ditolak.")
                } catch (_: AksesObatDitolak) {
                    // Expected.
                }
                pengguna.keluar()
                pengguna.masuk(emailKeluarga, kataSandi)
                val rencana = pengguna.amatiRencana().first()
                assertEquals("Oma Uji Baru", rencana.namaLansia)
                assertFalse(rencana.bisaMengubah)
                assertAksesDitolak {
                    firebase.firestore.collection("rencanaObat").document(id).update("stok", 99).await()
                }
                pengguna.keluar()
                firebase.auth.signInWithEmailAndPassword("admin@rawatkasih.test", kataSandi).await()
                admin.ubahPenugasan(pramurukti.uid, id, false)
                assertEquals(listOf("opa-damar-demo"), admin.daftarPramurukti().single { it.uid == pramurukti.uid }.rencanaIds)
                val emailPengganti = "pengganti-$id@rawatkasih.test"
                admin.simpanLansia(id, "Oma Uji Baru", "A2", emailPengganti, kataSandi)
                pengguna.keluar()
                pengguna.masuk(emailKeluarga, kataSandi)
                assertAksesDitolak {
                    firebase.firestore.collection("rencanaObat").document(id).get(Source.SERVER).await()
                }
                pengguna.keluar()
                pengguna.masuk(emailPengganti, kataSandi)
                assertEquals("Oma Uji Baru", pengguna.amatiRencana().first().namaLansia)
            } finally {
                firebase.auth.signOut()
            }
        }
    }

    @Test
    fun penggunaBisaMenggantiKataSandiAwal() = runBlocking {
        assumeTrue(BuildConfig.FIREBASE_EMULATOR)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val firebase = checkNotNull(FirebaseP04.ambil(context))
        val email = "sandi-" + UUID.randomUUID() + "@rawatkasih.test"
        val awal = "Sandi-Awal-Uji"
        val baru = "Sandi-Baru-Uji"
        val pengguna = FirebaseObatRepository(firebase.auth, firebase.firestore)

        withTimeout(45_000) {
            try {
                val uid = checkNotNull(firebase.auth.createUserWithEmailAndPassword(email, awal).await().user).uid
                assertSandiDitolak { pengguna.gantiKataSandi("Sandi-Salah-Uji", baru) }
                pengguna.gantiKataSandi(awal, baru)
                assertEquals(uid, firebase.auth.currentUser?.uid)
                pengguna.keluar()
                assertSandiDitolak { pengguna.masuk(email, awal) }
                pengguna.masuk(email, baru)
                assertEquals(uid, firebase.auth.currentUser?.uid)
            } finally {
                pengguna.keluar()
            }
        }
    }

    private suspend fun assertSandiDitolak(aksi: suspend () -> Unit) {
        try {
            aksi()
            fail("Firebase Auth harus menolak kata sandi yang salah.")
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            // Expected.
        }
    }

    private suspend fun assertAksesDitolak(aksi: suspend () -> Unit) {
        try {
            aksi()
            fail("Firestore harus menolak akses.")
        } catch (error: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, error.code)
        }
    }
}
