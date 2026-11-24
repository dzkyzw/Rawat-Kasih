package com.example.rawatkasih.feature.admin.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.rawatkasih.BuildConfig
import com.example.rawatkasih.data.remote.FirebaseP04
import com.example.rawatkasih.feature.obat.data.remote.FirebaseObatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AdminRepositoryTest {
    @Test
    fun buatAkunTanpaMenggantiSesiAdmin() = runBlocking {
        assumeTrue(BuildConfig.FIREBASE_EMULATOR)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val firebase = checkNotNull(FirebaseP04.ambil(context))
        val kataSandi = "RawatKasih-Demo-Lokal"
        val email = "uji-${UUID.randomUUID()}@rawatkasih.test"

        withTimeout(45_000) {
            try {
                val adminUid = checkNotNull(
                    firebase.auth.signInWithEmailAndPassword("admin@rawatkasih.test", kataSandi).await().user
                ).uid
                val admin = AdminRepository(firebase.auth, firebase.authPendaftaran, firebase.firestore)
                assertTrue(admin.daftarLansia().any { it.id == "opa-damar-demo" })
                assertTrue(admin.buatAkun(email, kataSandi, PeranAkun.KELUARGA, "opa-damar-demo"))
                assertFalse(admin.buatAkun(email, kataSandi, PeranAkun.KELUARGA, "opa-damar-demo"))
                assertEquals(adminUid, firebase.auth.currentUser?.uid)
                assertNull(firebase.authPendaftaran.currentUser)

                firebase.auth.signOut()
                val pengguna = FirebaseObatRepository(firebase.auth, firebase.firestore)
                pengguna.masuk(email, kataSandi)
                val rencana = pengguna.amatiRencana().first()
                assertEquals("Opa Damar", rencana.namaLansia)
                assertFalse(rencana.bisaMengubah)
            } finally {
                firebase.auth.signOut()
            }
        }
    }
}
