package com.example.rawatkasih.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Satu skema warna untuk seluruh aplikasi (layar yang memasang MaterialTheme sendiri juga memakainya). */
internal val SkemaRawatKasih = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    secondary = Terakota,
    onSecondary = Color.White,
    background = Gading,
    onBackground = Arang,
    surface = Gading,
    onSurface = Arang,
    onSurfaceVariant = ArangLembut,
    surfaceContainerLowest = Gading,
    surfaceContainerLow = Krem,
    surfaceContainer = Krem,
    surfaceContainerHigh = Krem,
    surfaceContainerHighest = KremTua,
    secondaryContainer = SageMuda,
    onSecondaryContainer = Arang,
    outline = GarisHalus,
    outlineVariant = GarisHalus,
    error = MerahHangat,
    onError = Color.White,
)

@Composable
fun RawatKasihTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SkemaRawatKasih, typography = Typography, content = content)
}
