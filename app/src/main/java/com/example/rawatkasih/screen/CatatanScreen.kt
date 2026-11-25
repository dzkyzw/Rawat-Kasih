package com.example.rawatkasih.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rawatkasih.feature.catatan.model.CatatanShift
import com.example.rawatkasih.feature.catatan.model.Mood
import com.example.rawatkasih.feature.penugasan.model.AturanPenugasan
import com.example.rawatkasih.ui.catatan.CatatanUiState
import com.example.rawatkasih.ui.catatan.CatatanViewModel
import com.example.rawatkasih.ui.catatan.StatusSimpanCatatan
import com.example.rawatkasih.ui.components.BentukKartu
import com.example.rawatkasih.ui.components.GayaBagian
import com.example.rawatkasih.ui.components.GayaIsi
import com.example.rawatkasih.ui.components.GayaKeterangan
import com.example.rawatkasih.ui.components.KartuGaris
import com.example.rawatkasih.ui.components.KepalaLayar
import com.example.rawatkasih.ui.components.PesanGalat
import com.example.rawatkasih.ui.components.PramuruktiBottomBar
import com.example.rawatkasih.ui.components.TabPramurukti
import com.example.rawatkasih.ui.components.TemaRawat
import com.example.rawatkasih.ui.components.TombolUtama
import com.example.rawatkasih.ui.components.WarnaGalat
import com.example.rawatkasih.ui.components.WarnaSekunder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Kunci handover (Fungsi 2): menutupi aplikasi pramurukti sampai catatan shift sebelumnya untuk lansia
 * dampingannya dikonfirmasi dibaca. Muncul memudar dari bawah supaya terasa sebagai "berhenti dulu".
 */
@Composable
internal fun HandoverRoute(viewModel: CatatanViewModel, konten: @Composable () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        konten()
        AnimatedVisibility(
            state.belumDibaca.isNotEmpty(),
            enter = fadeIn(tween(300)) + slideInVertically(tween(360)) { it / 16 },
            exit = fadeOut(tween(200))
        ) {
            HandoverKunci(state, viewModel::konfirmasiDibaca)
        }
    }
}

@Composable
private fun HandoverKunci(state: CatatanUiState, onKonfirmasi: () -> Unit) {
    BackHandler { /* sengaja dikunci sampai catatan dikonfirmasi */ }
    val daftar = state.belumDibaca
    TemaRawat {
        Scaffold { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    KepalaLayar(
                        "Handover shift", "Baca sebelum mulai",
                        "${daftar.size} catatan dari shift sebelumnya belum dibaca. Halaman lain terbuka setelah Anda mengonfirmasi."
                    )
                }
                items(daftar, key = { it.id }) { KartuCatatan(it, state.namaLansia(it.lansiaId)) }
                if (state.statusSimpan == StatusSimpanCatatan.GAGAL) {
                    item { Text("Konfirmasi gagal disimpan. Silakan coba lagi.", style = GayaIsi, color = WarnaGalat) }
                }
                item { TombolUtama("Saya sudah membaca semuanya", onKonfirmasi, enabled = state.statusSimpan != StatusSimpanCatatan.MENYIMPAN) }
            }
        }
    }
}

