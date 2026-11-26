package com.example.rawatkasih.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rawatkasih.R

// Token visual mengikuti ObatScreen (Fungsi 1) supaya semua layar tampil sebagai satu aplikasi.
internal val WarnaTeks = Color(0xFF111111)
internal val WarnaSekunder = Color(0xFF595959)
internal val WarnaGaris = Color(0xFFD9D9D9)
internal val WarnaGalat = Color(0xFFB3261E)
internal val BentukKartu = RoundedCornerShape(8.dp)
internal val GayaJudul = TextStyle(fontSize = 28.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold)
internal val GayaBagian = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
internal val GayaIsi = TextStyle(fontSize = 16.sp, lineHeight = 22.sp)
internal val GayaKeterangan = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)

/** Huruf merek, hanya untuk wordmark "Rawat Kasih" (splash dan kepala form masuk). */
internal val Gilda = FontFamily(Font(R.font.gilda_display))

private val SkemaRawat = lightColorScheme(
    primary = WarnaTeks,
    onPrimary = Color.White,
    background = Color.White,
    onBackground = WarnaTeks,
    surface = Color.White,
    onSurface = WarnaTeks,
    onSurfaceVariant = WarnaSekunder,
    surfaceContainerHigh = Color.White,
    secondaryContainer = Color(0xFFE8E8E8),
    onSecondaryContainer = WarnaTeks,
    outline = WarnaGaris,
    error = WarnaGalat
)

@Composable
internal fun TemaRawat(content: @Composable () -> Unit) = MaterialTheme(colorScheme = SkemaRawat, content = content)

@Composable
internal fun KepalaLayar(keterangan: String, judul: String, sub: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(keterangan.uppercase(), style = GayaKeterangan)
        Text(judul, Modifier.semantics { heading() }, style = GayaJudul)
        if (sub != null) Text(sub, style = GayaIsi, color = WarnaSekunder)
    }
}

@Composable
internal fun KartuGaris(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isi: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
    if (onClick != null) {
        OutlinedCard(onClick, modifier.fillMaxWidth(), shape = BentukKartu, border = BorderStroke(1.dp, WarnaGaris), content = isi)
    } else {
        OutlinedCard(modifier.fillMaxWidth(), shape = BentukKartu, border = BorderStroke(1.dp, WarnaGaris), content = isi)
    }
}

/** Baris menu: judul tebal + keterangan, seluruh kartu dapat ditekan. */
@Composable
internal fun KartuMenu(judul: String, isi: String, onClick: () -> Unit) {
    KartuGaris(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(judul, style = GayaBagian)
                Text(isi, style = GayaIsi, color = WarnaSekunder)
            }
            Text("›", style = GayaJudul, color = WarnaSekunder, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
internal fun TombolUtama(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled, shape = BentukKartu) { Text(label) }
}

@Composable
internal fun TombolGaris(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick, modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled, shape = BentukKartu,
        border = BorderStroke(1.dp, WarnaGaris)
    ) { Text(label, color = WarnaTeks) }
}

@Composable
internal fun Isian(
    label: String,
    nilai: String,
    ubah: (String) -> Unit,
    modifier: Modifier = Modifier,
    sandi: Boolean = false,
    jenis: KeyboardType = KeyboardType.Text,
    galat: Boolean = false
) {
    OutlinedTextField(
        value = nilai,
        onValueChange = ubah,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        isError = galat,
        visualTransformation = if (sandi) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (sandi) KeyboardType.Password else jenis),
        shape = BentukKartu
    )
}

@Composable
internal fun PesanGalat(pesan: String?) {
    if (pesan != null) Text(pesan, style = GayaIsi, color = WarnaGalat)
}
