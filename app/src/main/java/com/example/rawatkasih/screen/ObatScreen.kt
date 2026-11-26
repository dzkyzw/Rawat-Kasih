package com.example.rawatkasih.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import com.example.rawatkasih.feature.obat.model.StatusStok
import com.example.rawatkasih.feature.obat.model.rencanaObatContoh
import com.example.rawatkasih.ui.components.PramuruktiBottomBar
import com.example.rawatkasih.ui.components.TabPramurukti
import com.example.rawatkasih.ui.obat.ObatUiState
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
internal fun ObatRoute(
    viewModel: ObatViewModel,
    adminContent: @Composable (onProfil: () -> Unit) -> Unit,
    catatanContent: (@Composable (onKembali: () -> Unit, onProfil: () -> Unit, onTugas: () -> Unit) -> Unit)? = null,
    // Fungsi 2 · daftar tugas pramurukti, menggantikan daftar pilihan lansia bila tersedia.
    tugasContent: (@Composable (pilih: (String) -> Unit, keluar: () -> Unit, bukaProfil: () -> Unit, bukaCatatan: () -> Unit) -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.perluMasuk) {
        MaterialTheme(colorScheme = SkemaObat) {
            ObatLoginScreen(state.sedangMasuk, state.pesanMasuk, viewModel::masuk)
        }
        return
    }

    var profilTerbuka by rememberSaveable { mutableStateOf(false) }
    var catatanTerbuka by rememberSaveable { mutableStateOf(false) }
    val layarUtama = rememberSaveableStateHolder()
    val bukaProfil = { profilTerbuka = true }
    if (profilTerbuka && viewModel.modeOnline) {
        MaterialTheme(colorScheme = SkemaObat) {
            ProfilScreen(
                email = viewModel.emailAktif,
                peran = state.peranAkun,
                sedangGantiSandi = state.sedangGantiSandi,
                sandiDiubah = state.sandiDiubah,
                pesanGantiSandi = state.pesanGantiSandi,
                onGantiSandi = viewModel::gantiKataSandi,
                onTutupDialogSandi = viewModel::resetGantiSandi,
                onKembali = { profilTerbuka = false }
            )
        }
    } else if (catatanTerbuka && catatanContent != null && state.peranAkun == "PRAMURUKTI") {
        // Fungsi 2 · Catatan shift
        catatanContent({ catatanTerbuka = false }, bukaProfil) {
            catatanTerbuka = false
            if (state.lansiaId != null) viewModel.gantiLansia()
        }
    } else {
        layarUtama.SaveableStateProvider("utama") {
            if (state.peranAkun == "ADMIN") {
                MaterialTheme(colorScheme = SkemaObat) { adminContent(bukaProfil) }
            } else {
                val bukaCatatan = { catatanTerbuka = true }.takeIf { catatanContent != null && state.peranAkun == "PRAMURUKTI" }
                KontenObat(state, viewModel, bukaProfil, bukaCatatan, tugasContent)
            }
        }
    }
}

