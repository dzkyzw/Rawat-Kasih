package com.example.rawatkasih.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rawatkasih.feature.obat.model.StatusStok
import com.example.rawatkasih.feature.penugasan.model.AturanPenugasan
import com.example.rawatkasih.feature.penugasan.model.PolaShift
import com.example.rawatkasih.ui.catatan.CatatanUiState
import com.example.rawatkasih.ui.catatan.CatatanViewModel
import com.example.rawatkasih.ui.catatan.TugasLansia
import com.example.rawatkasih.ui.components.GayaBagian
import com.example.rawatkasih.ui.components.GayaIsi
import com.example.rawatkasih.ui.components.GayaKeterangan
import com.example.rawatkasih.ui.components.KartuGaris
import com.example.rawatkasih.ui.components.KepalaLayar
import com.example.rawatkasih.ui.components.PramuruktiBottomBar
import com.example.rawatkasih.ui.components.TabPramurukti
import com.example.rawatkasih.ui.components.TemaRawat
import com.example.rawatkasih.ui.components.TombolGaris
import com.example.rawatkasih.ui.components.WarnaGalat
import com.example.rawatkasih.ui.components.WarnaSekunder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Beranda pramurukti yang mendampingi banyak lansia: siapa yang sedang menjadi tanggungannya sekarang,
 * obat berikutnya, stok, dan apakah catatan shift ini sudah ditulis. Lansia di luar jam shiftnya dipisah
 * supaya tidak tercampur.
 */
@Composable
internal fun TugasRoute(
    viewModel: CatatanViewModel,
    onObat: (String) -> Unit,
    onCatatan: (String?) -> Unit,
    onProfil: () -> Unit,
    onKeluar: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TemaRawat {
        Scaffold(bottomBar = {
            PramuruktiBottomBar(
                tabAktif = TabPramurukti.LANSIA,
                tabTersedia = setOf(TabPramurukti.LANSIA, TabPramurukti.CATATAN, TabPramurukti.AKUN),
                onPilihTab = { tab ->
                    when (tab) {
                        TabPramurukti.CATATAN -> onCatatan(null)
                        TabPramurukti.AKUN -> onProfil()
                        else -> Unit
                    }
                }
            )
        }) { padding ->
            if (!state.dimuat && !state.gagalMemuat) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                Tugas(state, padding, onObat, onCatatan, onKeluar, viewModel::muat)
            }
        }
    }
}

@Composable
private fun Tugas(
    state: CatatanUiState,
    padding: PaddingValues,
    onObat: (String) -> Unit,
    onCatatan: (String?) -> Unit,
    onKeluar: () -> Unit,
    onMuatUlang: () -> Unit
) {
    val pola = state.pola
    val sekarang = Calendar.getInstance()
    // Satu kartu per lansia; bila ia memegang beberapa shift, yang sedang berjalan diutamakan.
    val perLansia = state.tugasSaya.groupBy { it.lansia.id }.values.map { daftar ->
        daftar.firstOrNull { AturanPenugasan.sedangBertugas(it.tugas, state.shiftSekarang) } ?: daftar.first()
    }
    val (aktif, nanti) = perLansia.partition { AturanPenugasan.sedangBertugas(it.tugas, state.shiftSekarang) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            KepalaLayar(
                "Tugas saya · ${state.email}", "Lansia dampingan",
                "Sekarang ${pola.nama(state.shiftSekarang).lowercase(Locale.ROOT)} (${pola.rentang(state.shiftSekarang)}) · " +
                    "${aktif.size} dari ${perLansia.size} lansia menjadi tanggungan Anda saat ini."
            )
        }
        if (state.gagalMemuat) {
            item {
                KartuGaris {
                    Text("Daftar tugas gagal dimuat.", style = GayaBagian)
                    TombolGaris("Coba lagi", onMuatUlang)
                }
            }
        }
        if (state.dimuat && perLansia.isEmpty()) {
            item {
                KartuGaris {
                    Text("Belum ada lansia dampingan", style = GayaBagian)
                    Text("Pengelola panti perlu menugaskan Anda ke lansia, per shift atau penuh waktu.", style = GayaIsi, color = WarnaSekunder)
                }
            }
        }
        if (aktif.isNotEmpty()) {
            item { Judul("Sedang bertugas (${aktif.size})") }
            items(aktif, key = { "aktif-" + it.lansia.id }) { KartuTugas(it, state, pola, sekarang, true, onObat, onCatatan) }
        }
        if (nanti.isNotEmpty()) {
            item { Judul("Di luar jam shift Anda (${nanti.size})") }
            items(nanti, key = { "nanti-" + it.lansia.id }) { KartuTugas(it, state, pola, sekarang, false, onObat, onCatatan) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onMuatUlang) { Text("Muat ulang") }
                TextButton(onClick = onKeluar) { Text("Keluar") }
            }
        }
    }
}

