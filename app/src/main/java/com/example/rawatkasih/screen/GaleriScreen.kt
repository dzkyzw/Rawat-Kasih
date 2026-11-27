package com.example.rawatkasih.screen

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rawatkasih.R
import com.example.rawatkasih.feature.galeri.data.AndroidPemrosesFoto
import com.example.rawatkasih.feature.galeri.model.AlasanKoreksi
import com.example.rawatkasih.feature.galeri.model.BidangForm
import com.example.rawatkasih.feature.galeri.model.FotoFisik
import com.example.rawatkasih.feature.galeri.model.KategoriKeluhan
import com.example.rawatkasih.feature.galeri.model.Keparahan
import com.example.rawatkasih.feature.galeri.model.MesinP3K
import com.example.rawatkasih.feature.galeri.model.PustakaSop
import com.example.rawatkasih.feature.galeri.model.StatusDokumentasi
import com.example.rawatkasih.feature.galeri.model.StatusTindak
import com.example.rawatkasih.feature.galeri.model.TindakLanjut
import com.example.rawatkasih.feature.galeri.model.inisial
import com.example.rawatkasih.feature.galeri.model.namaPetugas
import com.example.rawatkasih.ui.galeri.AksiSelesai
import com.example.rawatkasih.ui.galeri.GaleriUiState
import com.example.rawatkasih.ui.galeri.GaleriViewModel
import com.example.rawatkasih.ui.galeri.GambarBase64
import com.example.rawatkasih.ui.galeri.P3AreaTeks
import com.example.rawatkasih.ui.galeri.P3Avatar
import com.example.rawatkasih.ui.galeri.P3Banner
import com.example.rawatkasih.ui.galeri.P3BarisAksi
import com.example.rawatkasih.ui.galeri.P3Bentuk
import com.example.rawatkasih.ui.galeri.P3CatatanKaki
import com.example.rawatkasih.ui.galeri.P3Field
import com.example.rawatkasih.ui.galeri.P3Garis
import com.example.rawatkasih.ui.galeri.P3Ikon
import com.example.rawatkasih.ui.galeri.P3JudulHalaman
import com.example.rawatkasih.ui.galeri.P3Kartu
import com.example.rawatkasih.ui.galeri.P3KepalaBagian
import com.example.rawatkasih.ui.galeri.P3Nada
import com.example.rawatkasih.ui.galeri.P3Pilihan
import com.example.rawatkasih.ui.galeri.P3SlotAksi
import com.example.rawatkasih.ui.galeri.P3Status
import com.example.rawatkasih.ui.galeri.P3Teks
import com.example.rawatkasih.ui.galeri.P3Tepi
import com.example.rawatkasih.ui.galeri.P3Toast
import com.example.rawatkasih.ui.galeri.P3Tombol
import com.example.rawatkasih.ui.galeri.P3TopBarBeranda
import com.example.rawatkasih.ui.galeri.P3TopBarKembali
import com.example.rawatkasih.ui.galeri.P3Warna
import com.example.rawatkasih.ui.galeri.galatDi
import com.example.rawatkasih.ui.galeri.nada
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Fungsi 4 · Galeri fisik & SOP P3K, mengikuti desain Figma "F · F4" (layar 50–57, 52f/u/k/c).
 * Navigasi internal memakai tumpukan rute sederhana agar tombol kembali sistem berperilaku sama dengan panah kembali.
 */
