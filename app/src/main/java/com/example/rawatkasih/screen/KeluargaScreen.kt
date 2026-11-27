package com.example.rawatkasih.screen

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rawatkasih.R
import com.example.rawatkasih.feature.galeri.model.StatusDokumentasi
import com.example.rawatkasih.feature.kontak.bukaWhatsApp
import com.example.rawatkasih.feature.kontak.tampilNomor
import com.example.rawatkasih.feature.obat.model.StatusJadwal
import com.example.rawatkasih.feature.obat.model.StatusStok
import com.example.rawatkasih.ui.components.GayaBagian
import com.example.rawatkasih.ui.components.GayaIsi
import com.example.rawatkasih.ui.components.GayaKeterangan
import com.example.rawatkasih.ui.components.Isian
import com.example.rawatkasih.ui.components.KartuGaris
import com.example.rawatkasih.ui.components.KartuMenu
import com.example.rawatkasih.ui.components.KepalaLayar
import com.example.rawatkasih.ui.components.PesanGalat
import com.example.rawatkasih.ui.components.TemaRawat
import com.example.rawatkasih.ui.components.TombolGaris
import com.example.rawatkasih.ui.components.TombolUtama
import com.example.rawatkasih.ui.components.WarnaGalat
import com.example.rawatkasih.ui.components.WarnaSekunder
import com.example.rawatkasih.ui.galeri.GaleriViewModel
import com.example.rawatkasih.ui.keluarga.KontakViewModel
import com.example.rawatkasih.ui.kunjungan.KunjunganViewModel
import com.example.rawatkasih.ui.obat.ObatViewModel
import com.example.rawatkasih.ui.theme.Arang
import com.example.rawatkasih.ui.theme.Gading
import com.example.rawatkasih.ui.theme.Sage
import com.example.rawatkasih.ui.theme.SageMuda
import com.example.rawatkasih.ui.vitalsign.VitalSignViewModel
import java.util.Locale

private enum class TabKeluarga(val label: String, @get:DrawableRes val ikon: Int) {
    BERANDA("Beranda", R.drawable.nav_pramurukti_beranda),
    KESEHATAN("Kesehatan", R.drawable.nav_keluarga_kesehatan),
    OBAT("Obat", R.drawable.nav_keluarga_obat),
    AKUN("Akun", R.drawable.nav_pramurukti_akun)
}

/** Layar penuh yang dibuka dari tab; VITAL dan GALERI memakai layar Fungsi 3 dan Fungsi 4 apa adanya. */
private enum class LayarKeluarga { VITAL, GALERI, KUNJUNGAN, FORM_KUNJUNGAN }

/**
 * Akun keluarga: satu kerangka dengan bottom navigation (Beranda · Kesehatan · Obat · Akun).
 * Data tetap dari ViewModel tiap fungsi; layar ini hanya menyusun dan menautkannya.
 */
