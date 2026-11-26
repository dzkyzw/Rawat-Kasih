package com.example.rawatkasih.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rawatkasih.R
import com.example.rawatkasih.feature.galeri.model.FotoFisik
import com.example.rawatkasih.feature.galeri.model.MesinP3K
import com.example.rawatkasih.feature.galeri.model.namaPetugas
import com.example.rawatkasih.ui.admin.AdminUiState
import com.example.rawatkasih.ui.galeri.GaleriViewModel
import com.example.rawatkasih.ui.galeri.GambarBase64
import com.example.rawatkasih.ui.galeri.P3Banner
import com.example.rawatkasih.ui.galeri.P3Bentuk
import com.example.rawatkasih.ui.galeri.P3CatatanKaki
import com.example.rawatkasih.ui.galeri.P3Field
import com.example.rawatkasih.ui.galeri.P3JudulHalaman
import com.example.rawatkasih.ui.galeri.P3Kartu
import com.example.rawatkasih.ui.galeri.P3KepalaBagian
import com.example.rawatkasih.ui.galeri.P3Nada
import com.example.rawatkasih.ui.galeri.P3Status
import com.example.rawatkasih.ui.galeri.P3Teks
import com.example.rawatkasih.ui.galeri.P3Tombol
import com.example.rawatkasih.ui.galeri.P3Warna
import com.example.rawatkasih.ui.galeri.PantauGaleriViewModel
import com.example.rawatkasih.ui.galeri.nada

/**
 * Fungsi 4 · layar 65g "Pantau · Galeri fisik" di tab "Fisik" panel pengelola panti: ringkasan minggu ini,
 * dokumentasi yang dinilai mesin aturan perlu dokter, dan dokumentasi terbaru semua lansia.
 */
@Composable
internal fun PantauGaleriPanel(viewModel: PantauGaleriViewModel, admin: AdminUiState, aktif: Boolean) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lansia = admin.lansia.associate { it.id to it.nama }
    LaunchedEffect(lansia) { viewModel.mulai(lansia) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        P3JudulHalaman("Galeri fisik", "Pantau · 4 minggu terakhir")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Statistik("${state.ringkasan.mingguIni}", "Minggu ini", Modifier.weight(1f))
            Statistik("${state.ringkasan.lukaKulit}", "Luka kulit", Modifier.weight(1f))
            Statistik("${state.ringkasan.memarBengkak}", "Memar/bengkak", Modifier.weight(1f))
        }
        when {
            state.gagalMemuat -> P3Banner(P3Nada.BAHAYA, "Galeri gagal dimuat", "Ketuk untuk mencoba lagi.", onClick = { viewModel.mulai(lansia, paksa = true) })
            !state.dimuat -> Text("Memuat…", style = P3Teks.BodySmall)
        }
        state.pesan?.let { Text(it, style = P3Teks.BodySmall) }

        if (state.perluDokter.isNotEmpty()) {
            P3KepalaBagian("Perlu dokter (${state.perluDokter.size})")
            state.perluDokter.forEach { foto -> KartuPerluDokter(foto, state.namaLansia[foto.lansiaId] ?: "Lansia", aktif && !state.menyimpan, viewModel) }
        }

        P3KepalaBagian("Dokumentasi terbaru")
        if (state.dimuat && state.terbaru.isEmpty()) Text("Belum ada dokumentasi.", style = P3Teks.BodySmall.copy(color = P3Warna.Muted))
        state.terbaru.forEach { foto -> BarisDokumentasi(foto, state.namaLansia[foto.lansiaId] ?: "Lansia") }
        P3CatatanKaki("Foto hanya dokumentasi. Aplikasi tidak mendiagnosis; keputusan medis tetap di tangan tenaga kesehatan.")
    }
}

@Composable
private fun Statistik(angka: String, label: String, modifier: Modifier) {
    P3Kartu(modifier.height(76.dp), padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(angka, style = P3Teks.Heading.copy(fontSize = P3Teks.Title.fontSize, lineHeight = P3Teks.Title.lineHeight))
            Text(label, style = P3Teks.Micro, maxLines = 1)
        }
    }
}

@Composable
private fun BarisDokumentasi(foto: FotoFisik, nama: String) {
    P3Kartu(padding = 14.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            GambarBase64(foto.thumbnail, "Foto", Modifier.size(56.dp).clip(P3Bentuk.Md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(nama, style = P3Teks.Subhead)
                Text("${foto.kategori.label} · ${foto.lokasi} · ${GaleriViewModel.tanggalPendek(foto.waktu)}", style = P3Teks.BodySmall.copy(color = P3Warna.Muted))
                Text(foto.observasi, style = P3Teks.BodySmall, maxLines = 2)
            }
            P3Status(foto.status.label, foto.status.nada)
        }
    }
}

@Composable
private fun KartuPerluDokter(foto: FotoFisik, nama: String, aktif: Boolean, viewModel: PantauGaleriViewModel) {
    var tindakan by rememberSaveable(foto.id) { mutableStateOf("") }
    var galat by rememberSaveable(foto.id) { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BarisDokumentasi(foto, nama)
        P3Banner(
            P3Nada.BAHAYA, "Dilaporkan ${namaPetugas(foto.pengunggahEmail)} · ${GaleriViewModel.tanggalPendek(foto.waktu)}",
            foto.alasanDokter.joinToString("\n")
        )
        P3Field(
            "Tindak lanjut dokter/perawat", tindakan, { tindakan = it.take(MesinP3K.TINDAKAN_MAKS); galat = null },
            placeholder = "Mis. diperiksa dr. Rani, diberi salep resep", galat = galat
        )
        P3Tombol("Tandai sudah ditangani", { galat = viewModel.tandaiDitangani(foto, tindakan) }, ikon = R.drawable.p3_ic_check, enabled = aktif)
        Row(Modifier.fillMaxWidth().height(4.dp)) {}
    }
}