@Composable
internal fun GaleriRoute(viewModel: GaleriViewModel, lansiaAwal: String?, onKembali: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tumpukan by rememberSaveable { mutableStateOf(listOf(RUTE_GALERI)) }
    val rute = tumpukan.last()
    val buka = { r: String -> tumpukan = tumpukan + r }
    val kembali = {
        if (tumpukan.size > 1) tumpukan = tumpukan.dropLast(1) else onKembali()
    }
    BackHandler(onBack = kembali)

    // Setelah simpan berhasil: tutup form dan kembali ke layar asalnya.
    LaunchedEffect(state.selesai) {
        val (aksi, _) = state.selesai ?: return@LaunchedEffect
        tumpukan = when (aksi) {
            AksiSelesai.DOKUMENTASI -> listOf(RUTE_GALERI)
            AksiSelesai.TINDAK_LANJUT, AksiSelesai.KOREKSI -> tumpukan.dropLastWhile { !it.startsWith(RUTE_DETAIL) }.ifEmpty { listOf(RUTE_GALERI) }
        }
        viewModel.selesaiDitangani()
    }
    LaunchedEffect(state.toast?.id) {
        if (state.toast != null) {
            delay(4_000)
            viewModel.tutupToast()
        }
    }
    // Detail hanya mendengarkan subkoleksi selama dibuka.
    val idDetail = tumpukan.lastOrNull { it.startsWith(RUTE_DETAIL) }?.removePrefix(RUTE_DETAIL)
    LaunchedEffect(idDetail, state.foto) {
        val foto = state.cariFoto(idDetail)
        if (foto != null) viewModel.bukaDetail(foto) else if (idDetail == null) viewModel.tutupDetail()
    }

    Scaffold(containerColor = P3Warna.Paper) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.dimuat -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = P3Warna.BrandDeep) }
                rute == RUTE_TAMBAH -> LayarTambah(state, viewModel, kembali, onSop = { buka(RUTE_SOP + it) }, onPustaka = { buka(RUTE_PUSTAKA) })
                rute == RUTE_PUSTAKA -> LayarPustakaSop(onKembali = kembali, onSop = { buka(RUTE_SOP + it) })
                rute.startsWith(RUTE_SOP) -> {
                    val (sopId, fotoId) = rute.removePrefix(RUTE_SOP).split("|").let { it[0] to it.getOrNull(1) }
                    val sop = PustakaSop.cari(sopId)
                    if (sop == null) LayarHilang(kembali) else LayarSop(
                        sop = sop,
                        onKembali = kembali,
                        labelAksi = when {
                            !state.bisaMengubah -> null
                            fotoId != null && state.cariFoto(fotoId) != null -> "Tulis tindak lanjut"
                            else -> "Tambah dokumentasi"
                        },
                        onAksi = {
                            if (fotoId != null && state.cariFoto(fotoId) != null) buka(RUTE_TINDAK + fotoId)
                            else {
                                viewModel.mulaiTambah(lansiaAwal, kategoriUntuk(sop.id))
                                buka(RUTE_TAMBAH)
                            }
                        }
                    )
                }
                rute.startsWith(RUTE_DETAIL) -> {
                    val foto = state.cariFoto(rute.removePrefix(RUTE_DETAIL))
                    if (foto == null) LayarHilang(kembali) else LayarDetail(
                        foto, state, kembali,
                        onTindakLanjut = { buka(RUTE_TINDAK + foto.id) },
                        onKoreksi = { buka(RUTE_KOREKSI + foto.id) },
                        onSop = { sopId -> buka(RUTE_SOP + sopId + "|" + foto.id) }
                    )
                }
                rute.startsWith(RUTE_TINDAK) -> {
                    val foto = state.cariFoto(rute.removePrefix(RUTE_TINDAK))
                    if (foto == null) LayarHilang(kembali) else LayarTindakLanjut(foto, state, viewModel, kembali)
                }
                rute.startsWith(RUTE_KOREKSI) -> {
                    val foto = state.cariFoto(rute.removePrefix(RUTE_KOREKSI))
                    if (foto == null) LayarHilang(kembali) else LayarKoreksi(foto, state, viewModel, kembali)
                }
                else -> LayarGaleri(
                    state, viewModel, onKembali,
                    onTambah = {
                        viewModel.mulaiTambah(lansiaAwal)
                        buka(RUTE_TAMBAH)
                    },
                    onDetail = { buka(RUTE_DETAIL + it) },
                    onPustaka = { buka(RUTE_PUSTAKA) }
                )
            }
            state.toast?.let { toast ->
                P3Toast(
                    toast.pesan,
                    Modifier.align(Alignment.BottomCenter).padding(horizontal = P3Tepi, vertical = 20.dp),
                    aksi = if (toast.fotoId != null && rute == RUTE_GALERI) "Lihat" else null,
                    onAksi = {
                        viewModel.tutupToast()
                        toast.fotoId?.let { buka(RUTE_DETAIL + it) }
                    }
                )
            }
        }
    }
}

private const val RUTE_GALERI = "galeri"
private const val RUTE_TAMBAH = "tambah"
private const val RUTE_PUSTAKA = "pustaka"
private const val RUTE_SOP = "sop:"
private const val RUTE_DETAIL = "detail:"
private const val RUTE_TINDAK = "tindak:"
private const val RUTE_KOREKSI = "koreksi:"