@Composable
internal fun KeluargaRoute(
    lansiaId: String?,
    uid: String,
    obat: ObatViewModel,
    vital: VitalSignViewModel,
    galeri: GaleriViewModel,
    kunjungan: KunjunganViewModel,
    kontak: KontakViewModel
) {
    val obatState by obat.state.collectAsStateWithLifecycle()
    val vitalState by vital.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(TabKeluarga.BERANDA) }
    var layar by rememberSaveable { mutableStateOf<LayarKeluarga?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(lansiaId) {
        if (lansiaId != null) {
            vital.pilihLansia(lansiaId)
            kunjungan.pilihLansia(lansiaId)
            kontak.pilihLansia(lansiaId)
        }
    }
    LaunchedEffect(Unit) { kunjungan.pesan.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(Unit) { kontak.pesan.collect { snackbar.showSnackbar(it) } }

    val nama = obatState.rencana?.namaLansia ?: vitalState.namaLansiaAktif().takeIf { vitalState.lansia.isNotEmpty() } ?: "Lansia"
    val bukaVital = { lansiaId?.let(vital::pilihLansia); layar = LayarKeluarga.VITAL }

    // Layar Fungsi 3 dan 4 punya Scaffold dan tombol kembali sendiri.
    when (layar) {
        LayarKeluarga.VITAL -> { VitalSignRoute(vital, { layar = null }, {}, {}, {}); return }
        LayarKeluarga.GALERI -> { GaleriRoute(galeri, lansiaId) { layar = null }; return }
        else -> Unit
    }

    BackHandler(enabled = layar != null || tab != TabKeluarga.BERANDA) {
        when (layar) {
            LayarKeluarga.FORM_KUNJUNGAN -> layar = LayarKeluarga.KUNJUNGAN
            null -> tab = TabKeluarga.BERANDA
            else -> layar = null
        }
    }

    TemaRawat {
        Scaffold(
            containerColor = Gading,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = { if (layar == null) BarKeluarga(tab) { tab = it } }
        ) { padding ->
            when (layar) {
                LayarKeluarga.KUNJUNGAN -> Detail(padding, "Kunjungan · $nama", "Jadwal besuk", { layar = null }) {
                    KunjunganKeluargaKonten(kunjungan, uid, PaddingValues()) { layar = LayarKeluarga.FORM_KUNJUNGAN }
                }
                LayarKeluarga.FORM_KUNJUNGAN -> Detail(padding, "Kunjungan · $nama", "Ajukan kunjungan", { layar = LayarKeluarga.KUNJUNGAN }) {
                    FormKunjunganKonten(kunjungan, PaddingValues()) { layar = LayarKeluarga.KUNJUNGAN }
                }
                else -> when (tab) {
                    TabKeluarga.BERANDA -> Beranda(nama, padding, obat, vital, galeri, kunjungan, kontak, bukaVital,
                        onObat = { tab = TabKeluarga.OBAT }, onAkun = { tab = TabKeluarga.AKUN },
                        onGaleri = { layar = LayarKeluarga.GALERI }, onKunjungan = { layar = LayarKeluarga.KUNJUNGAN })
                    TabKeluarga.KESEHATAN -> Kesehatan(nama, padding, vital, galeri, bukaVital) { layar = LayarKeluarga.GALERI }
                    TabKeluarga.OBAT -> ObatKeluarga(nama, padding, obat)
                    TabKeluarga.AKUN -> AkunKeluarga(padding, obat, kontak)
                }
            }
        }
    }
}

@Composable
private fun BarKeluarga(aktif: TabKeluarga, onPilih: (TabKeluarga) -> Unit) {
    // Gaya sama dengan PramuruktiBottomBar supaya semua peran terasa satu aplikasi.
    val warna = NavigationBarItemDefaults.colors(selectedTextColor = Arang, unselectedTextColor = Arang, indicatorColor = SageMuda)
    NavigationBar(
        modifier = Modifier.navigationBarsPadding().height(72.dp),
        containerColor = Gading, tonalElevation = 0.dp, windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        TabKeluarga.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == aktif,
                onClick = { onPilih(tab) },
                icon = {
                    Image(
                        painterResource(tab.ikon), contentDescription = null, modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(if (tab == aktif) Sage else Arang)
                    )
                },
                label = { Text(tab.label, fontSize = 12.sp, lineHeight = 17.sp) },
                colors = warna
            )
        }
    }
}

/** Halaman di bawah tab: tombol kembali + judul, isi mengisi sisa layar. */
@Composable
private fun Detail(padding: PaddingValues, keterangan: String, judul: String, onKembali: () -> Unit, isi: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        TextButton(onClick = onKembali, modifier = Modifier.padding(start = 12.dp, top = 16.dp)) { Text("‹ Kembali", color = Sage) }
        Box(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)) { KepalaLayar(keterangan, judul) }
        Box(Modifier.weight(1f)) { isi() }
    }
}

@Composable
internal fun DaftarLayar(padding: PaddingValues, isi: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = isi
    )
}

