package com.example.rawatkasih.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rawatkasih.feature.obat.model.RencanaObat
import com.example.rawatkasih.feature.obat.model.StatusJadwal
import com.example.rawatkasih.feature.obat.model.rencanaObatContoh
import com.example.rawatkasih.ui.components.PramuruktiBottomBar
import com.example.rawatkasih.ui.components.TabPramurukti
import com.example.rawatkasih.ui.obat.ObatViewModel
import com.example.rawatkasih.ui.obat.StatusSimpanObat
import java.util.Locale

private val WarnaTeks = Color(0xFF111111)
private val WarnaSekunder = Color(0xFF595959)
private val WarnaGaris = Color(0xFFD9D9D9)
private val BentukKartu = RoundedCornerShape(8.dp)
private val SkemaObat = lightColorScheme(
    primary = WarnaTeks,
    onPrimary = Color.White,
    background = Color.White,
    onBackground = WarnaTeks,
    surface = Color.White,
    onSurface = WarnaTeks,
    onSurfaceVariant = WarnaSekunder,
    surfaceContainerHigh = Color.White,
    outline = WarnaGaris
)
private val GayaJudul = TextStyle(fontSize = 28.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold)
private val GayaBagian = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
private val GayaIsi = TextStyle(fontSize = 16.sp, lineHeight = 22.sp)
private val GayaKeterangan = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)

@Composable
internal fun ObatRoute(viewModel: ObatViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rencana = state.rencana
    if (rencana != null) {
        ObatScreen(
            state = rencana,
            statusSimpan = state.statusSimpan,
            onBersihkanKesalahan = viewModel::hapusKesalahanSimpan,
            onCatat = viewModel::catat
        )
    } else {
        MaterialTheme(colorScheme = SkemaObat) {
            Scaffold { padding ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.gagalMemuat) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Jadwal obat gagal dimuat.")
                            Button(onClick = viewModel::muat) { Text("Coba lagi") }
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun ObatScreen(
    state: RencanaObat,
    statusSimpan: StatusSimpanObat = StatusSimpanObat.SIAP,
    onBersihkanKesalahan: () -> Unit = {},
    onCatat: (String, StatusJadwal) -> Unit
) {
    var jamKonfirmasi by rememberSaveable { mutableStateOf<String?>(null) }
    var bukaStok by rememberSaveable { mutableStateOf(false) }
    val jadwal = state.jadwalBerikutnya

    MaterialTheme(colorScheme = SkemaObat) {
        Scaffold(bottomBar = { PramuruktiBottomBar(tabAktif = TabPramurukti.BERANDA) }) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${state.namaLansia.uppercase(Locale.ROOT)} · OBAT", style = GayaKeterangan)
                        Text("Jadwal obat", Modifier.semantics { heading() }, style = GayaJudul)
                        Text("Senin, 16 November · Data contoh", style = GayaIsi, color = WarnaSekunder)
                    }
                }
                item {
                    if (jadwal != null) {
                        KartuRingkasan(
                            "${jadwal.jam} · Jadwal ${jadwal.waktu.lowercase(Locale.ROOT)}",
                            "Nama obat · Dosis sesuai resep\nSesudah makan · Belum dikonfirmasi"
                        )
                    } else {
                        KartuRingkasan("Jadwal hari ini selesai", "Semua jadwal sudah dikonfirmasi.")
                    }
                }
                if (jadwal != null) {
                    item {
                        TombolObat("Konfirmasi sudah diberikan") { jamKonfirmasi = jadwal.jam }
                    }
                }
                item {
                    KartuRingkasan(
                        if (state.stokMenipis) "Persediaan menipis" else "Persediaan obat",
                        if (state.stokMenipis) {
                            "Sisa ${state.stok} dosis · Ajukan pengisian stok"
                        } else {
                            "Sisa ${state.stok} dosis · Batas minimum ${state.batasStok} dosis"
                        }
                    )
                }
                item { TombolObat("Kelola jadwal & stok") { bukaStok = true } }
                item { Text("Riwayat hari ini", Modifier.semantics { heading() }, style = GayaBagian) }
                items(state.riwayat, key = { it.jam }) { catatan ->
                    if (catatan.jam == "06.00") {
                        KartuRingkasan("06.00 · Tercatat", "Pemberian dikonfirmasi oleh Rina.")
                    } else {
                        val label = if (catatan.status == StatusJadwal.DIMINUM) "Diminum" else "Dilewati"
                        KartuRingkasan("${catatan.jam} · $label", "Jadwal ${catatan.waktu.lowercase(Locale.ROOT)} · Dicatat oleh Rina.")
                    }
                }
            }
        }

        val jam = jamKonfirmasi
        if (jam != null && jadwal?.jam == jam) {
            KonfirmasiJadwal(
                jam = jam,
                tersedia = state.stok > 0,
                statusSimpan = statusSimpan,
                onTutup = {
                    jamKonfirmasi = null
                    onBersihkanKesalahan()
                },
                onPilih = { status -> onCatat(jam, status) }
            )
        }
        if (bukaStok) {
            AlertDialog(
                onDismissRequest = { bukaStok = false },
                title = { Text("Jadwal & stok") },
                text = {
                    val jadwalTersisa = state.jadwal.count { it.status == StatusJadwal.BELUM_DIKONFIRMASI }
                    Text("Sisa stok ${state.stok} dosis. Batas minimum ${state.batasStok} dosis.\nJadwal belum dikonfirmasi $jadwalTersisa.")
                },
                confirmButton = { TextButton(onClick = { bukaStok = false }) { Text("Tutup") } }
            )
        }
    }
}

@Composable
private fun KonfirmasiJadwal(
    jam: String,
    tersedia: Boolean,
    statusSimpan: StatusSimpanObat,
    onTutup: () -> Unit,
    onPilih: (StatusJadwal) -> Unit
) {
    val sedangMenyimpan = statusSimpan == StatusSimpanObat.MENYIMPAN
    val pesan = when {
        sedangMenyimpan -> "Menyimpan konfirmasi…"
        statusSimpan == StatusSimpanObat.GAGAL -> "Konfirmasi gagal disimpan. Silakan coba lagi."
        tersedia -> "Pilih hasil pemberian obat."
        else -> "Stok habis. Jadwal dapat ditandai dilewati."
    }
    AlertDialog(
        onDismissRequest = { if (!sedangMenyimpan) onTutup() },
        title = { Text("Konfirmasi jadwal $jam") },
        text = { Text(pesan) },
        confirmButton = {
            Button(enabled = tersedia && !sedangMenyimpan, onClick = { onPilih(StatusJadwal.DIMINUM) }) {
                Text("Diminum")
            }
        },
        dismissButton = {
            TextButton(enabled = !sedangMenyimpan, onClick = { onPilih(StatusJadwal.DILEWATI) }) {
                Text("Dilewati")
            }
        }
    )
}

@Composable
private fun KartuRingkasan(judul: String, isi: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = BentukKartu,
        border = BorderStroke(1.dp, WarnaGaris)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(judul, style = GayaBagian)
            Text(isi, style = GayaIsi, color = WarnaSekunder)
        }
    }
}

@Composable
private fun TombolObat(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = BentukKartu
    ) {
        Text(label)
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ObatScreenPreview() {
    ObatScreen(state = rencanaObatContoh(), onCatat = { _, _ -> })
}