private fun kategoriUntuk(sopId: String) = when (sopId) {
    PustakaSop.LECET.id, PustakaSop.LUKA_TEKAN.id -> KategoriKeluhan.LUKA_KULIT
    PustakaSop.MEMAR.id -> KategoriKeluhan.MEMAR
    PustakaSop.BENGKAK.id -> KategoriKeluhan.BENGKAK
    else -> KategoriKeluhan.LAINNYA
}

// ------------------------------------------------------------------------------------------------------------
// 50 · Galeri dokumentasi fisik (+ 50s toast tersimpan)
// ------------------------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayarGaleri(
    state: GaleriUiState,
    viewModel: GaleriViewModel,
    onKembali: () -> Unit,
    onTambah: () -> Unit,
    onDetail: (String) -> Unit,
    onPustaka: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        P3TopBarBeranda(inisial(namaPetugas(state.emailSaya)).take(1), onKembali)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = P3Tepi, end = P3Tepi, top = 8.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                P3JudulHalaman("Galeri fisik", "Dokumentasi kondisi lansia · 4 minggu ini")
            }
            item(span = { GridItemSpan(2) }) {
                FlowRow(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    P3Pilihan("Semua", state.filter == null, { viewModel.pilihFilter(null) })
                    KategoriKeluhan.entries.forEach { k -> P3Pilihan(k.label, state.filter == k, { viewModel.pilihFilter(k) }) }
                }
            }
            when {
                state.gagalMemuat -> item(span = { GridItemSpan(2) }) {
                    P3Banner(P3Nada.BAHAYA, "Galeri gagal dimuat", "Ketuk untuk mencoba lagi.", onClick = viewModel::muat)
                }
                state.lansia.isEmpty() -> item(span = { GridItemSpan(2) }) {
                    P3Banner(P3Nada.NETRAL, "Belum ada lansia", "Pengelola panti perlu menghubungkan akun Anda dengan lansia.")
                }
                state.fotoTampil.isEmpty() -> item(span = { GridItemSpan(2) }) {
                    Text(
                        if (state.filter == null) "Belum ada dokumentasi dalam 4 minggu terakhir." else "Belum ada dokumentasi ${state.filter.label.lowercase()}.",
                        Modifier.padding(vertical = 24.dp), style = P3Teks.BodySmall.copy(color = P3Warna.Muted)
                    )
                }
            }
            items(state.fotoTampil, key = { it.id }) { foto ->
                KartuGrid(foto, state.namaLansia(foto.lansiaId)) { onDetail(foto.id) }
            }
            if (state.bisaMengubah && state.lansia.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) { P3Tombol("Tambah dokumentasi", onTambah, ikon = R.drawable.p3_ic_camera) }
            }
            item(span = { GridItemSpan(2) }) {
                P3BarisAksi(R.drawable.p3_ic_book, "SOP pertolongan pertama", "Panduan per kategori keluhan", onPustaka)
            }
        }
    }
}

@Composable
private fun KartuGrid(foto: FotoFisik, namaLansia: String, onClick: () -> Unit) {
    val baru = System.currentTimeMillis() - foto.waktu < TimeUnit.DAYS.toMillis(1)
    Column(
        Modifier.padding(bottom = 12.dp).clip(P3Bentuk.Lg).clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "${foto.kategori.label}, $namaLansia, ${GaleriViewModel.tanggalPendek(foto.waktu)}" },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GambarBase64(foto.thumbnail, "Foto · ${GaleriViewModel.jam(foto.waktu)}", Modifier.fillMaxWidth().height(140.dp).clip(P3Bentuk.Lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(foto.kategori.label, Modifier.weight(1f), style = P3Teks.Label, maxLines = 1)
            val status = foto.status
            if (status == StatusDokumentasi.PERLU_DOKTER || (status == StatusDokumentasi.BARU && baru)) P3Status(status.label, status.nada)
        }
        Text(namaLansia, style = P3Teks.BodySmall, maxLines = 1)
        Text(GaleriViewModel.tanggalPendek(foto.waktu), style = P3Teks.DataS)
    }
}

