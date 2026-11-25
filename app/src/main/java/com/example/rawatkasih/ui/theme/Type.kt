package com.example.rawatkasih.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.rawatkasih.R

/** Quicksand: huruf bulat yang ramah, untuk nama "Rawat Kasih" dan judul. */
@OptIn(ExperimentalTextApi::class)
val Quicksand = FontFamily(
    Font(R.font.quicksand, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.quicksand, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/** Atkinson Hyperlegible: dirancang untuk keterbacaan tinggi, untuk isi dan tombol. */
val Atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

private val bawaan = Typography()

val Typography = Typography(
    displayLarge = bawaan.displayLarge.copy(fontFamily = Quicksand, fontWeight = FontWeight.Bold),
    displayMedium = bawaan.displayMedium.copy(fontFamily = Quicksand, fontWeight = FontWeight.Bold),
    displaySmall = bawaan.displaySmall.copy(fontFamily = Quicksand, fontWeight = FontWeight.Bold),
    headlineLarge = bawaan.headlineLarge.copy(fontFamily = Quicksand, fontWeight = FontWeight.Bold),
    headlineMedium = bawaan.headlineMedium.copy(fontFamily = Quicksand, fontWeight = FontWeight.Bold),
    headlineSmall = bawaan.headlineSmall.copy(fontFamily = Quicksand, fontWeight = FontWeight.Bold),
    titleLarge = bawaan.titleLarge.copy(fontFamily = Quicksand, fontWeight = FontWeight.Bold),
    titleMedium = bawaan.titleMedium.copy(fontFamily = Quicksand, fontWeight = FontWeight.Bold),
    titleSmall = bawaan.titleSmall.copy(fontFamily = Quicksand, fontWeight = FontWeight.Bold),
    bodyLarge = bawaan.bodyLarge.copy(fontFamily = Atkinson),
    bodyMedium = bawaan.bodyMedium.copy(fontFamily = Atkinson),
    bodySmall = bawaan.bodySmall.copy(fontFamily = Atkinson),
    labelLarge = bawaan.labelLarge.copy(fontFamily = Atkinson, fontWeight = FontWeight.Bold),
    labelMedium = bawaan.labelMedium.copy(fontFamily = Atkinson, fontWeight = FontWeight.Bold),
    labelSmall = bawaan.labelSmall.copy(fontFamily = Atkinson, fontWeight = FontWeight.Bold),
)

/** Gaya teks tetap (dipakai layar dengan ukuran sendiri): judul & bagian Quicksand, isi Atkinson. */
val GayaJudulRk = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp)
val GayaBagianRk = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp)
val GayaIsiRk = TextStyle(fontFamily = Atkinson, fontSize = 16.sp, lineHeight = 22.sp)
val GayaKeteranganRk = TextStyle(fontFamily = Atkinson, fontSize = 12.sp, lineHeight = 17.sp)
