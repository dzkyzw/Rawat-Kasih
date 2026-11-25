package com.example.rawatkasih.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rawatkasih.feature.admin.data.LansiaAdmin
import com.example.rawatkasih.feature.admin.data.PramuruktiAdmin
import com.example.rawatkasih.feature.penugasan.model.Penugasan
import com.example.rawatkasih.feature.penugasan.model.PolaShift
import com.example.rawatkasih.ui.admin.AdminUiState
import com.example.rawatkasih.ui.catatan.ShiftAdminViewModel
import com.example.rawatkasih.ui.components.BentukKartu
import com.example.rawatkasih.ui.components.GayaBagian
import com.example.rawatkasih.ui.components.GayaIsi
import com.example.rawatkasih.ui.components.GayaKeterangan
import com.example.rawatkasih.ui.components.KartuGaris
import com.example.rawatkasih.ui.components.PesanGalat
import com.example.rawatkasih.ui.components.TombolUtama
import com.example.rawatkasih.ui.components.WarnaSekunder
import kotlinx.coroutines.launch

/**
 * Tab "Shift" di halaman pengelola (Fungsi 2): pola shift panti, cara jaga tiap lansia (bergantian per shift
 * atau penuh waktu), dan riwayat handover. Data lansia dan pramurukti memakai daftar milik AdminViewModel.
 * [onBerubah] memuat ulang daftar admin karena penugasan ikut mengubah rencanaIds pramurukti.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShiftAdminPanel(viewModel: ShiftAdminViewModel, admin: AdminUiState, aktif: Boolean, onBerubah: () -> Unit) {
    val pola by viewModel.pola.collectAsStateWithLifecycle()
    val lansiaId by viewModel.lansiaDipilih.collectAsStateWithLifecycle()
    val tugas by viewModel.tugas.collectAsStateWithLifecycle()
    val riwayat by viewModel.riwayat.collectAsStateWithLifecycle()
    val polaSaatIni = pola ?: run { LinearProgressIndicator(); return }

    PolaShiftEditor(polaSaatIni, aktif, viewModel, onBerubah)

    Text("Penugasan per lansia", Modifier.padding(top = 12.dp).semantics { heading() }, style = GayaBagian)
    if (admin.lansia.isEmpty()) {
        Text("Tambahkan profil lansia di tab Lansia dulu.", style = GayaIsi, color = WarnaSekunder)
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        admin.lansia.forEach { l ->
            FilterChip(l.id == lansiaId, { viewModel.pilihLansia(l.id) }, { Text(l.nama) }, enabled = aktif, shape = BentukKartu)
        }
    }
    val lansia = admin.lansia.firstOrNull { it.id == lansiaId } ?: return
    val milik = tugas ?: run { LinearProgressIndicator(); return }
    // Pesan "tersimpan" disimpan di luar editor karena editor dibuat ulang saat data baru masuk.
    var tersimpanUntuk by remember { mutableStateOf<String?>(null) }
    key(lansia.id, milik, admin.pramurukti) {
        PenugasanEditor(
            lansia, milik, admin.pramurukti, polaSaatIni, aktif, viewModel, tersimpan = tersimpanUntuk == lansia.id,
            onUbah = { tersimpanUntuk = null }, onTersimpan = { tersimpanUntuk = lansia.id; onBerubah() }
        )
    }

    Text("Riwayat handover ${lansia.nama}", Modifier.padding(top = 12.dp).semantics { heading() }, style = GayaBagian)
    if (riwayat.isEmpty()) Text("Belum ada catatan shift.", style = GayaIsi, color = WarnaSekunder)
    riwayat.forEach { KartuCatatan(it, null) }
}

@Composable
private fun PolaShiftEditor(pola: PolaShift, aktif: Boolean, viewModel: ShiftAdminViewModel, onBerubah: () -> Unit) {
    val scope = rememberCoroutineScope()
    var jam by remember(pola) { mutableStateOf(pola.jamMulai) }
    var pesan by remember { mutableStateOf<String?>(null) }
    var galat by remember { mutableStateOf<String?>(null) }
    var menyimpan by remember { mutableStateOf(false) }
    val draf = runCatching { PolaShift(jam) }.getOrNull()

    Text("Pola shift", Modifier.semantics { heading() }, style = GayaBagian)
    Text(
        "Atur berapa kali pergantian jaga dalam sehari. Panti tanpa shift cukup memakai satu shift sepanjang hari, lalu menugaskan pramurukti penuh waktu.",
        style = GayaIsi, color = WarnaSekunder
    )
    KartuGaris {
        Text("Jumlah shift", style = GayaBagian)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Tambah("−", "Kurangi shift", aktif && jam.size > 1) { jam = PolaShift.bagiRata(jam.size - 1, jam.first()).jamMulai; pesan = null }
            Text("${jam.size}", style = GayaBagian)
            Tambah("+", "Tambah shift", aktif && jam.size < 6) { jam = PolaShift.bagiRata(jam.size + 1, jam.first()).jamMulai; pesan = null }
        }
        Text("Jam dibagi rata otomatis. Geser jam mulai tiap shift bila panti punya jadwal sendiri.", style = GayaKeterangan, color = WarnaSekunder)
    }
    jam.forEachIndexed { i, mulai ->
        KartuGaris {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(draf?.nama(i) ?: "Shift ${i + 1}", style = GayaBagian)
                    Text(draf?.rentang(i) ?: "Mulai ${PolaShift.jam(mulai)}", style = GayaIsi, color = WarnaSekunder)
                }
                Tambah("−", "Majukan jam mulai", aktif) { jam = jam.toMutableList().also { it[i] = (mulai + 23) % 24 }; pesan = null }
                Text(PolaShift.jam(mulai), style = GayaIsi, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
                Tambah("+", "Mundurkan jam mulai", aktif) { jam = jam.toMutableList().also { it[i] = (mulai + 1) % 24 }; pesan = null }
            }
        }
    }
    if (draf == null) PesanGalat("Jam mulai harus berurutan dan tidak boleh sama.")
    if (jam.size < pola.jumlah) {
        Text("Pendamping pada shift yang dihapus ikut dilepas dari lansianya. Penugasan penuh waktu tidak berubah.", style = GayaKeterangan, color = WarnaSekunder)
    }
    PesanGalat(galat)
    pesan?.let { Text(it, style = GayaIsi) }
    TombolUtama(if (menyimpan) "Menyimpan…" else "Simpan pola shift", {
        val baru = draf ?: return@TombolUtama
        menyimpan = true
        scope.launch {
            galat = viewModel.simpanPola(baru)
            menyimpan = false
            if (galat == null) { pesan = "Pola shift tersimpan."; onBerubah() }
        }
    }, enabled = aktif && !menyimpan && draf != null && jam != pola.jamMulai)
}

@Composable
private fun Tambah(label: String, keterangan: String, aktif: Boolean, onClick: () -> Unit) {
    OutlinedIconButton(onClick, Modifier.size(44.dp).semantics { contentDescription = keterangan }, enabled = aktif, shape = BentukKartu) {
        Text(label, style = GayaBagian)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PenugasanEditor(
    lansia: LansiaAdmin,
    milik: List<Penugasan>,
    pramurukti: List<PramuruktiAdmin>,
    pola: PolaShift,
    aktif: Boolean,
    viewModel: ShiftAdminViewModel,
    tersimpan: Boolean,
    onUbah: () -> Unit,
    onTersimpan: () -> Unit
) {
    val scope = rememberCoroutineScope()
    // Dicentang di tab Pramurukti tanpa rincian shift = penuh waktu.
    val ditugaskan = pramurukti.filter { lansia.id in it.rencanaIds }.map { it.uid }.toSet()
    val penuhAwal = milik.firstOrNull { it.penuhWaktu }?.pramuruktiId
        ?: ditugaskan.firstOrNull { uid -> milik.none { it.pramuruktiId == uid } }
    var penuhWaktu by remember { mutableStateOf(milik.none { it.shiftKe != null } && penuhAwal != null) }
    var pramuruktiPenuh by remember { mutableStateOf(penuhAwal) }
    val perShift = remember {
        mutableStateMapOf<Int, String>().apply { milik.forEach { p -> p.shiftKe?.takeIf { it < pola.jumlah }?.let { put(it, p.pramuruktiId) } } }
    }
    var galat by remember { mutableStateOf<String?>(null) }
    var menyimpan by remember { mutableStateOf(false) }

    @Composable
    fun PilihPramurukti(terpilih: String?, pilih: (String?) -> Unit, bolehKosong: Boolean) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (bolehKosong) FilterChip(terpilih == null, { pilih(null) }, { Text("Kosong") }, enabled = aktif, shape = BentukKartu)
            pramurukti.forEach { a -> FilterChip(terpilih == a.uid, { pilih(a.uid) }, { Text(a.email) }, enabled = aktif, shape = BentukKartu) }
        }
    }

    if (pramurukti.isEmpty()) {
        Text("Belum ada akun pramurukti. Buat dulu di tab Pramurukti.", style = GayaIsi, color = WarnaSekunder)
        return
    }
    KartuGaris {
        Text("Cara jaga ${lansia.nama}", style = GayaBagian)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!penuhWaktu, { penuhWaktu = false; galat = null; onUbah() }, { Text("Bergantian per shift") }, enabled = aktif, shape = BentukKartu)
            FilterChip(penuhWaktu, { penuhWaktu = true; galat = null; onUbah() }, { Text("Penuh waktu") }, enabled = aktif, shape = BentukKartu)
        }
        Text(
            if (penuhWaktu) "Satu pramurukti merawat sepanjang hari. Tidak ada serah terima antar-shift; catatan tetap tersimpan sebagai riwayat harian."
            else "Setiap shift punya pendamping. Catatan shift sebelumnya wajib dibaca pendamping berikutnya sebelum mulai bekerja.",
            style = GayaKeterangan, color = WarnaSekunder
        )
    }
    if (penuhWaktu) {
        KartuGaris {
            Text("Pramurukti penuh waktu", style = GayaBagian)
            PilihPramurukti(pramuruktiPenuh, { pramuruktiPenuh = it; galat = null; onUbah() }, bolehKosong = false)
        }
    } else {
        (0 until pola.jumlah).forEach { ke ->
            KartuGaris {
                Text("${pola.nama(ke)} · ${pola.rentang(ke)}", style = GayaBagian)
                PilihPramurukti(perShift[ke], { uid -> if (uid == null) perShift.remove(ke) else perShift[ke] = uid; galat = null; onUbah() }, bolehKosong = true)
            }
        }
    }
    PesanGalat(galat)
    if (tersimpan) Text("Penugasan tersimpan.", style = GayaIsi)
    TombolUtama(if (menyimpan) "Menyimpan…" else "Simpan penugasan", {
        menyimpan = true
        scope.launch {
            galat = viewModel.simpanPenugasan(
                lansia.id, penuhWaktu, pramuruktiPenuh, perShift.toMap(), pramurukti.associate { it.uid to it.email }, ditugaskan
            )
            menyimpan = false
            if (galat == null) onTersimpan()
        }
    }, enabled = aktif && !menyimpan)
}