@Composable
internal fun CatatanRoute(viewModel: CatatanViewModel, onKembali: () -> Unit, onProfil: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onKembali)
    TemaRawat {
        Scaffold(bottomBar = {
            PramuruktiBottomBar(
                tabAktif = TabPramurukti.CATATAN,
                tabTersedia = setOf(TabPramurukti.BERANDA, TabPramurukti.CATATAN, TabPramurukti.AKUN),
                onPilihTab = { tab ->
                    when (tab) {
                        TabPramurukti.BERANDA -> onKembali()
                        TabPramurukti.AKUN -> onProfil()
                        else -> Unit
                    }
                }
            )
        }) { padding ->
            when {
                state.gagalMemuat -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Catatan shift gagal dimuat.")
                        Button(onClick = viewModel::muat) { Text("Coba lagi") }
                    }
                }
                !state.dimuat -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else -> CatatanScreen(state, padding, onSimpan = viewModel::simpan, onUbahForm = viewModel::hapusKesalahanSimpan)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatatanScreen(
    state: CatatanUiState,
    padding: PaddingValues,
    onSimpan: (String?, Mood?, String) -> String?,
    onUbahForm: () -> Unit
) {
    val lansiaSaya = state.tugasSaya.map { it.lansia }.distinctBy { it.id }
    var dipilih by rememberSaveable { mutableStateOf<String?>(null) }
    val lansiaId = dipilih?.takeIf { id -> lansiaSaya.any { it.id == id } } ?: lansiaSaya.firstOrNull()?.id
    var mood by rememberSaveable { mutableStateOf<Mood?>(null) }
    var isi by rememberSaveable { mutableStateOf("") }
    var galat by rememberSaveable { mutableStateOf<String?>(null) }
    var tersimpanTerakhir by rememberSaveable { mutableStateOf(state.jumlahTersimpan) }

    // Form baru dikosongkan setelah penyimpanan berhasil, supaya tulisan tidak hilang bila gagal.
    LaunchedEffect(state.jumlahTersimpan) {
        if (state.jumlahTersimpan > tersimpanTerakhir) {
            mood = null
            isi = ""
            tersimpanTerakhir = state.jumlahTersimpan
        }
    }

    val pola = state.pola
    val tugasIni = state.tugasSaya.filter { it.lansia.id == lansiaId }
    val aktif = tugasIni.firstOrNull { AturanPenugasan.sedangBertugas(it.tugas, state.shiftSekarang) } ?: tugasIni.firstOrNull()
    val penerus = aktif?.tugas?.shiftKe?.let { AturanPenugasan.penerusan(state.penugasan, lansiaId!!, it, pola) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            KepalaLayar(
                "Catatan · ${state.email}", "Catatan shift",
                "Sekarang ${pola.nama(state.shiftSekarang).lowercase(Locale.ROOT)} (${pola.rentang(state.shiftSekarang)})"
            )
        }
        if (lansiaSaya.isEmpty()) {
            item {
                KartuGaris {
                    Text("Belum ada lansia dampingan", style = GayaBagian)
                    Text("Pengelola panti perlu menugaskan Anda ke lansia, per shift atau penuh waktu.", style = GayaIsi, color = WarnaSekunder)
                }
            }
            return@LazyColumn
        }
        if (lansiaSaya.size > 1) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    lansiaSaya.forEach { l -> FilterChip(l.id == lansiaId, { dipilih = l.id; galat = null }, { Text(l.nama) }, shape = BentukKartu) }
                }
            }
        }
        item {
            val namaLansia = state.namaLansia(lansiaId.orEmpty())
            KartuGaris {
                Text("Suasana hati $namaLansia", style = GayaBagian)
                Text(tugasLabel(aktif?.tugas?.shiftKe, pola, state.shiftSekarang), style = GayaKeterangan, color = WarnaSekunder)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mood.entries.forEach { pilihan ->
                        FilterChip(mood == pilihan, { mood = pilihan; galat = null; onUbahForm() }, { Text(pilihan.label) }, shape = BentukKartu)
                    }
                }
                OutlinedTextField(
                    value = isi,
                    onValueChange = { isi = it; galat = null; onUbahForm() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Catatan untuk shift berikutnya") },
                    placeholder = { Text("Kondisi, perilaku, hal yang perlu diteruskan") },
                    minLines = 4,
                    isError = galat != null,
                    supportingText = galat?.let { pesan -> { Text(pesan) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = BentukKartu
                )
                if (state.statusSimpan == StatusSimpanCatatan.GAGAL) PesanGalat("Catatan gagal disimpan. Silakan coba lagi.")
                TombolUtama(
                    if (state.statusSimpan == StatusSimpanCatatan.MENYIMPAN) "Menyimpan…" else "Simpan catatan",
                    { galat = onSimpan(lansiaId, mood, isi) },
                    enabled = state.statusSimpan != StatusSimpanCatatan.MENYIMPAN
                )
                Text(
                    when {
                        aktif?.tugas?.penuhWaktu == true -> "Anda pendamping penuh waktu. Catatan tersimpan sebagai riwayat harian."
                        penerus != null -> "Diteruskan ke ${pola.nama(penerus.shiftKe!!).lowercase(Locale.ROOT)} · " +
                            penerus.email.ifBlank { "pramurukti" } + ". Ia wajib membacanya sebelum mulai bekerja."
                        else -> "Belum ada pendamping shift berikutnya untuk lansia ini."
                    },
                    style = GayaKeterangan, color = WarnaSekunder
                )
            }
        }
        item { Text("Riwayat catatan", Modifier.padding(top = 12.dp).semantics { heading() }, style = GayaBagian) }
        val riwayat = state.semua.filter { it.lansiaId == lansiaId }
        if (riwayat.isEmpty()) item { Text("Belum ada catatan.", style = GayaIsi, color = WarnaSekunder) }
        items(riwayat, key = { it.id }) { KartuCatatan(it, null) }
    }
}

private fun tugasLabel(shiftKe: Int?, pola: com.example.rawatkasih.feature.penugasan.model.PolaShift, sekarang: Int) = when {
    shiftKe == null -> "Tugas Anda: penuh waktu"
    shiftKe == sekarang -> "Tugas Anda: ${pola.nama(shiftKe).lowercase(Locale.ROOT)} ${pola.rentang(shiftKe)} · sedang berjalan"
    else -> "Tugas Anda: ${pola.nama(shiftKe).lowercase(Locale.ROOT)} ${pola.rentang(shiftKe)} · di luar jam shift"
}

/** Kartu satu catatan; [namaLansia] diisi bila daftar berisi banyak lansia (kunci handover, audit). */
@Composable
internal fun KartuCatatan(catatan: CatatanShift, namaLansia: String?) {
    KartuGaris {
        Text(
            "${jam(catatan.waktu)} · ${catatan.shift} · ${catatan.penulisNama}" + (namaLansia?.let { " · $it" } ?: ""),
            style = GayaKeterangan, color = WarnaSekunder
        )
        Text("Suasana hati: ${catatan.mood.label}", style = GayaBagian)
        Text(catatan.isi, style = GayaIsi)
        Text(
            if (catatan.sudahDibaca) "Dibaca ${catatan.dibacaOlehNama} pukul ${jam(catatan.dibacaPada ?: 0)}" else "Belum dibaca",
            style = GayaKeterangan,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun jam(waktu: Long): String = SimpleDateFormat("HH.mm", Locale.forLanguageTag("id")).format(Date(waktu))
