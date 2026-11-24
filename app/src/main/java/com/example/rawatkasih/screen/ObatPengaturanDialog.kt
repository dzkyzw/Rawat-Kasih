package com.example.rawatkasih.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.rawatkasih.ui.obat.StatusSimpanObat

private val FormatJam = Regex("([01][0-9]|2[0-3])[.][0-5][0-9]")

@Composable
internal fun ObatPengaturanDialog(
    stok: Int,
    status: StatusSimpanObat,
    pesan: String?,
    onTutup: () -> Unit,
    onSimpan: (Int, Int?, String?, String?) -> Unit
) {
    val stokAwal by rememberSaveable { mutableStateOf(stok) }
    var stokInput by rememberSaveable { mutableStateOf(stokAwal.toString()) }
    var jamInput by rememberSaveable { mutableStateOf("") }
    var waktuInput by rememberSaveable { mutableStateOf("") }
    val stokBaru = stokInput.toIntOrNull()
    val jam = jamInput.trim().replace(':', '.')
    val waktu = waktuInput.trim()
    val tambahJadwal = jam.isNotEmpty() || waktu.isNotEmpty()
    val sedangMenyimpan = status == StatusSimpanObat.MENYIMPAN
    val sudahTersimpan = status == StatusSimpanObat.TERSIMPAN
    val valid = stokBaru != null && stokBaru >= 0 &&
        (!tambahJadwal || FormatJam.matches(jam) && waktu.length in 1..40)
    val adaPerubahan = stokBaru != stokAwal || tambahJadwal

    AlertDialog(
        onDismissRequest = { if (!sedangMenyimpan) onTutup() },
        title = { Text("Jadwal & stok") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (sudahTersimpan) {
                    Text("Perubahan tersimpan. Sisa stok $stok dosis.")
                } else {
                    OutlinedTextField(
                        value = stokInput,
                        onValueChange = { stokInput = it },
                        label = { Text("Stok (dosis)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = !sedangMenyimpan,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Tambah jadwal dengan mengisi jam dan keterangannya.")
                    OutlinedTextField(
                        value = jamInput,
                        onValueChange = { jamInput = it },
                        label = { Text("Jam (HH.MM)") },
                        singleLine = true,
                        enabled = !sedangMenyimpan,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = waktuInput,
                        onValueChange = { waktuInput = it },
                        label = { Text("Keterangan, misalnya Sore") },
                        singleLine = true,
                        enabled = !sedangMenyimpan,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pesan != null) Text(pesan, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            if (sudahTersimpan) {
                TextButton(onClick = onTutup) { Text("Tutup") }
            } else {
                Button(
                    enabled = valid && adaPerubahan && !sedangMenyimpan,
                    onClick = {
                        onSimpan(stokAwal, stokBaru.takeIf { it != stokAwal }, jam.ifEmpty { null }, waktu.ifEmpty { null })
                    }
                ) { Text(if (sedangMenyimpan) "Menyimpan…" else "Simpan") }
            }
        },
        dismissButton = {
            if (!sudahTersimpan) TextButton(onClick = onTutup, enabled = !sedangMenyimpan) { Text("Batal") }
        }
    )
}
