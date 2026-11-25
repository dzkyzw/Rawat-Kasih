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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.rawatkasih.ui.theme.Arang
import com.example.rawatkasih.ui.theme.ArangLembut
import com.example.rawatkasih.ui.theme.GarisHalus
import com.example.rawatkasih.ui.theme.GayaBagianRk
import com.example.rawatkasih.ui.theme.GayaIsiRk
import com.example.rawatkasih.ui.theme.GayaJudulRk
import com.example.rawatkasih.ui.theme.GayaKeteranganRk
import com.example.rawatkasih.ui.theme.Krem
import com.example.rawatkasih.ui.theme.MerahHangat
import com.example.rawatkasih.ui.theme.SkemaRawatKasih

// Token visual = tema Rawat Kasih (sama dengan layar Fungsi 1), supaya semua layar tampil sebagai satu aplikasi.
internal val WarnaTeks = Arang
internal val WarnaSekunder = ArangLembut
internal val WarnaGaris = GarisHalus
internal val WarnaGalat = MerahHangat
internal val BentukKartu = RoundedCornerShape(16.dp)
internal val GayaJudul = GayaJudulRk
internal val GayaBagian = GayaBagianRk
internal val GayaIsi = GayaIsiRk
internal val GayaKeterangan = GayaKeteranganRk

@Composable
internal fun TemaRawat(content: @Composable () -> Unit) = MaterialTheme(colorScheme = SkemaRawatKasih, content = content)

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
    // Kartu krem tanpa garis tepi: area dibedakan oleh warna, bukan garis.
    val warna = CardDefaults.cardColors(containerColor = Krem, contentColor = WarnaTeks)
    if (onClick != null) {
        Card(onClick, modifier.fillMaxWidth(), shape = BentukKartu, colors = warna, content = isi)
    } else {
        Card(modifier.fillMaxWidth(), shape = BentukKartu, colors = warna, content = isi)
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