// ------------------------------------------------------------------------------------------------------------
// 51 · Tambah dokumentasi
// ------------------------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayarTambah(state: GaleriUiState, viewModel: GaleriViewModel, onKembali: () -> Unit, onSop: (String) -> Unit, onPustaka: () -> Unit) {
    val context = LocalContext.current
    val draf = state.draf
    val rekomendasi = state.pratinjau
    var uriKamera by rememberSaveable { mutableStateOf<String?>(null) }
    var pesanKamera by rememberSaveable { mutableStateOf<String?>(null) }
    var pilihLansia by rememberSaveable { mutableStateOf(false) }
    val kamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { berhasil ->
        if (berhasil) uriKamera?.let { viewModel.prosesFoto(Uri.parse(it)) }
    }
    val galeriHp = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(viewModel::prosesFoto) }
    val bisaTambahFoto = draf.foto.size < MesinP3K.FOTO_MAKS && !state.sedangMemprosesFoto

    Column(Modifier.fillMaxSize().imePadding()) {
        P3TopBarKembali("Tambah dokumentasi", onKembali)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = P3Tepi, end = P3Tepi, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Bagian("Foto kondisi fisik", state.galatDi(BidangForm.FOTO) ?: pesanKamera) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    draf.foto.forEachIndexed { i, foto ->
                        Box(Modifier.size(104.dp)) {
                            GambarBase64(foto.thumbnail, "Foto ${i + 1} · ${GaleriViewModel.jam(foto.waktu)}", Modifier.size(104.dp).clip(P3Bentuk.Lg))
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp).clip(CircleShape).background(P3Warna.Raised)
                                    .clickable(role = Role.Button) { viewModel.hapusFotoDraf(i) }
                                    .semantics { contentDescription = "Buang foto ${i + 1}" },
                                contentAlignment = Alignment.Center
                            ) { P3Ikon(R.drawable.p3_ic_close, ukuran = 16.dp) }
                        }
                    }
                    if (state.sedangMemprosesFoto) {
                        Box(Modifier.size(104.dp).clip(P3Bentuk.Lg).background(P3Warna.Paper3), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = P3Warna.BrandDeep, strokeWidth = 2.dp)
                        }
                    }
                    if (draf.foto.size < MesinP3K.FOTO_MAKS) {
                        P3SlotAksi(R.drawable.p3_ic_camera, "Ambil foto", tegas = true, enabled = bisaTambahFoto) {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", AndroidPemrosesFoto.fileKamera(context))
                            uriKamera = uri.toString()
                            pesanKamera = try {
                                kamera.launch(uri)
                                null
                            } catch (e: ActivityNotFoundException) {
                                "Aplikasi kamera tidak ditemukan. Pilih foto dari galeri HP."
                            }
                        }
                        P3SlotAksi(R.drawable.p3_ic_image, "Dari galeri", tegas = false, enabled = bisaTambahFoto) {
                            galeriHp.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    }
                }
            }

            Box {
                P3Field(
                    label = "Lansia",
                    nilai = draf.lansiaId?.let(state::namaLansia).orEmpty(),
                    onNilai = null,
                    placeholder = "Pilih lansia",
                    galat = state.galatDi(BidangForm.LANSIA),
                    onKlik = if (state.lansia.size > 1) ({ pilihLansia = true }) else null,
                    trailing = if (state.lansia.size > 1) ({ P3Ikon(R.drawable.p3_ic_chevron_right, ukuran = 20.dp) }) else null
                )
                DropdownMenu(expanded = pilihLansia, onDismissRequest = { pilihLansia = false }) {
                    state.lansia.forEach { l ->
                        DropdownMenuItem(text = { Text(l.nama, style = P3Teks.Body) }, onClick = {
                            pilihLansia = false
                            viewModel.ubahDraf { it.copy(lansiaId = l.id) }
                        })
                    }
                }
            }

            Bagian("Kategori keluhan", state.galatDi(BidangForm.KATEGORI)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KategoriKeluhan.entries.forEach { k ->
                        P3Pilihan(k.label, draf.kategori == k, { viewModel.ubahDraf { it.copy(kategori = k) } })
                    }
                }
            }

            // Tingkat keparahan (flowchart 2.2.4): Parah/Darurat → perlu dokter + SOP darurat sementara.
            Bagian("Tingkat keparahan", state.galatDi(BidangForm.KEPARAHAN)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Keparahan.entries.forEach { p ->
                        P3Pilihan(p.label, draf.keparahan == p, { viewModel.ubahDraf { it.copy(keparahan = p) } })
                    }
                }
                draf.keparahan?.let { p ->
                    Text(p.keterangan, style = P3Teks.Caption.copy(color = if (p.butuhDokter) P3Warna.AccentInk else P3Warna.Muted))
                }
            }

            P3Field(
                label = "Lokasi pada tubuh",
                nilai = draf.lokasi,
                onNilai = { teks -> viewModel.ubahDraf { it.copy(lokasi = teks.take(MesinP3K.LOKASI_MAKS)) } },
                placeholder = "Mis. tungkai bawah kiri",
                galat = state.galatDi(BidangForm.LOKASI)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Observasi dan tindak lanjut", style = P3Teks.Label)
                P3AreaTeks(
                    draf.observasi,
                    { teks -> viewModel.ubahDraf { it.copy(observasi = teks.take(MesinP3K.OBSERVASI_MAKS)) } },
                    "Ukuran, warna, sejak kapan, dan yang sudah dilakukan",
                    galat = state.galatDi(BidangForm.OBSERVASI) != null
                )
                state.galatDi(BidangForm.OBSERVASI)?.let { Text(it, style = P3Teks.Caption.copy(color = P3Warna.AccentInk)) }
                Text(
                    "Waktu otomatis ${GaleriViewModel.tanggalJam(System.currentTimeMillis())} · petugas ${namaPetugas(state.emailSaya)}",
                    style = P3Teks.DataS
                )
            }

            // Tanda bahaya dari banner SOP terpilih; dicentang = dokumentasi ditandai perlu dokter (aturan mesin P3K).
            rekomendasi?.sop?.let { sop ->
                Bagian("Tanda bahaya", null) {
                    Text("Centang bila terlihat. Satu tanda saja menandai dokumentasi perlu dokter.", style = P3Teks.Caption)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        sop.tandaBahaya.forEach { t ->
                            val aktif = t in draf.tandaBahaya
                            P3Pilihan(t.label, aktif, {
                                viewModel.ubahDraf { d -> d.copy(tandaBahaya = if (aktif) d.tandaBahaya - t else d.tandaBahaya + t) }
                            })
                        }
                    }
                }
            }

            if (rekomendasi != null) {
                if (rekomendasi.perluDokter) {
                    P3Banner(
                        P3Nada.BAHAYA, "Segera hubungi perawat atau dokter",
                        rekomendasi.alasan.joinToString("\n") + "\nPengelola panti akan melihat dokumentasi ini di daftar perlu dokter."
                    )
                }
                if (rekomendasi.darurat) LangkahDarurat()
                val sop = rekomendasi.sop
                if (sop != null) {
                    P3Banner(P3Nada.INFO, "Ikuti SOP ${sop.judul.lowercase()}", "Foto hanya dokumentasi — tidak dipakai untuk diagnosis.", onClick = { onSop(sop.id) })
                } else {
                    P3Banner(P3Nada.NETRAL, "Belum ada SOP yang cocok", "Buka pustaka SOP pertolongan pertama.", onClick = onPustaka)
                }
                rekomendasi.sopLain?.let { lain ->
                    P3Banner(P3Nada.INFO, "Ikuti juga SOP ${lain.judul.lowercase()}", null, onClick = { onSop(lain.id) })
                }
            }

            state.pesanGagal?.let { Text(it, style = P3Teks.Caption.copy(color = P3Warna.AccentInk)) }
            P3Tombol("Simpan dokumentasi", viewModel::simpan, memuat = state.menyimpan, enabled = !state.sedangMemprosesFoto)
        }
    }
}