@Composable
private fun Beranda(
    nama: String, padding: PaddingValues,
    obat: ObatViewModel, vital: VitalSignViewModel, galeri: GaleriViewModel, kunjungan: KunjunganViewModel, kontak: KontakViewModel,
    onVital: () -> Unit, onObat: () -> Unit, onAkun: () -> Unit, onGaleri: () -> Unit, onKunjungan: () -> Unit
) {
    val obatState by obat.state.collectAsStateWithLifecycle()
    val vitalState by vital.state.collectAsStateWithLifecycle()
    val galeriState by galeri.state.collectAsStateWithLifecycle()
    val kunjunganState by kunjungan.state.collectAsStateWithLifecycle()
    val nomorPanti by kontak.nomorPanti.collectAsStateWithLifecycle()
    val nomorSaya by kontak.nomorKeluarga.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DaftarLayar(padding) {
        item { KepalaLayar("Keluarga", nama, "Kabar terbaru dari panti hari ini.") }
        item { KartuPortalVitalKeluarga(vitalState.ringkasanAktif(), onVital) }
        item {
            val rencana = obatState.rencana
            val berikutnya = rencana?.jadwalBerikutnya
            KartuMenu(
                "Obat",
                when {
                    obatState.gagalMemuat -> "Jadwal obat gagal dimuat"
                    rencana == null -> "Memuat jadwal"
                    berikutnya != null -> "Berikutnya ${berikutnya.jam} · ${berikutnya.waktu.lowercase(Locale.ROOT)}"
                    rencana.jadwal.isEmpty() -> "Belum ada jadwal obat"
                    else -> "Jadwal hari ini selesai"
                },
                onObat
            )
        }
        item {
            val perluDokter = galeriState.foto.count { it.status == StatusDokumentasi.PERLU_DOKTER }
            KartuMenu(
                "Galeri fisik",
                when {
                    !galeriState.dimuat -> "Memuat dokumentasi"
                    perluDokter > 0 -> "$perluDokter kondisi perlu diperiksa dokter"
                    galeriState.foto.isEmpty() -> "Belum ada dokumentasi kondisi fisik"
                    else -> "${galeriState.foto.size} dokumentasi kondisi fisik"
                },
                onGaleri
            )
        }
        item {
            KartuMenu(
                "Kunjungan",
                when {
                    kunjunganState.gagalMemuat -> "Kunjungan gagal dimuat"
                    !kunjunganState.dimuat -> "Memuat jadwal kunjungan"
                    else -> ringkasKunjungan(kunjunganState.daftar, kunjungan.hariIni())
                },
                onKunjungan
            )
        }
        item {
            KartuGaris {
                Text("Hubungi panti", style = GayaBagian)
                val no = nomorPanti
                if (no != null) {
                    Text("WhatsApp ${tampilNomor(no)}", style = GayaIsi, color = WarnaSekunder)
                    TombolUtama("Chat WhatsApp panti", {
                        bukaWhatsApp(context, no, "Halo, saya keluarga dari $nama. ")
                    })
                } else {
                    Text("Nomor WhatsApp panti belum diisi pengelola.", style = GayaIsi, color = WarnaSekunder)
                }
            }
        }
        if (nomorSaya == null) item {
            KartuMenu("Nomor WhatsApp Anda", "Tambahkan supaya panti bisa menghubungi Anda", onAkun)
        }
    }
}

@Composable
private fun Kesehatan(nama: String, padding: PaddingValues, vital: VitalSignViewModel, galeri: GaleriViewModel, onVital: () -> Unit, onGaleri: () -> Unit) {
    val vitalState by vital.state.collectAsStateWithLifecycle()
    val galeriState by galeri.state.collectAsStateWithLifecycle()
    DaftarLayar(padding) {
        item { KepalaLayar("Kesehatan", nama, "Tanda vital dan kondisi fisik yang dicatat pramurukti.") }
        item { KartuPortalVitalKeluarga(vitalState.ringkasanAktif(), onVital) }
        item {
            val terakhir = vitalState.ringkasanAktif()?.terbaru
            KartuMenu(
                "Grafik tren 7 hari",
                if (terakhir == null) "Belum ada pemeriksaan" else "Tekanan darah & gula darah · ${vitalState.riwayatAktif().size} pemeriksaan",
                onVital
            )
        }
        item {
            KartuMenu(
                "Galeri fisik & SOP P3K",
                if (galeriState.foto.isEmpty()) "Belum ada dokumentasi" else "${galeriState.foto.size} dokumentasi",
                onGaleri
            )
        }
    }
}