@Composable
private fun Judul(teks: String) {
    Text(teks, Modifier.padding(top = 12.dp).semantics { heading() }, style = GayaBagian)
}

@Composable
private fun KartuTugas(
    item: TugasLansia,
    state: CatatanUiState,
    pola: PolaShift,
    sekarang: Calendar,
    bertugas: Boolean,
    onObat: (String) -> Unit,
    onCatatan: (String?) -> Unit
) {
    val id = item.lansia.id
    val tugas = item.tugas
    val obat = state.obat[id]
    val awal = AturanPenugasan.awalTugas(tugas, pola, sekarang)
    val catatanSaya = state.semua.firstOrNull { it.lansiaId == id && it.penulisId == tugas.pramuruktiId && it.waktu >= awal }

    KartuGaris {
        Text(item.lansia.nama, style = GayaBagian)
        Text(
            tugas.shiftKe?.let { "${pola.nama(it)} ${pola.rentang(it)}" + if (bertugas) " · sedang berjalan" else " · mulai ${PolaShift.jam(pola.jamMulai[it])}" }
                ?: "Penuh waktu",
            style = GayaKeterangan, color = WarnaSekunder
        )
        val berikutnya = obat?.jadwalBerikutnya
        Text(
            when {
                obat == null -> "Rencana obat belum tersedia"
                berikutnya != null -> "Obat berikutnya ${berikutnya.jam} · ${berikutnya.waktu.lowercase(Locale.ROOT)}"
                obat.jadwal.isEmpty() -> "Belum ada jadwal obat"
                else -> "Jadwal obat hari ini selesai"
            },
            style = GayaIsi
        )
        when (obat?.statusStok) {
            StatusStok.HABIS -> Text("Stok obat habis · segera isi ulang", style = GayaIsi, color = WarnaGalat, fontWeight = FontWeight.Bold)
            StatusStok.MENIPIS -> Text("Stok menipis · sisa ${obat.stok} dosis", style = GayaIsi, fontWeight = FontWeight.Bold)
            else -> Unit
        }
        if (bertugas) {
            Text(
                if (catatanSaya != null) "Catatan ${if (tugas.penuhWaktu) "hari ini" else "shift ini"} sudah ditulis pukul ${jam(catatanSaya.waktu)}"
                else "Catatan ${if (tugas.penuhWaktu) "hari ini" else "shift ini"} belum ditulis",
                style = GayaIsi,
                color = if (catatanSaya != null) WarnaSekunder else androidx.compose.ui.graphics.Color.Unspecified,
                fontWeight = if (catatanSaya != null) FontWeight.Normal else FontWeight.Bold
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TombolGaris("Obat", { onObat(id) }, Modifier.weight(1f))
            TombolGaris("Catatan", { onCatatan(id) }, Modifier.weight(1f))
        }
    }
}

private fun jam(waktu: Long): String = SimpleDateFormat("HH.mm", Locale.forLanguageTag("id")).format(Date(waktu))
