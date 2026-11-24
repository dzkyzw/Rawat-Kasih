package com.example.rawatkasih.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rawatkasih.feature.admin.data.PeranAkun
import com.example.rawatkasih.ui.admin.AdminViewModel

@Composable
internal fun AdminRoute(viewModel: AdminViewModel, onKeluar: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var kataSandi by remember { mutableStateOf("") }
    var peran by rememberSaveable { mutableStateOf<PeranAkun?>(null) }
    var rencanaId by rememberSaveable { mutableStateOf<String?>(null) }
    val lansia = state.lansia.firstOrNull { it.id == rencanaId }
    val fokus = LocalFocusManager.current
    val inputAktif = !state.sedangMembuat

    LaunchedEffect(viewModel) { viewModel.muat() }
    LaunchedEffect(state.pesanBerhasil) {
        if (state.pesanBerhasil != null) {
            email = ""
            kataSandi = ""
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Buat akun", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onKeluar, enabled = inputAktif) { Text("Keluar") }
            }
            if (state.sedangMemuat) LinearProgressIndicator(Modifier.fillMaxWidth())
            val pesanDaftar = state.gagalMemuat
            if (pesanDaftar != null) {
                PesanAdmin(pesanDaftar, gagal = true)
                OutlinedButton(onClick = viewModel::muat) { Text("Coba lagi") }
            } else if (!state.sedangMemuat && state.lansia.isEmpty()) {
                Text("Belum ada rencana obat lansia yang bisa dipilih.")
                OutlinedButton(onClick = viewModel::muat) { Text("Muat ulang") }
            }
            PilihanAdmin("Peran", peran?.label, PeranAkun.entries, { it.label }, inputAktif) { peran = it }
            PilihanAdmin("Lansia", lansia?.nama, state.lansia, { it.nama }, inputAktif) { rencanaId = it.id }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = inputAktif
            )
            OutlinedTextField(
                value = kataSandi,
                onValueChange = { kataSandi = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Kata sandi awal") },
                supportingText = { Text("Minimal 8 karakter") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = inputAktif
            )
            state.pesanError?.let { PesanAdmin(it, gagal = true) }
            state.pesanBerhasil?.let { PesanAdmin(it, gagal = false) }
            Button(
                onClick = {
                    val pilihanPeran = peran
                    if (pilihanPeran != null && lansia != null) {
                        fokus.clearFocus()
                        viewModel.buatAkun(email, kataSandi, pilihanPeran, lansia.id)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = inputAktif && email.isNotBlank() && kataSandi.length in 8..128 &&
                    peran != null && lansia != null
            ) {
                Text(if (state.sedangMembuat) "Membuat akun…" else "Buat akun")
            }
        }
    }
}

@Composable
private fun <T> PilihanAdmin(
    label: String,
    terpilih: String?,
    opsi: List<T>,
    nama: (T) -> String,
    aktif: Boolean,
    onPilih: (T) -> Unit
) {
    var terbuka by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { terbuka = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = aktif && opsi.isNotEmpty()
        ) {
            Text(if (terpilih == null) "Pilih ${label.lowercase()}" else "$label · $terpilih")
        }
        DropdownMenu(expanded = terbuka, onDismissRequest = { terbuka = false }) {
            for (item in opsi) {
                DropdownMenuItem(text = { Text(nama(item)) }, onClick = {
                    terbuka = false
                    onPilih(item)
                })
            }
        }
    }
}

@Composable
private fun PesanAdmin(pesan: String, gagal: Boolean) {
    Text(
        pesan,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = if (gagal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    )
}