/** SOP pertolongan darurat sementara untuk keparahan Parah/Darurat (kartu bernomor gaya P3, seperti layar SOP). */
@Composable
private fun LangkahDarurat() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        P3KepalaBagian("Pertolongan darurat sementara")
        P3Kartu(padding = 16.dp) {
            Column {
                PustakaSop.DARURAT_SEMENTARA.forEachIndexed { i, langkah ->
                    if (i > 0) P3Garis()
                    Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(28.dp).background(P3Warna.AccentWash, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = P3Teks.Label.copy(color = P3Warna.AccentInk))
                        }
                        Text(langkah, Modifier.padding(top = 2.dp), style = P3Teks.Body)
                    }
                }
            }
        }
    }
}

/** Label bagian (P3 / Label) + isi + galat di bawahnya. */
@Composable
private fun Bagian(judul: String, galat: String?, isi: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(judul, style = P3Teks.Label)
        isi()
        if (galat != null) Text(galat, style = P3Teks.Caption.copy(color = P3Warna.AccentInk))
    }
}

// ------------------------------------------------------------------------------------------------------------
// 52 · Detail dokumentasi (+ 52u tindak lanjut tersimpan, 52c dikoreksi)
// ------------------------------------------------------------------------------------------------------------

@Composable
private fun LayarDetail(
    foto: FotoFisik,
    state: GaleriUiState,
    onKembali: () -> Unit,
    onTindakLanjut: () -> Unit,
    onKoreksi: () -> Unit,
    onSop: (String) -> Unit
) {
    val nama = state.namaLansia(foto.lansiaId)
    val detail = state.detail?.takeIf { it.fotoId == foto.id }
    val sop = PustakaSop.cari(foto.sopId)
    Column(Modifier.fillMaxSize()) {
        P3TopBarKembali("Dokumentasi $nama", onKembali)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = P3Tepi, end = P3Tepi, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FotoDetail(foto, detail?.gambar, detail?.gagalMuatGambar == true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(foto.kategori.label, style = P3Teks.Heading)
                P3Status(foto.status.label, foto.status.nada)
            }
            P3Kartu {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Fakta("Lokasi", foto.lokasi, Modifier.weight(1f))
                        Fakta("Waktu", GaleriViewModel.tanggalJam(foto.waktu), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Fakta("Petugas", namaPetugas(foto.pengunggahEmail), Modifier.weight(1f))
                        Fakta("Keparahan", foto.keparahan?.label ?: "—", Modifier.weight(1f))
                    }
                    Fakta("Observasi", foto.observasi, Modifier.fillMaxWidth())
                }
            }
            if (foto.perluDokter) {
                P3Banner(
                    if (foto.ditangani) P3Nada.OK else P3Nada.BAHAYA,
                    if (foto.ditangani) "Sudah ditangani" else "Segera hubungi perawat atau dokter",
                    foto.alasanDokter.joinToString("\n")
                )
            }
            if (foto.keparahan?.butuhDokter == true && !foto.ditangani) LangkahDarurat()

            P3KepalaBagian("Tindak lanjut")
            val daftar = detail?.tindakLanjut.orEmpty()
            if (daftar.isEmpty()) Text("Belum ada tindak lanjut.", style = P3Teks.BodySmall.copy(color = P3Warna.Muted))
            daftar.forEach { BarisTindakLanjut(it) }

            val koreksi = detail?.koreksi.orEmpty()
            if (koreksi.isNotEmpty()) {
                P3KepalaBagian("Riwayat perubahan")
                P3Kartu {
                    Column {
                        koreksi.forEach { k ->
                            EntriAudit(
                                k.perubahan.joinToString("\n"),
                                "${namaPetugas(k.olehEmail)} · ${GaleriViewModel.tanggalPendek(k.waktu)}",
                                "Alasan: ${k.alasan.label.lowercase()}.",
                                terbaru = k == koreksi.first(),
                                terakhir = false
                            )
                        }
                        EntriAudit("Dokumentasi dibuat", "${namaPetugas(foto.pengunggahEmail)} · ${GaleriViewModel.tanggalPendek(foto.waktu)}", null, terbaru = false, terakhir = true)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.bisaMengubah) {
                    P3BarisAksi(R.drawable.p3_ic_pencil, "Tambah tindak lanjut", "Selesai atau terjadwal", onTindakLanjut)
                    P3BarisAksi(R.drawable.p3_ic_image, "Koreksi dokumentasi", "Versi asli tetap tersimpan", onKoreksi)
                }
                if (sop != null) P3BarisAksi(R.drawable.p3_ic_book, "SOP ${sop.judul.lowercase()}", "Lima langkah pertama", { onSop(sop.id) })
            }
            P3CatatanKaki("Dokumentasi tidak bisa dihapus dan tidak dipakai untuk diagnosis. Kesalahan diperbaiki lewat koreksi.")
        }
    }
}

