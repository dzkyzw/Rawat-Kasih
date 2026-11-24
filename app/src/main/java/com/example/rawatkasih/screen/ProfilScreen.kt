package com.example.rawatkasih.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun ProfilScreen(
    email: String?,
    peran: String?,
    sedangGantiSandi: Boolean,
    sandiDiubah: Boolean,
    pesanGantiSandi: String?,
    onGantiSandi: (String, String, String) -> Unit,
    onTutupDialogSandi: () -> Unit,
    onKembali: () -> Unit
) {
    var bukaDialogSandi by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onKembali)

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onKembali) { Text("Kembali") }
            Text("Profil", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (email != null) {
                        Text("Email", style = MaterialTheme.typography.labelMedium)
                        Text(email, style = MaterialTheme.typography.bodyLarge)
                    }
                    val labelPeran = when (peran) {
                        "ADMIN" -> "Admin"
                        "KELUARGA" -> "Keluarga"
                        "PRAMURUKTI" -> "Pramurukti"
                        else -> null
                    }
                    if (labelPeran != null) Text(labelPeran, style = MaterialTheme.typography.bodyMedium)
                }
            }
            OutlinedButton(onClick = { bukaDialogSandi = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Ganti kata sandi")
            }
        }
    }
    if (bukaDialogSandi) {
        GantiKataSandiDialog(
            sedangMenyimpan = sedangGantiSandi,
            berhasil = sandiDiubah,
            pesanError = pesanGantiSandi,
            onTutup = {
                bukaDialogSandi = false
                onTutupDialogSandi()
            },
            onSimpan = onGantiSandi
        )
    }
}
