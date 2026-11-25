package com.example.rawatkasih.feature.catatan.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.rawatkasih.BuildConfig
import com.example.rawatkasih.data.remote.FirebaseP04
import com.example.rawatkasih.feature.admin.data.AdminRepository
import com.example.rawatkasih.feature.catatan.model.Handover
import com.example.rawatkasih.feature.catatan.model.Mood
import com.example.rawatkasih.feature.penugasan.data.PenugasanRepository
import com.example.rawatkasih.feature.penugasan.model.AturanPenugasan
import com.example.rawatkasih.feature.penugasan.model.PolaShift
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Alur Fungsi 2 di Firebase emulator: pengelola membuat pola 2 shift dan menugaskan Pagi/Malam, shift pagi
 * menulis, shift malam terkunci sampai membaca, lalu pola diubah dan penugasan lewat centang tetap konsisten.
 */
@RunWith(AndroidJUnit4::class)
class CatatanFirebaseTest {
    @Test
    fun handoverAntarShiftMengikutiPenugasan() = runBlocking {
        assumeTrue(BuildConfig.FIREBASE_EMULATOR)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val firebase = checkNotNull(FirebaseP04.ambil(context))
        val sandi = "RawatKasih-Demo-Lokal"
        val id = UUID.randomUUID().toString()
        val emailPagi = "pagi-$id@rawatkasih.test"
        val emailMalam = "malam-$id@rawatkasih.test"
        val admin = AdminRepository(firebase.auth, firebase.authPendaftaran, firebase.firestore)
        val penugasan = PenugasanRepository(firebase.auth, firebase.firestore)
        val catatan = CatatanRepository(firebase.firestore)

        withTimeout(90_000) {
            try {
                firebase.auth.signInWithEmailAndPassword("admin@rawatkasih.test", sandi).await()
                admin.simpanLansia(id, "Oma Shift", "B1", "", "")
                admin.buatPramurukti(emailPagi, sandi)
                admin.buatPramurukti(emailMalam, sandi)
                val staf = admin.daftarPramurukti()
                val pagi = staf.single { it.email == emailPagi }.uid
                val malam = staf.single { it.email == emailMalam }.uid
                penugasan.simpanPola(PolaShift.bagiRata(2))
                penugasan.simpanPenugasan(id, null, mapOf(0 to pagi, 1 to malam), mapOf(pagi to emailPagi, malam to emailMalam), emptySet())
                assertTrue(admin.daftarPramurukti().filter { it.uid in setOf(pagi, malam) }.all { id in it.rencanaIds })

                firebase.auth.signInWithEmailAndPassword(emailPagi, sandi).await()
                assertEquals(listOf(id), penugasan.amatiSesi().first { it?.uid == pagi }!!.lansiaIds)
                val tugas = penugasan.amatiPenugasan(id).first()
                assertEquals(emailMalam, AturanPenugasan.penerusan(tugas, id, 0, PolaShift.bagiRata(2))?.email)
                catatan.simpan(id, pagi, emailPagi, "Pagi", Mood.GELISAH, "Sulit tidur, pantau malam ini.")
                assertDitolak { catatan.simpan(id, pagi, emailPagi, "Pagi", Mood.TENANG, "pendek") }

                firebase.auth.signInWithEmailAndPassword(emailMalam, sandi).await()
                val kunci = Handover.belumDibaca(catatan.amatiCatatan(id).first { it.isNotEmpty() }, malam, setOf(id))
                assertEquals(listOf("Sulit tidur, pantau malam ini."), kunci.map { it.isi })
                catatan.tandaiDibaca(kunci, malam, emailMalam)
                val dibaca = catatan.amatiCatatan(id).first { it.single().dibacaPada != null }.single()
                assertEquals(emailMalam, dibaca.dibacaOlehNama)
                assertDitolak { catatan.tandaiDibaca(kunci, malam, emailMalam) } // hanya sekali

                // Satu shift saja: pendamping malam ikut dilepas; centang di tab Pramurukti = penuh waktu.
                firebase.auth.signInWithEmailAndPassword("admin@rawatkasih.test", sandi).await()
                penugasan.simpanPola(PolaShift(listOf(7)))
                assertTrue(id !in admin.daftarPramurukti().single { it.uid == malam }.rencanaIds)
                admin.ubahPenugasan(malam, id, true)
                val akhir = penugasan.amatiPenugasan(id).first { p -> p.any { it.pramuruktiId == malam } }
                assertNull(akhir.single { it.pramuruktiId == malam }.shiftKe)
                assertEquals(0, akhir.single { it.pramuruktiId == pagi }.shiftKe)
                admin.ubahPenugasan(malam, id, false)
                assertTrue(penugasan.amatiPenugasan(id).first { p -> p.none { it.pramuruktiId == malam } }.isNotEmpty())
            } finally {
                firebase.auth.signOut()
            }
        }
    }

    private suspend fun assertDitolak(aksi: suspend () -> Unit) {
        try {
            aksi()
            fail("Firestore harus menolak perubahan ini.")
        } catch (error: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, error.code)
        }
    }
}