@Composable
private fun FotoDetail(foto: FotoFisik, gambar: List<String>?, gagal: Boolean) {
    val label = "Foto · ${GaleriViewModel.tanggalPendek(foto.waktu)}"
    val modifier = Modifier.fillMaxWidth().height(200.dp).clip(P3Bentuk.Lg)
    if (gambar.isNullOrEmpty()) {
        // Thumbnail tampil dulu sambil foto penuh dimuat.
        Box {
            GambarBase64(foto.thumbnail, label, modifier)
            if (!gagal && gambar == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center).size(24.dp), color = P3Warna.Raised, strokeWidth = 2.dp)
            }
        }
        return
    }
    val pager = rememberPagerState { gambar.size }
    Box {
        HorizontalPager(pager, pageSpacing = 8.dp) { i -> GambarBase64(gambar[i], label, modifier, deskripsi = "Foto ${i + 1} dari ${gambar.size}") }
        if (gambar.size > 1) {
            Text(
                "${pager.currentPage + 1}/${gambar.size}",
                Modifier.align(Alignment.TopEnd).padding(10.dp).background(P3Warna.Raised, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
                style = P3Teks.Micro.copy(color = P3Warna.Ink)
            )
        }
    }
}

@Composable
private fun Fakta(label: String, nilai: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = P3Teks.Caption)
        Text(nilai, style = P3Teks.Body)
    }
}

