package com.example.rawatkasih.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun GantiKataSandiDialog(
    sedangMenyimpan: Boolean,
    berhasil: Boolean,
    pesanError: String?,
    onTutup: () -> Unit,
    onSimpan: (String, String, String) -> Unit
) {
    var lama by remember(berhasil) { mutableStateOf("") }
    var baru by remember(berhasil) { mutableStateOf("") }
    var konfirmasi by remember(berhasil) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!sedangMenyimpan) onTutup() },
        title = { Text("Ganti kata sandi") },
        text = {
            if (berhasil) {
                Text("Kata sandi berhasil diubah. Gunakan kata sandi baru saat masuk lagi.")
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KolomSandi("Kata sandi lama", lama, { lama = it }, !sedangMenyimpan)
                    KolomSandi("Kata sandi baru", baru, { baru = it }, !sedangMenyimpan, "8–128 karakter")
                    KolomSandi("Ulangi kata sandi baru", konfirmasi, { konfirmasi = it }, !sedangMenyimpan)
                    if (pesanError != null) {
                        Text(
                            pesanError,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (berhasil) {
                TextButton(onClick = onTutup) { Text("Selesai") }
            } else {
                TextButton(
                    onClick = { onSimpan(lama, baru, konfirmasi) },
                    enabled = !sedangMenyimpan
                ) { Text(if (sedangMenyimpan) "Menyimpan…" else "Simpan") }
            }
        },
        dismissButton = {
            if (!berhasil) TextButton(onClick = onTutup, enabled = !sedangMenyimpan) { Text("Batal") }
        }
    )
}

@Composable
private fun KolomSandi(
    label: String,
    nilai: String,
    onUbah: (String) -> Unit,
    aktif: Boolean,
    keterangan: String? = null
) {
    OutlinedTextField(
        value = nilai,
        onValueChange = onUbah,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = keterangan?.let { { Text(it) } },
        singleLine = true,
        enabled = aktif,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = PasswordVisualTransformation()
    )
}