@Composable
private fun KontenObat(
    state: ObatUiState,
    viewModel: ObatViewModel,
    bukaProfil: () -> Unit,
    bukaCatatan: (() -> Unit)?,
    tugasContent: (@Composable ((String) -> Unit, () -> Unit, () -> Unit, () -> Unit) -> Unit)?
) {
    val daftar = state.daftarLansia
    if (daftar != null && tugasContent != null && bukaCatatan != null) {
        tugasContent(viewModel::pilihLansia, viewModel::keluar, bukaProfil, bukaCatatan)
        return
    }
    if (daftar != null) {
        MaterialTheme(colorScheme = SkemaObat) {
            Scaffold { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Text("Lansia yang ditugaskan", style = MaterialTheme.typography.headlineSmall) }
                    if (daftar.isEmpty()) {
                        item { Text("Belum ada penugasan. Hubungi pengelola panti.") }
                    }
                    items(daftar, key = { it.id }) { lansia ->
                        OutlinedButton(onClick = { viewModel.pilihLansia(lansia.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(lansia.nama)
                        }
                    }
                    item {
                        TextButton(onClick = viewModel::gantiLansia) { Text("Muat ulang") }
                        if (bukaCatatan != null) TextButton(onClick = bukaCatatan) { Text("Catatan shift") }
                        TextButton(onClick = bukaProfil) { Text("Profil") }
                        TextButton(onClick = viewModel::keluar) { Text("Keluar") }
                    }
                }
            }
        }
        return
    }
    val rencana = state.rencana
    if (rencana != null) {
        ObatScreen(
            state = rencana,
            statusSimpan = state.statusSimpan,
            pesanSimpan = state.pesanSimpan,
            pesanMuat = state.pesanMuat,
            sedangMuatUlang = state.sedangMuatUlang,
            onBersihkanKesalahan = viewModel::hapusKesalahanSimpan,
            onCatat = viewModel::catat,
            onKeluar = if (viewModel.modeOnline) viewModel::keluar else null,
            onMuatUlang = if (viewModel.modeOnline) viewModel::segarkan else null,
            onProfil = if (viewModel.modeOnline) bukaProfil else null,
            onGantiLansia = if (state.lansiaId != null) viewModel::gantiLansia else null,
            onSimpanPengaturan = if (viewModel.modeOnline) viewModel::simpanPengaturan else null,
            onCatatan = bukaCatatan
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
                            Text(state.pesanMuat ?: "Jadwal obat gagal dimuat.")
                            Button(onClick = viewModel::muat) { Text("Coba lagi") }
                            if (state.lansiaId != null) {
                                TextButton(onClick = viewModel::gantiLansia) { Text("Pilih lansia lain") }
                            }
                            if (viewModel.modeOnline) {
                                TextButton(onClick = bukaProfil) { Text("Profil") }
                                TextButton(onClick = viewModel::keluar) { Text("Keluar") }
                            }
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
    pesanSimpan: String? = null,
    pesanMuat: String? = null,
    sedangMuatUlang: Boolean = false,
    onBersihkanKesalahan: () -> Unit = {},
    onCatat: (String, StatusJadwal, String) -> Unit,
    onKeluar: (() -> Unit)? = null,
    onMuatUlang: (() -> Unit)? = null,
    onGantiLansia: (() -> Unit)? = null,
    onProfil: (() -> Unit)? = null,
    onSimpanPengaturan: ((Int, Int?, String?, String?) -> Unit)? = null,
    onCatatan: (() -> Unit)? = null
) {
    var jamKonfirmasi by rememberSaveable { mutableStateOf<String?>(null) }
    var bukaStok by rememberSaveable { mutableStateOf(false) }
    val jadwal = state.jadwalBerikutnya

    MaterialTheme(colorScheme = SkemaObat) {
        Scaffold(bottomBar = {
            if (state.bisaMengubah) {
                PramuruktiBottomBar(
                    tabAktif = TabPramurukti.BERANDA,
                    tabTersedia = setOfNotNull(
                        TabPramurukti.AKUN,
                        TabPramurukti.CATATAN.takeIf { onCatatan != null },
                        TabPramurukti.LANSIA.takeIf { onCatatan != null && onGantiLansia != null }
                    ),
                    onPilihTab = if (onProfil != null && statusSimpan != StatusSimpanObat.MENYIMPAN) {
                        { tab ->
                            when (tab) {
                                TabPramurukti.CATATAN -> onCatatan?.invoke()
                                TabPramurukti.LANSIA -> onGantiLansia?.invoke()
                                else -> onProfil()
                            }
                        }
                    } else null
                )
            }
        }) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${state.namaLansia.uppercase(Locale.ROOT)} · OBAT", style = GayaKeterangan)
                        Text("Jadwal obat", Modifier.semantics { heading() }, style = GayaJudul)
                        val keterangan = when {
                            onKeluar == null -> "Senin, 16 November · Data contoh"
                            state.bisaMengubah -> "Demo online · Pramurukti"
                            else -> "Demo online · Keluarga"
                        }
                        Text(keterangan, style = GayaIsi, color = WarnaSekunder)
                        if (onKeluar != null && onMuatUlang != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val bisaKlik = statusSimpan != StatusSimpanObat.MENYIMPAN
                                TextButton(onClick = onMuatUlang, enabled = bisaKlik && !sedangMuatUlang) {
                                    Text(if (sedangMuatUlang) "Memuat…" else "Muat ulang")
                                }
                                TextButton(onClick = onKeluar, enabled = bisaKlik) { Text("Keluar") }
                            }
                        }
                        if (onProfil != null && !state.bisaMengubah) {
                            TextButton(onClick = onProfil, enabled = statusSimpan != StatusSimpanObat.MENYIMPAN) {
                                Text("Profil")
                            }
                        }
                        if (onGantiLansia != null) {
                            OutlinedButton(
                                onClick = onGantiLansia,
                                enabled = statusSimpan != StatusSimpanObat.MENYIMPAN && !sedangMuatUlang
                            ) { Text("Ganti lansia") }
                        }
                        if (pesanMuat != null) Text(pesanMuat, color = MaterialTheme.colorScheme.error)
                    }
                }
                item {
                    if (jadwal != null) {
                        KartuRingkasan(
                            "${jadwal.jam} · Jadwal ${jadwal.waktu.lowercase(Locale.ROOT)}",
                            "Nama obat · Dosis sesuai resep\nSesudah makan · Belum dikonfirmasi"
                        )
                    } else {
                        if (state.jadwal.isEmpty()) {
                            KartuRingkasan("Belum ada jadwal obat", "Jadwal muncul setelah ditambahkan oleh pramurukti.")
                        } else {
                            KartuRingkasan("Jadwal hari ini selesai", "Semua jadwal sudah dikonfirmasi.")
                        }
                    }
                }
                if (jadwal != null && state.bisaMengubah) {
                    item {
                        TombolObat("Konfirmasi sudah diberikan") {
                            onBersihkanKesalahan()
                            jamKonfirmasi = jadwal.jam
                        }
                    }
                }
                item {
                    val (judulStok, isiStok, warnaBatasStok) = when (state.statusStok) {
                        StatusStok.HABIS -> Triple(
                            "Obat habis",
                            "Sisa 0 dosis · Segera lakukan restock darurat",
                            MaterialTheme.colorScheme.error
                        )
                        StatusStok.MENIPIS -> Triple(
                            "Persediaan menipis",
                            "Sisa ${state.stok} dosis · Ajukan pengisian stok",
                            Color(0xFFB26A00)
                        )
                        StatusStok.AMAN -> Triple(
                            "Persediaan obat",
                            "Sisa ${state.stok} dosis · Batas minimum ${state.batasStok} dosis",
                            WarnaGaris
                        )
                    }
                    KartuRingkasan(judulStok, isiStok, warnaBatas = warnaBatasStok)
                }
                if (state.bisaMengubah) {
                    item {
                        TombolObat("Kelola jadwal & stok") {
                            onBersihkanKesalahan()
                            bukaStok = true
                        }
                    }
                }
                item {
                    Text(
                        if (onKeluar == null) "Riwayat hari ini" else "Riwayat pemberian",
                        Modifier.semantics { heading() },
                        style = GayaBagian
                    )
                }
                items(state.riwayat, key = { it.jam }) { catatan ->
                    if (onKeluar == null && catatan.jam == "06.00") {
                        KartuRingkasan("06.00 · Tercatat", "Pemberian dikonfirmasi oleh Rina.")
                    } else {
                        val label = if (catatan.status == StatusJadwal.DIMINUM) "Diminum" else "Dilewati"
                        val petugas = if (onKeluar == null) " · Dicatat oleh Rina." else ""
                        val alasan = if (catatan.status == StatusJadwal.DILEWATI && catatan.alasanPenolakan.isNotEmpty()) {
                            "\nAlasan: ${catatan.alasanPenolakan}"
                        } else ""
                        KartuRingkasan("${catatan.jam} · $label", "Jadwal ${catatan.waktu.lowercase(Locale.ROOT)}$petugas$alasan")
                    }
                }
            }
        }

        val jam = jamKonfirmasi
        if (jam != null && jadwal?.jam == jam && state.bisaMengubah) {
            KonfirmasiJadwal(
                jam = jam,
                tersedia = state.stok > 0,
                statusSimpan = statusSimpan,
                pesanSimpan = pesanSimpan,
                onTutup = {
                    jamKonfirmasi = null
                    onBersihkanKesalahan()
                },
                onPilih = { status, alasan -> onCatat(jam, status, alasan) }
            )
        }
        if (bukaStok && state.bisaMengubah && onSimpanPengaturan != null) {
            ObatPengaturanDialog(
                stok = state.stok,
                status = statusSimpan,
                pesan = pesanSimpan,
                onTutup = {
                    bukaStok = false
                    onBersihkanKesalahan()
                },
                onSimpan = onSimpanPengaturan
            )
        } else if (bukaStok && state.bisaMengubah) {
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
    pesanSimpan: String?,
    onTutup: () -> Unit,
    onPilih: (StatusJadwal, String) -> Unit
) {
    var alasanPenolakan by rememberSaveable(jam) { mutableStateOf("") }
    val sedangMenyimpan = statusSimpan == StatusSimpanObat.MENYIMPAN
    val pesan = when {
        sedangMenyimpan -> "Menyimpan konfirmasi…"
        statusSimpan == StatusSimpanObat.GAGAL -> pesanSimpan ?: "Konfirmasi gagal disimpan. Silakan coba lagi."
        tersedia -> "Pilih hasil pemberian obat."
        else -> "Stok habis. Jadwal dapat ditandai dilewati."
    }
    AlertDialog(
        onDismissRequest = { if (!sedangMenyimpan) onTutup() },
        title = { Text("Konfirmasi jadwal $jam") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(pesan)
                OutlinedTextField(
                    value = alasanPenolakan,
                    onValueChange = { if (it.length <= 120) alasanPenolakan = it },
                    label = { Text("Alasan jika dilewati (opsional)") },
                    singleLine = true,
                    enabled = !sedangMenyimpan,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(enabled = tersedia && !sedangMenyimpan, onClick = { onPilih(StatusJadwal.DIMINUM, "") }) {
                Text("Diminum")
            }
        },
        dismissButton = {
            TextButton(enabled = !sedangMenyimpan, onClick = { onPilih(StatusJadwal.DILEWATI, alasanPenolakan) }) {
                Text("Dilewati")
            }
        }
    )
}

@Composable
private fun KartuRingkasan(judul: String, isi: String, warnaBatas: Color = WarnaGaris) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = BentukKartu,
        border = BorderStroke(1.dp, warnaBatas)
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
    ObatScreen(state = rencanaObatContoh(), onCatat = { _, _, _ -> })
}
