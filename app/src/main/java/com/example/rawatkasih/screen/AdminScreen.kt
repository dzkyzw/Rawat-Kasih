package com.example.rawatkasih.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rawatkasih.ui.admin.AdminUiState
import com.example.rawatkasih.ui.admin.AdminViewModel
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AdminRoute(
    viewModel: AdminViewModel,
    onProfil: () -> Unit,
    onKeluar: () -> Unit,
    panelShift: @Composable (AdminUiState, Boolean) -> Unit = { _, _ -> },
    panelVital: @Composable (AdminUiState, Boolean) -> Unit = { _, _ -> },
    panelKunjungan: @Composable (AdminUiState, Boolean) -> Unit = { _, _ -> }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val aktif = !state.sedangMemuat && !state.sedangMenyimpan
    LaunchedEffect(viewModel) { viewModel.muat() }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Pengelola Panti", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onKeluar, enabled = aktif) { Text("Keluar") }
            }
            TextButton(onClick = onProfil, enabled = aktif) { Text("Profil") }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == 0, enabled = aktif, onClick = { tab = 0 }, label = { Text("Lansia") })
                FilterChip(selected = tab == 1, enabled = aktif, onClick = { tab = 1 }, label = { Text("Pramurukti") })
                FilterChip(selected = tab == 2, enabled = aktif, onClick = { tab = 2 }, label = { Text("Shift") })
                FilterChip(selected = tab == 3, enabled = aktif, onClick = { tab = 3 }, label = { Text("Vital") })
                FilterChip(selected = tab == 4, enabled = aktif, onClick = { tab = 4 }, label = { Text("Kunjungan") })
            }
            if (!aktif) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.gagalMemuat?.let {
                PesanAdmin(it, gagal = true)
                OutlinedButton(onClick = viewModel::muat, enabled = aktif) { Text("Muat ulang") }
            }
            state.pesanError?.let { PesanAdmin(it, gagal = true) }
            state.pesanBerhasil?.let { PesanAdmin(it, gagal = false) }
            when (tab) {
                1 -> PanelPramurukti(state, aktif, viewModel)
                2 -> panelShift(state, aktif)
                3 -> panelVital(state, aktif)
                4 -> panelKunjungan(state, aktif)
                else -> PanelLansia(state, aktif, viewModel)
            }
        }
    }
}

@Composable
private fun PanelLansia(state: AdminUiState, aktif: Boolean, viewModel: AdminViewModel) {
    var lansiaId by rememberSaveable { mutableStateOf<String?>(null) }
    var idBaru by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    val profil = state.lansia.firstOrNull { it.id == lansiaId }
    val fokus = LocalFocusManager.current

    LaunchedEffect(state.lansiaTersimpan) {
        val id = state.lansiaTersimpan
        if (id == idBaru) {
            lansiaId = id
            idBaru = UUID.randomUUID().toString()
        }
    }

    PilihanAdmin("Lansia", profil?.nama, state.lansia, { it.nama }, aktif) { lansiaId = it.id }
    if (lansiaId != null) {
        TextButton(onClick = { lansiaId = null; idBaru = UUID.randomUUID().toString() }, enabled = aktif) {
            Text("Tambah lansia")
        }
    }
    Text(if (lansiaId == null) "Tambah lansia" else "Edit profil lansia", style = MaterialTheme.typography.titleMedium)

    val idForm = lansiaId ?: idBaru
    var nama by rememberSaveable(idForm) { mutableStateOf(profil?.nama.orEmpty()) }
    var kamar by rememberSaveable(idForm) { mutableStateOf(profil?.kamar.orEmpty()) }
    var email by rememberSaveable(idForm) { mutableStateOf("") }
    var kataSandi by remember(idForm) { mutableStateOf("") }
    LaunchedEffect(state.pesanBerhasil) {
        if (state.lansiaTersimpan == idForm) {
            email = ""
            kataSandi = ""
        }
    }

    KolomAdmin("Nama lansia", nama, { nama = it }, aktif)
    KolomAdmin("Nomor kamar (opsional)", kamar, { kamar = it }, aktif)
    Text("Kontak keluarga (opsional)", style = MaterialTheme.typography.titleMedium)
    val emailAktif = profil?.emailKeluarga
    if (emailAktif != null) {
        Text("Terhubung ke ${emailAktif}.")
        TombolTautan(emailAktif, aktif, viewModel)
    }
    KolomAdmin(if (emailAktif == null) "Email keluarga" else "Email keluarga pengganti", email, { email = it }, aktif, KeyboardType.Email)
    if (email.isNotBlank() || kataSandi.isNotEmpty()) {
        KolomAdmin("Kata sandi awal", kataSandi, { kataSandi = it }, aktif, KeyboardType.Password)
        if (emailAktif != null && !email.trim().equals(emailAktif, ignoreCase = true)) {
            Text("Akses akun keluarga sebelumnya akan berakhir setelah penggantian disimpan.")
        }
    }
    Button(
        onClick = { fokus.clearFocus(); viewModel.simpanLansia(idForm, nama, kamar, email, kataSandi) },
        enabled = aktif && nama.trim().length in 1..100 && kamar.trim().length <= 40,
        modifier = Modifier.fillMaxWidth()
    ) { Text("Simpan profil lansia") }
}

