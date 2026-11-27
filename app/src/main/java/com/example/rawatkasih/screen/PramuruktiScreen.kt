package com.example.rawatkasih.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rawatkasih.feature.galeri.model.StatusDokumentasi
import com.example.rawatkasih.feature.obat.model.StatusStok
import com.example.rawatkasih.feature.vitalsign.model.StatusRisiko
import com.example.rawatkasih.ui.catatan.CatatanViewModel
import com.example.rawatkasih.ui.components.KartuMenu
import com.example.rawatkasih.ui.components.KepalaLayar
import com.example.rawatkasih.ui.components.PramuruktiBottomBar
import com.example.rawatkasih.ui.components.TabPramurukti
import com.example.rawatkasih.ui.components.TemaRawat
import com.example.rawatkasih.ui.galeri.GaleriViewModel
import com.example.rawatkasih.ui.obat.ObatViewModel
import com.example.rawatkasih.ui.theme.Gading
import com.example.rawatkasih.ui.vitalsign.VitalSignViewModel
import java.util.Locale

/** Layar penuh dari tab Lansia; ketiganya memakai layar Fungsi 1, 3, dan 4 apa adanya. */
private enum class LayarPramurukti { OBAT, VITAL, GALERI }

/**
 * Akun pramurukti: satu kerangka dengan bottom bar yang selalu sama (Beranda · Lansia · Catatan · Akun).
 * Obat dan vital sign memakai bar yang sama, jadi berpindah tab dari layar mana pun selalu berfungsi.
 */
@Composable
internal fun PramuruktiRoute(obat: ObatViewModel, catatan: CatatanViewModel, vital: VitalSignViewModel, galeri: GaleriViewModel) {
    var tab by rememberSaveable { mutableStateOf(TabPramurukti.BERANDA) }
    var layar by rememberSaveable { mutableStateOf<LayarPramurukti?>(null) }
    var lansiaGaleri by rememberSaveable { mutableStateOf<String?>(null) }

    val tutup = {
        // Rencana obat dilepas supaya daftar penugasan dimuat lagi saat lansia lain dibuka.
        if (layar == LayarPramurukti.OBAT) obat.gantiLansia()
        layar = null
    }
    val keTab: (TabPramurukti) -> Unit = { tutup(); tab = it }

    when (layar) {
        LayarPramurukti.OBAT -> {
            ObatRoute(obat, adminContent = {}, navPramurukti = keTab)
            BackHandler(onBack = tutup)
            return
        }
        LayarPramurukti.VITAL -> {
            VitalSignRoute(
                vital,
                onKembali = { keTab(TabPramurukti.BERANDA) },
                onProfil = { keTab(TabPramurukti.AKUN) },
                onTugas = { keTab(TabPramurukti.LANSIA) },
                onCatatan = { keTab(TabPramurukti.CATATAN) }
            )
            // Didaftarkan setelah milik layar vital supaya tombol kembali kembali ke tab sebelumnya.
            BackHandler(onBack = tutup)
            return
        }
        LayarPramurukti.GALERI -> {
            GaleriRoute(galeri, lansiaGaleri, onKembali = tutup)
            return
        }
        null -> Unit
    }

    BackHandler(enabled = tab != TabPramurukti.BERANDA) { tab = TabPramurukti.BERANDA }
    val bukaObat = { id: String -> obat.pilihLansia(id); layar = LayarPramurukti.OBAT }
    val bukaVital = { id: String? -> id?.let(vital::pilihLansia); layar = LayarPramurukti.VITAL }
    val bukaGaleri = { id: String? -> lansiaGaleri = id; layar = LayarPramurukti.GALERI }

    TemaRawat {
        Scaffold(containerColor = Gading, bottomBar = { PramuruktiBottomBar(tab, onPilihTab = { tab = it }) }) { padding ->
            when (tab) {
                TabPramurukti.BERANDA -> BerandaPramurukti(padding, catatan, vital, galeri, onTab = { tab = it }, onVital = bukaVital, onGaleri = { bukaGaleri(null) })
                TabPramurukti.LANSIA -> TugasKonten(
                    catatan, padding,
                    onObat = bukaObat,
                    onCatatan = { id -> id?.let(catatan::pilihLansia); tab = TabPramurukti.CATATAN },
                    onVital = bukaVital,
                    onGaleri = bukaGaleri
                )
                TabPramurukti.CATATAN -> CatatanKonten(catatan, padding)
                TabPramurukti.AKUN -> AkunKonten(padding, obat, "Pramurukti")
            }
        }
    }
}