@Composable
private fun BarisTindakLanjut(t: TindakLanjut) {
    val petugas = namaPetugas(t.petugasEmail)
    val sub = if (t.status == StatusTindak.TERJADWAL) "${hariRelatif(t.waktu)} · $petugas" else petugas
    P3Kartu {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(GaleriViewModel.jam(t.waktu), Modifier.width(52.dp), style = P3Teks.Data.copy(color = P3Warna.BrandInk))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(t.tindakan, style = P3Teks.Subhead)
                Text(sub, style = P3Teks.BodySmall.copy(color = P3Warna.Muted))
            }
            P3Status(t.status.label, if (t.status == StatusTindak.SELESAI) P3Nada.OK else P3Nada.NETRAL)
        }
    }
}

/** "Besok, 17 Okt" / "Hari ini" / "17 Okt" untuk tindak lanjut terjadwal. */
private fun hariRelatif(ms: Long): String {
    val hariIni = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    val selisih = Math.floorDiv(ms - hariIni, TimeUnit.DAYS.toMillis(1))
    val tanggal = GaleriViewModel.tanggalPendek(ms).substringBefore(" ·")
    return when (selisih) {
        0L -> "Hari ini"
        1L -> "Besok, $tanggal"
        else -> tanggal
    }
}

@Composable
private fun EntriAudit(judul: String, oleh: String, alasan: String?, terbaru: Boolean, terakhir: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = 4.dp).size(10.dp).background(if (terbaru) P3Warna.Brand else P3Warna.RuleStrong, CircleShape))
            if (!terakhir) Box(Modifier.width(1.5.dp).height(if (alasan != null) 58.dp else 36.dp).background(P3Warna.Rule))
        }
        Column(Modifier.padding(bottom = if (terakhir) 0.dp else 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(judul, style = P3Teks.Label)
            Text(oleh, style = P3Teks.Caption)
            if (alasan != null) Text(alasan, style = P3Teks.BodySmall)
        }
    }
}

// ------------------------------------------------------------------------------------------------------------
// 52f · Tambah tindak lanjut
// ------------------------------------------------------------------------------------------------------------

@Composable
private fun LayarTindakLanjut(foto: FotoFisik, state: GaleriUiState, viewModel: GaleriViewModel, onKembali: () -> Unit) {
    val nama = state.namaLansia(foto.lansiaId)
    var tindakan by rememberSaveable(foto.id) { mutableStateOf("") }
    var status by rememberSaveable(foto.id) { mutableStateOf(StatusTindak.SELESAI) }
    var jadwal by rememberSaveable(foto.id) { mutableLongStateOf(0L) }
    var galat by rememberSaveable(foto.id) { mutableStateOf<String?>(null) }
    var sekarang by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            sekarang = System.currentTimeMillis()
        }
    }
    val pilihanJadwal = pilihanJadwal(sekarang)

    Column(Modifier.fillMaxSize().imePadding()) {
        P3TopBarKembali("Tambah tindak lanjut", onKembali)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = P3Tepi, end = P3Tepi, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            P3Kartu {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    P3Avatar(inisial(nama))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${foto.kategori.label} · $nama", style = P3Teks.Subhead)
                        Text("${foto.lokasi} · ${GaleriViewModel.tanggalPendek(foto.waktu).replace(" ·", "")}", style = P3Teks.BodySmall.copy(color = P3Warna.Muted))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Tindakan", style = P3Teks.Label)
                P3AreaTeks(tindakan, { tindakan = it.take(MesinP3K.TINDAKAN_MAKS); galat = null }, "Mis. luka dicek ulang, kering, tidak kemerahan.", galat = galat != null)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status", style = P3Teks.Label)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusTindak.entries.forEach { s ->
                        P3Pilihan(s.label, status == s, { status = s; galat = null }, tinggi = 38.dp)
                    }
                }
            }
            if (status == StatusTindak.SELESAI) {
                P3Field("Waktu", GaleriViewModel.tanggalJam(sekarang), onNilai = null, keterangan = "Terisi otomatis.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    P3Field(
                        "Waktu", if (jadwal > sekarang) GaleriViewModel.tanggalJam(jadwal) else "", onNilai = null,
                        placeholder = "Pilih jadwal di bawah", keterangan = "Jadwal tindak lanjut berikutnya."
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pilihanJadwal.forEach { (label, ms) -> P3Pilihan(label, jadwal == ms, { jadwal = ms; galat = null }) }
                    }
                }
            }
            galat?.let { Text(it, style = P3Teks.Caption.copy(color = P3Warna.AccentInk)) }
            state.pesanGagal?.let { Text(it, style = P3Teks.Caption.copy(color = P3Warna.AccentInk)) }
        }
        Box(Modifier.padding(start = P3Tepi, end = P3Tepi, bottom = 20.dp)) {
            P3Tombol("Simpan tindak lanjut", {
                galat = viewModel.tambahTindakLanjut(foto, tindakan, status, if (status == StatusTindak.SELESAI) sekarang else jadwal)
            }, memuat = state.menyimpan)
        }
    }
}

