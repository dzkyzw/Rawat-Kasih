package com.example.rawatkasih.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rawatkasih.feature.kontak.bukaWhatsApp
import com.example.rawatkasih.feature.kontak.tampilNomor
import com.example.rawatkasih.feature.kunjungan.model.AturanKunjungan
import com.example.rawatkasih.feature.kunjungan.model.Kunjungan
import com.example.rawatkasih.feature.kunjungan.model.StatusKunjungan
import com.example.rawatkasih.ui.admin.AdminUiState
import com.example.rawatkasih.ui.components.BentukKartu
import com.example.rawatkasih.ui.components.GayaBagian
import com.example.rawatkasih.ui.components.GayaIsi
import com.example.rawatkasih.ui.components.GayaKeterangan
import com.example.rawatkasih.ui.components.Isian
import com.example.rawatkasih.ui.components.KartuGaris
import com.example.rawatkasih.ui.components.PesanGalat
import com.example.rawatkasih.ui.components.TombolGaris
import com.example.rawatkasih.ui.components.TombolUtama
import com.example.rawatkasih.ui.components.WarnaSekunder
import com.example.rawatkasih.ui.keluarga.KontakViewModel
import com.example.rawatkasih.ui.kunjungan.KunjunganViewModel
import com.example.rawatkasih.ui.kunjungan.KunjunganViewModel.Companion.tanggalPanjang
import com.example.rawatkasih.ui.theme.MerahHangat
import com.example.rawatkasih.ui.theme.Sage
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Ringkasan satu baris untuk beranda: kunjungan terdekat yang masih berlaku. */
internal fun ringkasKunjungan(daftar: List<Kunjungan>, hariIni: String): String {
    val k = AturanKunjungan.akanDatang(daftar, hariIni).firstOrNull() ?: return "Belum ada kunjungan terjadwal"
    val hari = if (k.tanggal == hariIni) "Hari ini" else tanggalPanjang(k.tanggal)
    return "$hari · ${k.jam} · ${k.status.label.lowercase()}"
}

/** Keluarga: kunjungan satu lansia, yang akan datang lalu riwayat. */
@Composable
internal fun KunjunganKeluargaKonten(vm: KunjunganViewModel, uidSaya: String, padding: PaddingValues, onAjukan: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val hariIni = vm.hariIni()

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Ajukan jadwal besuk. Pengelola panti akan menyetujui atau menolaknya.",
                style = GayaIsi, color = WarnaSekunder,
            )
        }
        item { TombolUtama("Ajukan kunjungan", onAjukan, enabled = !state.menyimpan) }
        when {
            state.gagalMemuat -> item { Text("Kunjungan gagal dimuat. Periksa koneksi lalu buka lagi halaman ini.", style = GayaIsi, color = MerahHangat) }
            !state.dimuat -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.daftar.isEmpty() -> item { Text("Belum ada kunjungan yang diajukan.", style = GayaIsi, color = WarnaSekunder) }
            else -> {
                bagian("Akan datang", AturanKunjungan.akanDatang(state.daftar, hariIni)) { k ->
                    KartuKunjungan(k, hariIni, null) {
                        if (k.pengajuUid == uidSaya) {
                            TextButton(onClick = { vm.batalkan(k) }, enabled = !state.menyimpan) { Text("Batalkan kunjungan", color = Sage) }
                        }
                    }
                }
                bagian("Riwayat", AturanKunjungan.riwayat(state.daftar, hariIni)) { k -> KartuKunjungan(k, hariIni, null) }
            }
        }
    }
}

/**
 * Pengelola (tab Kunjungan di AdminScreen): nomor WhatsApp panti, permintaan yang menunggu, dan jadwal yang
 * sudah disetujui. Berada di dalam Column yang sudah bisa digulir, jadi bukan LazyColumn.
 */
