package com.example.rawatkasih.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.rawatkasih.BuildConfig
import com.example.rawatkasih.MainActivity
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfilNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun ketigaPeranMembukaGantiKataSandiDariProfil() {
        assumeTrue(BuildConfig.FIREBASE_EMULATOR)
        cekProfil("admin@rawatkasih.test", "Admin", "Profil")
        cekAkunKerangka("keluarga@rawatkasih.test", "Keluarga")
        cekAkunKerangka("pramurukti@rawatkasih.test", "Pramurukti")
    }

    private fun cekProfil(email: String, peran: String, tombolProfil: String, lansia: String? = null) {
        tungguTombol("Masuk ke Rawat Kasih")
        val kolom = compose.onAllNodes(hasSetTextAction())
        kolom[0].performTextInput(email)
        kolom[1].performTextInput("RawatKasih-Demo-Lokal")
        compose.onNodeWithText("Masuk").performClick()

        if (lansia != null) {
            tungguTombol(lansia)
            compose.onNodeWithText(lansia).performClick()
        }
        tungguTombol(tombolProfil)
        compose.onNodeWithText("Ganti kata sandi").assertDoesNotExist()
        compose.onNodeWithText(tombolProfil).performClick()
        compose.onNodeWithText(email).assertIsDisplayed()
        compose.onNodeWithText(peran).assertIsDisplayed()
        compose.onNodeWithText("Ganti kata sandi").performClick()
        compose.onNodeWithText("Kata sandi lama").assertIsDisplayed()
        compose.onNodeWithText("Batal").performClick()
        compose.onNodeWithText("Kembali").performClick()

        tungguTombol(tombolProfil)
        compose.onNodeWithText("Ganti kata sandi").assertDoesNotExist()
        compose.onNodeWithText("Keluar").performClick()
    }

    /** Keluarga dan pramurukti memakai kerangka bottom navigation: Akun adalah tab, bukan layar Profil terpisah. */
    private fun cekAkunKerangka(email: String, peran: String) {
        tungguTombol("Masuk ke Rawat Kasih")
        val kolom = compose.onAllNodes(hasSetTextAction())
        kolom[0].performTextInput(email)
        kolom[1].performTextInput("RawatKasih-Demo-Lokal")
        compose.onNodeWithText("Masuk").performClick()

        tungguTombol("Akun")
        compose.onNodeWithText("Ganti kata sandi").assertDoesNotExist()
        compose.onNodeWithText("Akun").performClick()
        compose.onNodeWithText(email).assertIsDisplayed()
        compose.onNodeWithText(peran).assertIsDisplayed()
        compose.onNodeWithText("Ganti kata sandi").performClick()
        compose.onNodeWithText("Kata sandi lama").assertIsDisplayed()
        compose.onNodeWithText("Batal").performClick()
        compose.onNodeWithText("Keluar").performClick()
    }

    private fun tungguTombol(teks: String) {
        compose.waitUntil(25_000) {
            compose.onAllNodes(hasText(teks) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