/** Tiga pilihan jadwal cepat: 3 jam lagi, besok 09.00, lusa 09.00. */
private fun pilihanJadwal(sekarang: Long): List<Pair<String, Long>> {
    fun pukul9(hariLagi: Int) = Calendar.getInstance().apply {
        timeInMillis = sekarang
        add(Calendar.DAY_OF_YEAR, hariLagi)
        set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val tigaJam = (sekarang + TimeUnit.HOURS.toMillis(3)).let { it - it % TimeUnit.MINUTES.toMillis(5) }
    return listOf("3 jam lagi" to tigaJam, "Besok 09.00" to pukul9(1), "Lusa 09.00" to pukul9(2))
}

// ------------------------------------------------------------------------------------------------------------
// 52k · Koreksi dokumentasi
// ------------------------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayarKoreksi(foto: FotoFisik, state: GaleriUiState, viewModel: GaleriViewModel, onKembali: () -> Unit) {
    var kategori by rememberSaveable(foto.id) { mutableStateOf(foto.kategori) }
    var lokasi by rememberSaveable(foto.id) { mutableStateOf(foto.lokasi) }
    var alasan by rememberSaveable(foto.id) { mutableStateOf<AlasanKoreksi?>(null) }
    var galat by rememberSaveable(foto.id) { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().imePadding()) {
        P3TopBarKembali("Koreksi dokumentasi", onKembali)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = P3Tepi, end = P3Tepi, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            P3Banner(P3Nada.INFO, "Foto dan versi asli tetap tersimpan", "Perawat dan Admin melihat versi terbaru beserta riwayat perubahannya.")
            Bagian("Kategori keluhan", null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KategoriKeluhan.entries.forEach { k -> P3Pilihan(k.label, kategori == k, { kategori = k; galat = null }, tinggi = 38.dp) }
                }
            }
            P3Field(
                "Lokasi pada tubuh", lokasi, { lokasi = it.take(MesinP3K.LOKASI_MAKS); galat = null },
                keterangan = "Sebelumnya: ${foto.lokasi.lowercase()}"
            )
            Bagian("Alasan koreksi", null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlasanKoreksi.entries.forEach { a -> P3Pilihan(a.label, alasan == a, { alasan = a; galat = null }, tinggi = 38.dp) }
                }
                Text("Wajib. Tersimpan bersama nama dan waktu Anda.", style = P3Teks.Caption)
            }
            galat?.let { Text(it, style = P3Teks.Caption.copy(color = P3Warna.AccentInk)) }
            state.pesanGagal?.let { Text(it, style = P3Teks.Caption.copy(color = P3Warna.AccentInk)) }
        }
        Box(Modifier.padding(start = P3Tepi, end = P3Tepi, bottom = 20.dp)) {
            P3Tombol("Simpan koreksi", { galat = viewModel.koreksi(foto, kategori, lokasi, alasan) }, memuat = state.menyimpan)
        }
    }
}

@Composable
private fun LayarHilang(onKembali: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        P3TopBarKembali("Dokumentasi", onKembali)
        Column(Modifier.padding(P3Tepi), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            P3Banner(P3Nada.NETRAL, "Dokumentasi tidak ditemukan", "Mungkin lebih dari 4 minggu lalu atau akses Anda berubah.")
            P3Garis()
        }
    }
}
