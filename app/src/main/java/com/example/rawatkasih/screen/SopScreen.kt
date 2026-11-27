package com.example.rawatkasih.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rawatkasih.R
import com.example.rawatkasih.feature.galeri.model.KelompokSop
import com.example.rawatkasih.feature.galeri.model.PustakaSop
import com.example.rawatkasih.feature.galeri.model.SopP3K
import com.example.rawatkasih.ui.galeri.P3Banner
import com.example.rawatkasih.ui.galeri.P3BarisAksi
import com.example.rawatkasih.ui.galeri.P3CatatanKaki
import com.example.rawatkasih.ui.galeri.P3Field
import com.example.rawatkasih.ui.galeri.P3Garis
import com.example.rawatkasih.ui.galeri.P3JudulHalaman
import com.example.rawatkasih.ui.galeri.P3Kartu
import com.example.rawatkasih.ui.galeri.P3KepalaBagian
import com.example.rawatkasih.ui.galeri.P3Nada
import com.example.rawatkasih.ui.galeri.P3Teks
import com.example.rawatkasih.ui.galeri.P3Tepi
import com.example.rawatkasih.ui.galeri.P3Tombol
import com.example.rawatkasih.ui.galeri.P3TopBarKembali
import com.example.rawatkasih.ui.galeri.P3Warna

private fun ikonKelompok(k: KelompokSop) = when (k) {
    KelompokSop.LUKA_KULIT -> R.drawable.p3_ic_bandage
    KelompokSop.MEMAR_BENGKAK -> R.drawable.p3_ic_droplet
    KelompokSop.JATUH -> R.drawable.p3_ic_alert
    KelompokSop.SUHU -> R.drawable.p3_ic_thermometer
}

/** 53 · SOP pertolongan pertama: pencarian + daftar per kategori keluhan. */
@Composable
internal fun LayarPustakaSop(onKembali: () -> Unit, onSop: (String) -> Unit) {
    var kueri by rememberSaveable { mutableStateOf("") }
    var terbuka by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().imePadding()) {
        P3TopBarKembali("SOP pertolongan pertama", onKembali)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = P3Tepi, end = P3Tepi, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            P3Field("Cari SOP", kueri, { kueri = it.take(40) }, placeholder = "Kategori atau kata kunci")
            if (kueri.isBlank()) {
                P3KepalaBagian("Per kategori keluhan", Modifier.padding(top = 8.dp))
                KelompokSop.entries.forEach { k ->
                    val isi = PustakaSop.dalam(k)
                    val langkah = isi.maxOf { it.langkah.size }
                    P3BarisAksi(ikonKelompok(k), k.label, "${isi.size} SOP · $langkah langkah", {
                        if (isi.size == 1) onSop(isi.single().id) else terbuka = if (terbuka == k.name) null else k.name
                    })
                    if (terbuka == k.name) {
                        Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            isi.forEach { sop -> P3BarisAksi(R.drawable.p3_ic_book, sop.judul, "${sop.langkah.size} langkah", { onSop(sop.id) }) }
                        }
                    }
                }
            } else {
                val hasil = PustakaSop.cariTeks(kueri)
                P3KepalaBagian("Hasil pencarian", Modifier.padding(top = 8.dp))
                if (hasil.isEmpty()) Text("Tidak ada SOP yang cocok dengan \"${kueri.trim()}\".", style = P3Teks.BodySmall.copy(color = P3Warna.Muted))
                hasil.forEach { sop -> P3BarisAksi(ikonKelompok(sop.kelompok), sop.judul, sop.kelompok.label, { onSop(sop.id) }) }
            }
            Box(Modifier.padding(top = 8.dp)) {
                P3Banner(P3Nada.WARN, "Contoh referensi panti", "Isi SOP harus divalidasi tenaga kesehatan panti sebelum dipakai.")
            }
        }
    }
}

/** 54–57 · Satu SOP: lima langkah pertama, kapan harus menghubungi perawat/dokter, dan aksi lanjut. */
@Composable
internal fun LayarSop(sop: SopP3K, onKembali: () -> Unit, labelAksi: String?, onAksi: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        P3TopBarKembali("SOP · ${sop.labelKategori}", onKembali)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = P3Tepi, end = P3Tepi, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            P3JudulHalaman(sop.judul, "Lima langkah pertama · ${sop.labelKategori}")
            P3Kartu(padding = 16.dp) {
                Column {
                    sop.langkah.forEachIndexed { i, langkah ->
                        if (i > 0) P3Garis()
                        Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(Modifier.size(28.dp).background(P3Warna.OkWash, CircleShape), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", style = P3Teks.Label.copy(color = P3Warna.BrandInk))
                            }
                            Text(langkah, Modifier.padding(top = 2.dp), style = P3Teks.Body)
                        }
                    }
                }
            }
            P3Banner(P3Nada.BAHAYA, "Segera hubungi perawat atau dokter", sop.peringatan)
            P3CatatanKaki("Contoh referensi panti, bukan pengganti penilaian tenaga kesehatan.")
        }
        if (labelAksi != null) {
            Box(Modifier.padding(start = P3Tepi, end = P3Tepi, bottom = 20.dp)) {
                P3Tombol(labelAksi, onAksi, ikon = if (labelAksi.startsWith("Tulis")) R.drawable.p3_ic_pencil else R.drawable.p3_ic_camera)
            }
        }
    }
}