@Composable
internal fun PanelKunjunganAdmin(vm: KunjunganViewModel, kontak: KontakViewModel, admin: AdminUiState, aktif: Boolean) {
    val semua by vm.semua.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val nomorKeluarga by kontak.semuaNomorKeluarga.collectAsStateWithLifecycle()
    var tolak by remember { mutableStateOf<Kunjungan?>(null) }
    val nama = admin.lansia.associate { it.id to it.nama }
    val hariIni = vm.hariIni()
    val context = LocalContext.current

    NomorWaPanti(kontak, aktif)

    val daftar = semua
    if (daftar == null) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    } else {
        val menunggu = AturanKunjungan.menunggu(daftar, hariIni)
        val disetujui = AturanKunjungan.akanDatang(daftar, hariIni).filter { it.status == StatusKunjungan.DISETUJUI }
        if (menunggu.isEmpty() && disetujui.isEmpty()) {
            Text("Tidak ada permintaan kunjungan yang perlu ditanggapi.", style = GayaIsi, color = WarnaSekunder)
        }
        val chatKeluarga: @Composable (Kunjungan) -> Unit = { k ->
            nomorKeluarga[k.lansiaId]?.let { no ->
                TextButton(onClick = {
                    bukaWhatsApp(context, no, "Halo, kami dari panti mengenai kunjungan ke ${nama[k.lansiaId] ?: "lansia"} pada ${tanggalPanjang(k.tanggal)} pukul ${k.jam}.")
                }) { Text("Chat WhatsApp keluarga", color = Sage) }
            }
        }
        if (menunggu.isNotEmpty()) JudulBagian("Menunggu persetujuan")
        menunggu.forEach { k ->
            KartuKunjungan(k, hariIni, nama[k.lansiaId]) {
                TombolTanggapan(state.menyimpan || !aktif, onSetuju = { vm.tanggapi(k, true) }, onTolak = { tolak = k })
                chatKeluarga(k)
            }
        }
        if (disetujui.isNotEmpty()) JudulBagian("Terjadwal")
        disetujui.forEach { k -> KartuKunjungan(k, hariIni, nama[k.lansiaId]) { chatKeluarga(k) } }
    }
    // Ruang untuk tombol mengambang "Galeri fisik" (Fungsi 4) supaya tidak menutupi tombol terakhir.
    Spacer(Modifier.height(96.dp))
    tolak?.let { k -> DialogTolak(k, onBatal = { tolak = null }) { alasan -> vm.tanggapi(k, false, alasan); tolak = null } }
}

@Composable
private fun NomorWaPanti(kontak: KontakViewModel, aktif: Boolean) {
    val nomor by kontak.nomorPanti.collectAsStateWithLifecycle()
    val menyimpan by kontak.menyimpan.collectAsStateWithLifecycle()
    var ubah by rememberSaveable { mutableStateOf(false) }
    var isian by rememberSaveable { mutableStateOf("") }
    var galat by rememberSaveable { mutableStateOf<String?>(null) }
    KartuGaris {
        Text("WhatsApp panti", style = GayaBagian)
        if (!ubah && nomor != null) {
            Text(tampilNomor(nomor!!), style = GayaIsi)
            Text("Keluarga menekan \"Chat WhatsApp panti\" untuk menghubungi nomor ini.", style = GayaKeterangan, color = WarnaSekunder)
            TombolGaris("Ganti nomor", { ubah = true; isian = "" }, enabled = aktif)
        } else {
            Text("Isi nomor agar keluarga bisa menghubungi panti lewat WhatsApp.", style = GayaIsi, color = WarnaSekunder)
            Isian("Nomor WhatsApp panti", isian, { isian = it; galat = null }, jenis = KeyboardType.Phone, galat = galat != null)
            PesanGalat(galat)
            TombolUtama(if (menyimpan) "Menyimpan…" else "Simpan nomor", {
                galat = kontak.simpanNomorPanti(isian)
                if (galat == null) ubah = false
            }, enabled = aktif && !menyimpan)
        }
    }
}

@Composable
private fun JudulBagian(judul: String) {
    Text(judul, Modifier.padding(top = 8.dp).semantics { heading() }, style = GayaBagian)
}

private fun LazyListScope.bagian(judul: String, daftar: List<Kunjungan>, kartu: @Composable (Kunjungan) -> Unit) {
    if (daftar.isEmpty()) return
    item { JudulBagian(judul) }
    items(daftar, key = { "${it.lansiaId}/${it.id}" }) { kartu(it) }
}

@Composable
private fun KartuKunjungan(k: Kunjungan, hariIni: String, namaLansia: String?, aksi: @Composable () -> Unit = {}) {
    KartuGaris {
        if (namaLansia != null) Text(namaLansia, style = GayaKeterangan, color = WarnaSekunder)
        Text("${if (k.tanggal == hariIni) "Hari ini" else tanggalPanjang(k.tanggal)} · ${k.jam}", style = GayaBagian)
        Text(
            "${k.jumlahOrang} orang · ${k.status.label}",
            style = GayaIsi, fontWeight = FontWeight.Bold,
            color = when (k.status) {
                StatusKunjungan.DISETUJUI -> Sage
                StatusKunjungan.DITOLAK -> MerahHangat
                else -> WarnaSekunder
            },
        )
        Text("Diajukan oleh ${k.pengajuEmail}", style = GayaKeterangan, color = WarnaSekunder)
        if (k.catatan.isNotBlank()) Text("Catatan: ${k.catatan}", style = GayaIsi)
        if (k.status == StatusKunjungan.DITOLAK && k.alasan.isNotBlank()) Text("Alasan: ${k.alasan}", style = GayaIsi)
        aksi()
    }
}

@Composable
private fun TombolTanggapan(nonaktif: Boolean, onSetuju: () -> Unit, onTolak: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TombolUtama("Setujui", onSetuju, Modifier.weight(1f), enabled = !nonaktif)
        TombolGaris("Tolak", onTolak, Modifier.weight(1f), enabled = !nonaktif)
    }
}