@Composable
private fun ObatKeluarga(nama: String, padding: PaddingValues, obat: ObatViewModel) {
    val state by obat.state.collectAsStateWithLifecycle()
    val rencana = state.rencana
    DaftarLayar(padding) {
        item { KepalaLayar("Obat", nama, "Jadwal dan pemberian obat dicatat oleh pramurukti.") }
        when {
            state.gagalMemuat -> item {
                KartuGaris {
                    Text(state.pesanMuat ?: "Jadwal obat gagal dimuat.", style = GayaIsi, color = WarnaGalat)
                    TombolGaris("Coba lagi", obat::muat)
                }
            }
            rencana == null -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            else -> {
                item {
                    val j = rencana.jadwalBerikutnya
                    KartuGaris {
                        Text("Jadwal berikutnya", style = GayaKeterangan, color = WarnaSekunder)
                        when {
                            j != null -> {
                                Text("${j.jam} · ${j.waktu.lowercase(Locale.ROOT)}", style = GayaBagian)
                                Text("Belum dikonfirmasi", style = GayaIsi, color = WarnaSekunder)
                            }
                            rencana.jadwal.isEmpty() -> Text("Belum ada jadwal obat", style = GayaBagian)
                            else -> Text("Jadwal hari ini selesai", style = GayaBagian)
                        }
                    }
                }
                item {
                    KartuGaris {
                        Text("Persediaan", style = GayaKeterangan, color = WarnaSekunder)
                        Text(
                            when (rencana.statusStok) {
                                StatusStok.HABIS -> "Obat habis · pengisian diurus panti"
                                StatusStok.MENIPIS -> "Sisa ${rencana.stok} dosis · segera diisi panti"
                                StatusStok.AMAN -> "Sisa ${rencana.stok} dosis"
                            },
                            style = GayaBagian
                        )
                    }
                }
                if (rencana.riwayat.isNotEmpty()) {
                    item { Text("Riwayat pemberian", Modifier.padding(top = 8.dp), style = GayaBagian) }
                    items(rencana.riwayat, key = { it.jam }) { c ->
                        KartuGaris {
                            Text("${c.jam} · ${if (c.status == StatusJadwal.DIMINUM) "Diminum" else "Dilewati"}", style = GayaBagian)
                            Text("Jadwal ${c.waktu.lowercase(Locale.ROOT)}", style = GayaIsi, color = WarnaSekunder)
                            if (c.status == StatusJadwal.DILEWATI && c.alasanPenolakan.isNotEmpty()) {
                                Text("Alasan: ${c.alasanPenolakan}", style = GayaIsi)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Tab Akun yang sama untuk keluarga dan pramurukti: email, [tambahan], ganti kata sandi, keluar. */
@Composable
internal fun AkunKonten(padding: PaddingValues, obat: ObatViewModel, peran: String, tambahan: LazyListScope.() -> Unit = {}) {
    val state by obat.state.collectAsStateWithLifecycle()
    var dialogSandi by rememberSaveable { mutableStateOf(false) }

    DaftarLayar(padding) {
        item { KepalaLayar("Akun", peran, obat.emailAktif) }
        tambahan()
        item { TombolGaris("Ganti kata sandi", { dialogSandi = true }) }
        item { TombolGaris("Keluar", obat::keluar) }
    }

    if (dialogSandi) {
        GantiKataSandiDialog(
            sedangMenyimpan = state.sedangGantiSandi,
            berhasil = state.sandiDiubah,
            pesanError = state.pesanGantiSandi,
            onTutup = { dialogSandi = false; obat.resetGantiSandi() },
            onSimpan = obat::gantiKataSandi
        )
    }
}

@Composable
private fun AkunKeluarga(padding: PaddingValues, obat: ObatViewModel, kontak: KontakViewModel) {
    val nomor by kontak.nomorKeluarga.collectAsStateWithLifecycle()
    val menyimpan by kontak.menyimpan.collectAsStateWithLifecycle()
    var ubahNomor by rememberSaveable { mutableStateOf(false) }
    var isian by rememberSaveable { mutableStateOf("") }
    var galat by rememberSaveable { mutableStateOf<String?>(null) }

    AkunKonten(padding, obat, "Keluarga") {
        item {
            KartuGaris {
                Text("Nomor WhatsApp saya", style = GayaBagian)
                val no = nomor
                if (no != null && !ubahNomor) {
                    Text(tampilNomor(no), style = GayaIsi)
                    Text("Panti memakai nomor ini untuk menghubungi Anda.", style = GayaKeterangan, color = WarnaSekunder)
                    TombolGaris("Ganti nomor", { ubahNomor = true; isian = "" })
                } else {
                    Text("Opsional. Panti memakai nomor ini untuk menghubungi Anda.", style = GayaIsi, color = WarnaSekunder)
                    Isian("Nomor WhatsApp", isian, { isian = it; galat = null }, jenis = KeyboardType.Phone, galat = galat != null)
                    PesanGalat(galat)
                    TombolUtama(if (menyimpan) "Menyimpan…" else "Simpan nomor", {
                        galat = kontak.simpanNomorKeluarga(isian)
                        if (galat == null) ubahNomor = false
                    }, enabled = !menyimpan)
                }
            }
        }
    }
}