@Composable
private fun PanelPramurukti(state: AdminUiState, aktif: Boolean, viewModel: AdminViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var kataSandi by remember { mutableStateOf("") }
    var stafUid by rememberSaveable { mutableStateOf<String?>(null) }
    val staf = state.pramurukti.firstOrNull { it.uid == stafUid }
    val fokus = LocalFocusManager.current
    LaunchedEffect(state.emailPramuruktiTersimpan) {
        val tersimpan = state.emailPramuruktiTersimpan
        if (tersimpan != null) {
            stafUid = state.pramurukti.firstOrNull { it.email.equals(tersimpan, ignoreCase = true) }?.uid
            email = ""
            kataSandi = ""
        }
    }

    Text("Buat akun pramurukti", style = MaterialTheme.typography.titleMedium)
    KolomAdmin("Email pramurukti", email, { email = it }, aktif, KeyboardType.Email)
    KolomAdmin("Kata sandi awal", kataSandi, { kataSandi = it }, aktif, KeyboardType.Password)
    Button(
        onClick = { fokus.clearFocus(); viewModel.buatPramurukti(email, kataSandi) },
        enabled = aktif && email.isNotBlank() && kataSandi.length in 8..128,
        modifier = Modifier.fillMaxWidth()
    ) { Text("Buat akun pramurukti") }

    Text("Penugasan lansia", style = MaterialTheme.typography.titleMedium)
    PilihanAdmin("Pramurukti", staf?.email, state.pramurukti, { it.email }, aktif) { stafUid = it.uid }
    if (staf != null) {
        if ('@' in staf.email) TombolTautan(staf.email, aktif, viewModel)
        if (state.lansia.isEmpty()) Text("Tambahkan profil lansia untuk mulai menugaskan.")
        for (lansia in state.lansia) {
            val ditugaskan = lansia.id in staf.rencanaIds
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = ditugaskan,
                    onCheckedChange = { viewModel.ubahPenugasan(staf.uid, lansia.id, it) },
                    enabled = aktif,
                    modifier = Modifier.semantics { contentDescription = lansia.nama }
                )
                Text(lansia.nama)
            }
        }
    }
}

@Composable
private fun TombolTautan(email: String, aktif: Boolean, viewModel: AdminViewModel) {
    TextButton(onClick = { viewModel.kirimTautanSandi(email) }, enabled = aktif) {
        Text("Kirim tautan atur kata sandi")
    }
}

@Composable
private fun KolomAdmin(
    label: String,
    nilai: String,
    ubah: (String) -> Unit,
    aktif: Boolean,
    jenis: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = nilai,
        onValueChange = ubah,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = jenis),
        visualTransformation = if (jenis == KeyboardType.Password) PasswordVisualTransformation() else VisualTransformation.None,
        supportingText = if (jenis == KeyboardType.Password) ({ Text("Minimal 8 karakter") }) else null,
        singleLine = true,
        enabled = aktif
    )
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