@Composable
private fun DialogTolak(k: Kunjungan, onBatal: () -> Unit, onTolak: (String) -> Unit) {
    var alasan by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onBatal,
        title = { Text("Tolak kunjungan?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${tanggalPanjang(k.tanggal)} · ${k.jam} dari ${k.pengajuEmail}.", style = GayaIsi)
                OutlinedTextField(
                    alasan, { if (it.length <= AturanKunjungan.MAKS_CATATAN) alasan = it },
                    label = { Text("Alasan (opsional)") }, placeholder = { Text("Mis. lansia sedang kontrol ke dokter") },
                    shape = BentukKartu, minLines = 2,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onTolak(alasan) }) { Text("Tolak", color = MerahHangat) } },
        dismissButton = { TextButton(onClick = onBatal) { Text("Batal") } },
    )
}

/** Form keluarga: tanggal (kalender), jam besuk, jumlah orang, dan catatan. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FormKunjunganKonten(vm: KunjunganViewModel, padding: PaddingValues, onSelesai: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tanggal by rememberSaveable { mutableStateOf<String?>(null) }
    var jam by rememberSaveable { mutableStateOf<String?>(null) }
    var jumlah by rememberSaveable { mutableIntStateOf(1) }
    var catatan by rememberSaveable { mutableStateOf("") }
    var galat by rememberSaveable { mutableStateOf<String?>(null) }
    var kalender by rememberSaveable { mutableStateOf(false) }
    // Tutup form hanya setelah permintaan BARU tersimpan, bukan karena hitungan dari kiriman sebelumnya.
    val tersimpanAwal = rememberSaveable { state.jumlahTersimpan }
    LaunchedEffect(state.jumlahTersimpan) { if (state.jumlahTersimpan > tersimpanAwal) onSelesai() }

    val hariIni = vm.hariIni()
    val batas = vm.batasAkhir()
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tanggal", style = GayaKeterangan, color = WarnaSekunder)
                TombolGaris(tanggal?.let(::tanggalPanjang) ?: "Pilih tanggal", { kalender = true })
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Jam besuk", style = GayaKeterangan, color = WarnaSekunder)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AturanKunjungan.JAM_BESUK.forEach { j -> FilterChip(jam == j, { jam = j; galat = null }, { Text(j) }, shape = BentukKartu) }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Jumlah pengunjung", style = GayaKeterangan, color = WarnaSekunder)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedIconButton(onClick = { if (jumlah > 1) jumlah-- }, enabled = jumlah > 1, modifier = Modifier.semantics { contentDescription = "Kurangi pengunjung" }) {
                        Text("−", style = GayaBagian)
                    }
                    Text("$jumlah orang", Modifier.widthIn(min = 72.dp), style = GayaBagian, textAlign = TextAlign.Center)
                    OutlinedIconButton(
                        onClick = { if (jumlah < AturanKunjungan.MAKS_ORANG) jumlah++ }, enabled = jumlah < AturanKunjungan.MAKS_ORANG,
                        modifier = Modifier.semantics { contentDescription = "Tambah pengunjung" },
                    ) { Text("+", style = GayaBagian) }
                }
            }
        }
        item {
            OutlinedTextField(
                catatan, { if (it.length <= AturanKunjungan.MAKS_CATATAN) catatan = it },
                Modifier.fillMaxWidth(), label = { Text("Catatan (opsional)") },
                placeholder = { Text("Mis. membawa cucu, ingin makan siang bersama") }, shape = BentukKartu, minLines = 2,
            )
        }
        item { PesanGalat(galat) }
        item {
            TombolUtama(if (state.menyimpan) "Mengirim…" else "Kirim permintaan", {
                galat = vm.ajukan(tanggal, jam, jumlah, catatan)
            }, enabled = !state.menyimpan)
        }
    }

    if (kalender) {
        val utc = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") } }
        val pilihan = rememberDatePickerState(
            initialSelectedDateMillis = tanggal?.let { runCatching { utc.parse(it)?.time }.getOrNull() },
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utc.format(utcTimeMillis).let { it in hariIni..batas }
            },
        )
        DatePickerDialog(
            onDismissRequest = { kalender = false },
            confirmButton = {
                TextButton(onClick = {
                    pilihan.selectedDateMillis?.let { tanggal = utc.format(it); galat = null }
                    kalender = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { kalender = false }) { Text("Batal") } },
        ) {
            DatePicker(
                pilihan,
                showModeToggle = false,
                title = { Text("Tanggal kunjungan", Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp), style = GayaKeterangan) },
                headline = {
                    Text(
                        pilihan.selectedDateMillis?.let { tanggalPanjang(utc.format(it)) } ?: "Pilih tanggal",
                        Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp), style = GayaBagian,
                    )
                },
            )
        }
    }
}