@Composable
private fun BerandaPramurukti(
    padding: PaddingValues,
    catatan: CatatanViewModel,
    vital: VitalSignViewModel,
    galeri: GaleriViewModel,
    onTab: (TabPramurukti) -> Unit,
    onVital: (String?) -> Unit,
    onGaleri: () -> Unit
) {
    val cs by catatan.state.collectAsStateWithLifecycle()
    val vs by vital.state.collectAsStateWithLifecycle()
    val gs by galeri.state.collectAsStateWithLifecycle()
    if (!cs.dimuat && !cs.gagalMemuat) {
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val (aktif, nanti) = tugasPerLansia(cs)
    val pola = cs.pola

    DaftarLayar(padding) {
        item {
            KepalaLayar(
                "Beranda · ${cs.email}", "Tugas hari ini",
                "Sekarang ${pola.nama(cs.shiftSekarang).lowercase(Locale.ROOT)} (${pola.rentang(cs.shiftSekarang)})."
            )
        }
        item {
            KartuMenu(
                "Lansia dampingan",
                when {
                    cs.gagalMemuat -> "Daftar tugas gagal dimuat"
                    aktif.isEmpty() && nanti.isEmpty() -> "Belum ada penugasan dari pengelola"
                    aktif.isEmpty() -> "Tidak ada yang sedang bertugas · ${nanti.size} di luar shift Anda"
                    else -> "${aktif.size} lansia sedang menjadi tanggungan Anda"
                }
            ) { onTab(TabPramurukti.LANSIA) }
        }
        item {
            val belum = aktif.filterNot { catatanSudahDitulis(it, cs) }
            KartuMenu(
                "Catatan shift",
                when {
                    aktif.isEmpty() -> "Tulis catatan saat Anda sedang bertugas"
                    belum.isEmpty() -> "Semua catatan shift ini sudah ditulis"
                    else -> "${belum.size} catatan belum ditulis · ${belum.joinToString { it.lansia.nama }}"
                }
            ) { onTab(TabPramurukti.CATATAN) }
        }
        item {
            val berikutnya = aktif.mapNotNull { t -> cs.obat[t.lansia.id]?.jadwalBerikutnya?.let { it to t.lansia.nama } }.minByOrNull { it.first.jam }
            val stokKurang = aktif.count { t ->
                cs.obat[t.lansia.id]?.takeIf { it.jadwal.isNotEmpty() }?.statusStok.let { s -> s == StatusStok.HABIS || s == StatusStok.MENIPIS }
            }
            KartuMenu(
                "Obat",
                buildString {
                    append(berikutnya?.let { (j, nama) -> "Berikutnya ${j.jam} · $nama" } ?: "Tidak ada jadwal obat yang menunggu")
                    if (stokKurang > 0) append(" · stok $stokKurang lansia perlu diisi")
                }
            ) { onTab(TabPramurukti.LANSIA) }
        }
        item {
            val perhatian = vs.ringkasanPerLansia.filterValues { it.terbaru != null && it.statusEfektif != StatusRisiko.NORMAL }
            KartuMenu(
                "Vital sign",
                when {
                    !vs.dimuat -> "Memuat pemeriksaan"
                    perhatian.isNotEmpty() -> "${perhatian.size} lansia perlu perhatian"
                    vs.ringkasanPerLansia.values.none { it.terbaru != null } -> "Belum ada pemeriksaan tercatat"
                    else -> "Semua dalam batas aman"
                }
            ) { onVital(perhatian.keys.firstOrNull()) }
        }
        item {
            val perluDokter = gs.foto.count { it.status == StatusDokumentasi.PERLU_DOKTER }
            KartuMenu(
                "Galeri fisik",
                when {
                    !gs.dimuat -> "Memuat dokumentasi"
                    perluDokter > 0 -> "$perluDokter kondisi perlu diperiksa dokter"
                    gs.foto.isEmpty() -> "Belum ada dokumentasi kondisi fisik"
                    else -> "${gs.foto.size} dokumentasi kondisi fisik"
                },
                onGaleri
            )
        }
    }
}
