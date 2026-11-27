package com.example.rawatkasih.feature.kunjungan.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.rawatkasih.BuildConfig
import com.example.rawatkasih.data.remote.FirebaseP04
import com.example.rawatkasih.feature.admin.data.AdminRepository
import com.example.rawatkasih.feature.kontak.KontakRepository
import com.example.rawatkasih.feature.kunjungan.model.StatusKunjungan
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Kerangka keluarga di Firebase emulator: keluarga mengajukan kunjungan dan menyimpan nomor WA sendiri, data
 * tidak valid ditolak Rules, pramurukti yang ditugaskan hanya membaca, pengelola menyetujui sekali lewat
 * collection group dan mengisi WA panti, lalu keluarga membatalkan.
 */
@RunWith(AndroidJUnit4::class)
class KunjunganFirebaseTest {
    @Test
    fun kunjunganDanNomorWa() = runBlocking {
        assumeTrue(BuildConfig.FIREBASE_EMULATOR)
        val firebase = checkNotNull(FirebaseP04.ambil(InstrumentationRegistry.getInstrumentation().targetContext))
        val sandi = "RawatKasih-Demo-Lokal"
        val id = "kunjungan-" + UUID.randomUUID().toString().take(8)
        val emailKeluarga = "anak-$id@rawatkasih.test"
        val emailStaf = "staf-$id@rawatkasih.test"
        val admin = AdminRepository(firebase.auth, firebase.authPendaftaran, firebase.firestore)
        val kunjungan = KunjunganRepository(firebase.auth, firebase.firestore)
        val kontak = KontakRepository(firebase.auth, firebase.firestore)
        val tanggal = "2099-01-15"
        suspend fun masuk(email: String) = firebase.auth.signInWithEmailAndPassword(email, sandi).await()

        withTimeout(90_000) {
            try {
                masuk("admin@rawatkasih.test")
                admin.simpanLansia(id, "Oma Besuk", "B2", emailKeluarga, sandi)
                admin.buatPramurukti(emailStaf, sandi)
                admin.ubahPenugasan(admin.daftarPramurukti().single { it.email == emailStaf }.uid, id, tugaskan = true)

                // Keluarga: mengajukan, Rules menolak jam di luar jam besuk / terlalu banyak orang / menyetujui sendiri.
                masuk(emailKeluarga)
                kunjungan.ajukan(id, tanggal, "10.00", 2, "Membawa cucu")
                ditolak { kunjungan.ajukan(id, tanggal, "22.00", 2, "") }
                ditolak { kunjungan.ajukan(id, tanggal, "10.00", 9, "") }
                val diajukan = kunjungan.amati(id).first { it.isNotEmpty() }.single()
                assertEquals(StatusKunjungan.MENUNGGU, diajukan.status)
                ditolak { kunjungan.tanggapi(diajukan, setuju = true, alasan = "") }
                kontak.simpanNomorKeluarga(id, "6281234567890")
                ditolak { kontak.simpanNomorKeluarga(id, "12345") }
                ditolak { kontak.simpanNomorPanti("6281111111111") }

                // Pramurukti yang ditugaskan: membaca kunjungan, tidak bisa mengajukan.
                masuk(emailStaf)
                assertEquals(1, kunjungan.amati(id).first().size)
                ditolak { kunjungan.ajukan(id, tanggal, "11.00", 1, "") }

                // Pengelola: semua permintaan + nomor keluarga, menyetujui sekali, mengisi WA panti.
                masuk("admin@rawatkasih.test")
                assertTrue(kunjungan.amatiSemua().first().any { it.id == diajukan.id && it.lansiaId == id })
                assertEquals("6281234567890", kontak.amatiSemuaNomorKeluarga().first()[id])
                kunjungan.tanggapi(diajukan, setuju = true, alasan = "")
                val disetujui = kunjungan.amati(id).first { it.single().status == StatusKunjungan.DISETUJUI }.single()
                ditolak { kunjungan.tanggapi(disetujui, setuju = false, alasan = "Berubah pikiran") }
                kontak.simpanNomorPanti("6281111111111")

                // Keluarga: melihat WA panti lalu membatalkan.
                masuk(emailKeluarga)
                assertEquals("6281111111111", kontak.amatiNomorPanti().first())
                kunjungan.batalkan(disetujui)
                assertEquals(
                    StatusKunjungan.DIBATALKAN,
                    kunjungan.amati(id).first { it.single().status != StatusKunjungan.DISETUJUI }.single().status
                )
            } finally {
                firebase.auth.signOut()
            }
        }
    }

    private suspend fun ditolak(aksi: suspend () -> Unit) {
        try {
            aksi()
            fail("Seharusnya ditolak Firestore Rules.")
        } catch (error: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, error.code)
        }
    }
}
