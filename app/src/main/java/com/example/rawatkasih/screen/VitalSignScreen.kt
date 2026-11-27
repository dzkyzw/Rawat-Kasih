package com.example.rawatkasih.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rawatkasih.feature.vitalsign.model.AnalisisVitalSign
import com.example.rawatkasih.feature.vitalsign.model.CatatanVitalSign
import com.example.rawatkasih.feature.vitalsign.model.RingkasanTrenVital
import com.example.rawatkasih.feature.vitalsign.model.StatusRisiko
import com.example.rawatkasih.ui.components.BentukKartu
import com.example.rawatkasih.ui.components.GayaBagian
import com.example.rawatkasih.ui.components.GayaIsi
import com.example.rawatkasih.ui.components.GayaKeterangan
import com.example.rawatkasih.ui.components.Isian
import com.example.rawatkasih.ui.components.KartuGaris
import com.example.rawatkasih.ui.components.KepalaLayar
import com.example.rawatkasih.ui.components.PesanGalat
import com.example.rawatkasih.ui.components.PramuruktiBottomBar
import com.example.rawatkasih.ui.components.TabPramurukti
import com.example.rawatkasih.ui.components.TemaRawat
import com.example.rawatkasih.ui.components.TombolGaris
import com.example.rawatkasih.ui.components.TombolUtama
import com.example.rawatkasih.ui.components.WarnaSekunder
import com.example.rawatkasih.ui.components.WarnaTeks
import com.example.rawatkasih.ui.theme.ArangPudar
import com.example.rawatkasih.ui.theme.GarisHalus
import com.example.rawatkasih.ui.theme.Krem
import com.example.rawatkasih.ui.theme.MerahHangat
import com.example.rawatkasih.ui.theme.Sage
import com.example.rawatkasih.ui.theme.SageMuda
import com.example.rawatkasih.ui.theme.Terakota
import com.example.rawatkasih.ui.theme.TerakotaTua
import com.example.rawatkasih.ui.vitalsign.StatusSimpanVital
import com.example.rawatkasih.ui.vitalsign.VitalSignUiState
import com.example.rawatkasih.ui.vitalsign.VitalSignViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private val LatarWaspada = Color(0xFFFDF2EC)
private val LatarBahaya = Color(0xFFFCECEC)

internal fun warnaRisiko(status: StatusRisiko): Color = when (status) {
    StatusRisiko.NORMAL -> Sage
    StatusRisiko.WASPADA -> Terakota
    StatusRisiko.BAHAYA -> MerahHangat
}

internal fun latarRisiko(status: StatusRisiko): Color = when (status) {
    StatusRisiko.NORMAL -> SageMuda
    StatusRisiko.WASPADA -> LatarWaspada
    StatusRisiko.BAHAYA -> LatarBahaya
}

@Composable
internal fun VitalSignRoute(
    viewModel: VitalSignViewModel,
    onKembali: () -> Unit,
    onProfil: () -> Unit,
    onTugas: () -> Unit,
    onCatatan: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onKembali)
    TemaRawat {
        Scaffold(bottomBar = {
            if (state.bisaInput) {
                PramuruktiBottomBar(
                    tabAktif = TabPramurukti.LANSIA,
                    tabTersedia = setOf(TabPramurukti.BERANDA, TabPramurukti.LANSIA, TabPramurukti.CATATAN, TabPramurukti.AKUN),
                    onPilihTab = { tab ->
                        when (tab) {
                            TabPramurukti.BERANDA -> onKembali()
                            TabPramurukti.LANSIA -> onTugas()
                            TabPramurukti.CATATAN -> onCatatan()
                            TabPramurukti.AKUN -> onProfil()
                        }
                    }
                )
            }
        }) { padding ->
            when {
                state.gagalMemuat -> Box(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(state.pesanMuat ?: "Data vital sign gagal dimuat.", style = GayaIsi)
                        Button(onClick = viewModel::muat) { Text("Coba lagi") }
                        TextButton(onClick = onKembali) { Text("Kembali") }
                    }
                }
                !state.dimuat -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
                else -> VitalSignScreen(
                    state = state,
                    padding = padding,
                    onPilihLansia = viewModel::pilihLansia,
                    onSimpan = viewModel::simpan,
                    onUbahForm = viewModel::hapusKesalahanSimpan,
                    onKembali = onKembali
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VitalSignScreen(
    state: VitalSignUiState,
    padding: PaddingValues,
    onPilihLansia: (String) -> Unit,
    onSimpan: (String?, String, String, String) -> String?,
    onUbahForm: () -> Unit,
    onKembali: () -> Unit
) {
    val lansiaId = state.lansiaAktifId()
    val namaLansia = state.namaLansiaAktif()
    val ringkasan = state.ringkasanAktif()
    val riwayat = state.riwayatAktif()

    var sistol by rememberSaveable { mutableStateOf("") }
    var diastol by rememberSaveable { mutableStateOf("") }
    var gulaDarah by rememberSaveable { mutableStateOf("") }
    var galatForm by rememberSaveable { mutableStateOf<String?>(null) }
    var tersimpanTerakhir by rememberSaveable { mutableStateOf(state.jumlahTersimpan) }

    LaunchedEffect(state.jumlahTersimpan) {
        if (state.jumlahTersimpan > tersimpanTerakhir) {
            sistol = ""
            diastol = ""
            gulaDarah = ""
            galatForm = null
            tersimpanTerakhir = state.jumlahTersimpan
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            KepalaLayar(
                keterangan = if (state.bisaInput) "Vital sign · ${state.email}" else "Portal keluarga · ${namaLansia.uppercase(Locale.ROOT)}",
                judul = "Monitoring vital sign",
                sub = if (state.bisaInput) {
                    "Catat tekanan darah & gula darah untuk analisis tren 7 hari dan deteksi risiko otomatis."
                } else {
                    "Grafik tren tekanan darah & gula darah 7 hari terakhir secara transparan."
                }
            )
            if (!state.bisaInput) {
                TextButton(onClick = onKembali, modifier = Modifier.padding(top = 4.dp)) {
                    Text("‹ Kembali ke jadwal obat")
                }
            }
        }

        if (state.lansia.isEmpty()) {
            item {
                KartuGaris {
                    Text("Belum ada lansia terhubung", style = GayaBagian)
                    Text(
                        "Hubungi pengelola panti agar akun Anda terhubung dengan profil lansia.",
                        style = GayaIsi, color = WarnaSekunder
                    )
                }
            }
            return@LazyColumn
        }

        if (state.lansia.size > 1) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.lansia.forEach { l ->
                        FilterChip(
                            selected = l.id == lansiaId,
                            onClick = { onPilihLansia(l.id); galatForm = null },
                            label = { Text(l.nama) },
                            shape = BentukKartu
                        )
                    }
                }
            }
        }

        if (ringkasan != null) {
            item {
                KartuStatusRisikoVital(
                    namaLansia = namaLansia,
                    ringkasan = ringkasan,
                    untukKeluarga = !state.bisaInput
                )
            }
        }

        if (state.bisaInput) {
            item {
                KartuGaris {
                    Text("Catat vital sign · $namaLansia", style = GayaBagian)
                    Text(
                        "Batas aman: TD 90/60–140/90 mmHg · Gula darah 70–200 mg/dL.",
                        style = GayaKeterangan, color = WarnaSekunder
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Isian(
                            label = "Sistol (mmHg)",
                            nilai = sistol,
                            ubah = { sistol = it.filter(Char::isDigit); galatForm = null; onUbahForm() },
                            modifier = Modifier.weight(1f),
                            jenis = KeyboardType.Number,
                            galat = galatForm != null
                        )
                        Isian(
                            label = "Diastol (mmHg)",
                            nilai = diastol,
                            ubah = { diastol = it.filter(Char::isDigit); galatForm = null; onUbahForm() },
                            modifier = Modifier.weight(1f),
                            jenis = KeyboardType.Number,
                            galat = galatForm != null
                        )
                    }
                    Isian(
                        label = "Gula darah (mg/dL)",
                        nilai = gulaDarah,
                        ubah = { gulaDarah = it.filter(Char::isDigit); galatForm = null; onUbahForm() },
                        jenis = KeyboardType.Number,
                        galat = galatForm != null
                    )
                    PesanGalat(galatForm)
                    if (state.statusSimpan == StatusSimpanVital.GAGAL) {
                        PesanGalat(state.pesanSimpan ?: "Data vital sign gagal disimpan.")
                    }
                    TombolUtama(
                        label = if (state.statusSimpan == StatusSimpanVital.MENYIMPAN) "Menyimpan…" else "Simpan pemeriksaan",
                        onClick = { galatForm = onSimpan(lansiaId, sistol, diastol, gulaDarah) },
                        enabled = state.statusSimpan != StatusSimpanVital.MENYIMPAN
                    )
                }
            }
        }

        if (ringkasan != null && ringkasan.terbaru != null) {
            item { KartuRingkasanTren7Hari(ringkasan) }
            item { KartuGrafikTekananDarah(ringkasan) }
            item { KartuGrafikGulaDarah(ringkasan) }
        }

        item {
            Text(
                "Riwayat pemeriksaan",
                Modifier.padding(top = 12.dp).semantics { heading() },
                style = GayaBagian
            )
        }
        if (riwayat.isEmpty()) {
            item {
                Text("Belum ada riwayat pengukuran vital sign.", style = GayaIsi, color = WarnaSekunder)
            }
        } else {
            items(riwayat, key = { it.id }) { item ->
                KartuRiwayatVital(item)
            }
        }
    }
}

/**
 * Banner alert otomatis yang muncul di Portal Keluarga (ObatScreen) saat kondisi vital abnormal,
 * sekaligus menjadi tombol cepat menuju halaman grafik tren 7 hari.
 */
@Composable
internal fun KartuPortalVitalKeluarga(
    ringkasan: RingkasanTrenVital?,
    onBukaVital: () -> Unit
) {
    val terbaru = ringkasan?.terbaru
    val status = ringkasan?.statusEfektif ?: StatusRisiko.NORMAL
    val warna = warnaRisiko(status)
    val latar = if (terbaru != null && status != StatusRisiko.NORMAL) latarRisiko(status) else Krem
    val border = if (terbaru != null && status != StatusRisiko.NORMAL) BorderStroke(1.5.dp, warna) else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = BentukKartu,
        colors = CardDefaults.cardColors(containerColor = latar, contentColor = WarnaTeks),
        border = border
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (ringkasan?.perluAlertKeluarga == true) "Alert Vital Sign · ${status.label}" else "Vital Sign & Tren 7 Hari",
                    style = GayaBagian,
                    color = if (ringkasan?.perluAlertKeluarga == true) warna else WarnaTeks
                )
                if (terbaru != null) BadgeRisiko(status)
            }
            if (terbaru == null) {
                Text("Belum ada pemeriksaan tekanan darah atau gula darah tercatat.", style = GayaIsi, color = WarnaSekunder)
            } else {
                Text(
                    "Terakhir (${formatWaktuSingkat(terbaru.waktu)}): TD ${terbaru.sistol}/${terbaru.diastol} mmHg · GD ${terbaru.gulaDarah} mg/dL",
                    style = GayaIsi
                )
                if (ringkasan.daftarPeringatan.isNotEmpty()) {
                    ringkasan.daftarPeringatan.forEach { pesan ->
                        Text("• $pesan", style = GayaIsi, color = warna, fontWeight = FontWeight.Medium)
                    }
                }
            }
            TombolGaris("Lihat grafik tren vital sign", onBukaVital)
        }
    }
}

@Composable
internal fun KartuStatusRisikoVital(
    namaLansia: String,
    ringkasan: RingkasanTrenVital,
    untukKeluarga: Boolean
) {
    val terbaru = ringkasan.terbaru
    if (terbaru == null) {
        KartuGaris {
            Text("Belum ada pengukuran untuk $namaLansia", style = GayaBagian)
            Text(
                "Hasil analisis risiko dan grafik tren 7 hari akan muncul setelah pengukuran pertama disimpan.",
                style = GayaIsi, color = WarnaSekunder
            )
        }
        return
    }
    val status = ringkasan.statusEfektif
    val warna = warnaRisiko(status)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = BentukKartu,
        colors = CardDefaults.cardColors(containerColor = latarRisiko(status), contentColor = WarnaTeks),
        border = BorderStroke(1.5.dp, warna)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Status risiko: ${status.label}", style = GayaBagian, color = warna)
                BadgeRisiko(status)
            }
            Text(
                "Pengukuran terbaru · ${formatWaktuSingkat(terbaru.waktu)}",
                style = GayaKeterangan, color = WarnaSekunder
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KolomMetrik("Tekanan Darah", "${terbaru.sistol}/${terbaru.diastol} mmHg", Modifier.weight(1f))
                KolomMetrik("Gula Darah", "${terbaru.gulaDarah} mg/dL", Modifier.weight(1f))
            }
            if (ringkasan.daftarPeringatan.isEmpty()) {
                Text(
                    "Seluruh parameter vital berada dalam batas aman (TD 90/60–140/90 mmHg, GD 70–200 mg/dL).",
                    style = GayaIsi, color = Sage
                )
            } else {
                ringkasan.daftarPeringatan.forEach { pesan ->
                    Text("• $pesan", style = GayaIsi, color = warna, fontWeight = FontWeight.Medium)
                }
                Text(
                    if (untukKeluarga) {
                        "Alert otomatis dari sistem monitoring panti. Hubungi pengelola panti bila perlu konfirmasi lanjut."
                    } else {
                        "Alert risiko ini otomatis tampil di Portal Keluarga secara transparan."
                    },
                    style = GayaKeterangan, color = WarnaSekunder
                )
            }
        }
    }
}

@Composable
internal fun KartuRingkasanTren7Hari(ringkasan: RingkasanTrenVital) {
    KartuGaris {
        Text("Analisis tren 7 hari", style = GayaBagian)
        if (!ringkasan.adaHistori7Hari) {
            Text(
                "Histori belum mencapai 7 hari (${ringkasan.titik7Hari.size} data tercatat). Sistem menampilkan pengukuran terkini tanpa perhitungan delta rata-rata.",
                style = GayaIsi, color = WarnaSekunder
            )
        } else {
            Text(
                "Dihitung dari ${ringkasan.titik7Hari.size} pengukuran dalam 7 hari terakhir.",
                style = GayaKeterangan, color = WarnaSekunder
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KolomMetrik(
                    label = "Rata-rata TD",
                    nilai = "${ringkasan.rataSistol}/${ringkasan.rataDiastol} mmHg",
                    modifier = Modifier.weight(1f),
                    sub = "Delta ${formatDelta(ringkasan.deltaSistol)} / ${formatDelta(ringkasan.deltaDiastol)}"
                )
                KolomMetrik(
                    label = "Rata-rata GD",
                    nilai = "${ringkasan.rataGulaDarah} mg/dL",
                    modifier = Modifier.weight(1f),
                    sub = "Delta ${formatDelta(ringkasan.deltaGulaDarah)} mg/dL"
                )
            }
        }
    }
}

@Composable
internal fun KartuGrafikTekananDarah(ringkasan: RingkasanTrenVital) {
    val titik = ringkasan.titik7Hari
    val warnaGaris = warnaRisiko(ringkasan.statusEfektif)
    KartuGaris {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Grafik Tekanan Darah (7 Hari)", style = GayaBagian)
            BadgeRisiko(ringkasan.statusEfektif)
        }
        Text(
            "Garis tebal: Sistol · Garis putus: Diastol · Batas aman: 90/60–140/90 mmHg",
            style = GayaKeterangan, color = WarnaSekunder
        )
        GrafikGarisCanvas(
            titik = titik,
            nilaiUtama = { it.sistol.toFloat() },
            nilaiSekunder = { it.diastol.toFloat() },
            batasAtas = AnalisisVitalSign.SISTOL_NORMAL_MAX.toFloat(),
            batasBawah = AnalisisVitalSign.DIASTOL_NORMAL_MIN.toFloat(),
            minDasar = 50f,
            maxDasar = 180f,
            warnaUtama = warnaGaris,
            warnaSekunder = ArangPudar,
            satuan = "mmHg"
        )
    }
}

@Composable
internal fun KartuGrafikGulaDarah(ringkasan: RingkasanTrenVital) {
    val titik = ringkasan.titik7Hari
    val warnaGaris = warnaRisiko(ringkasan.statusEfektif)
    KartuGaris {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Grafik Gula Darah (7 Hari)", style = GayaBagian)
            BadgeRisiko(ringkasan.statusEfektif)
        }
        Text(
            "Batas aman: 70–200 mg/dL · Warna mengikuti status risiko kesehatan",
            style = GayaKeterangan, color = WarnaSekunder
        )
        GrafikGarisCanvas(
            titik = titik,
            nilaiUtama = { it.gulaDarah.toFloat() },
            nilaiSekunder = null,
            batasAtas = AnalisisVitalSign.GULA_NORMAL_MAX.toFloat(),
            batasBawah = AnalisisVitalSign.GULA_NORMAL_MIN.toFloat(),
            minDasar = 50f,
            maxDasar = 260f,
            warnaUtama = warnaGaris,
            warnaSekunder = ArangPudar,
            satuan = "mg/dL"
        )
    }
}

@Composable
private fun GrafikGarisCanvas(
    titik: List<CatatanVitalSign>,
    nilaiUtama: (CatatanVitalSign) -> Float,
    nilaiSekunder: ((CatatanVitalSign) -> Float)?,
    batasAtas: Float,
    batasBawah: Float,
    minDasar: Float,
    maxDasar: Float,
    warnaUtama: Color,
    warnaSekunder: Color,
    satuan: String
) {
    if (titik.isEmpty()) return
    val semuaNilai = buildList {
        titik.forEach {
            add(nilaiUtama(it))
            if (nilaiSekunder != null) add(nilaiSekunder(it))
        }
        add(batasAtas)
        add(batasBawah)
    }
    val yMin = min(minDasar, (semuaNilai.minOrNull() ?: minDasar) - 10f)
    val yMax = max(maxDasar, (semuaNilai.maxOrNull() ?: maxDasar) + 10f)
    val rentangY = (yMax - yMin).coerceAtLeast(10f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Maks ${yMax.toInt()} $satuan", style = GayaKeterangan, color = WarnaSekunder)
            Text("Batas aman ${batasBawah.toInt()}–${batasAtas.toInt()}", style = GayaKeterangan, color = WarnaSekunder)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color.White.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            val w = size.width
            val h = size.height
            fun yUntuk(v: Float): Float = h - ((v - yMin) / rentangY) * h
            fun xUntuk(index: Int): Float = if (titik.size == 1) w / 2f else (index.toFloat() / (titik.size - 1)) * w

            // Garis ambang batas medis (putus-putus halus).
            val efekPutus = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            val yAtas = yUntuk(batasAtas)
            val yBawah = yUntuk(batasBawah)
            drawLine(Terakota.copy(alpha = 0.45f), Offset(0f, yAtas), Offset(w, yAtas), strokeWidth = 2f, pathEffect = efekPutus)
            drawLine(Terakota.copy(alpha = 0.45f), Offset(0f, yBawah), Offset(w, yBawah), strokeWidth = 2f, pathEffect = efekPutus)

            // Garis grid horizontal tengah.
            drawLine(GarisHalus, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1.5f)

            // Seri sekunder (Diastol) jika ada.
            if (nilaiSekunder != null) {
                val pathSekunder = Path()
                titik.forEachIndexed { i, item ->
                    val p = Offset(xUntuk(i), yUntuk(nilaiSekunder(item)))
                    if (i == 0) pathSekunder.moveTo(p.x, p.y) else pathSekunder.lineTo(p.x, p.y)
                }
                if (titik.size > 1) {
                    drawPath(
                        path = pathSekunder,
                        color = warnaSekunder,
                        style = Stroke(width = 4f, cap = StrokeCap.Round, pathEffect = efekPutus)
                    )
                }
                titik.forEachIndexed { i, item ->
                    drawCircle(color = warnaSekunder, radius = 6f, center = Offset(xUntuk(i), yUntuk(nilaiSekunder(item))))
                }
            }

            // Seri utama (Sistol atau Gula Darah).
            val pathUtama = Path()
            titik.forEachIndexed { i, item ->
                val p = Offset(xUntuk(i), yUntuk(nilaiUtama(item)))
                if (i == 0) pathUtama.moveTo(p.x, p.y) else pathUtama.lineTo(p.x, p.y)
            }
            if (titik.size > 1) {
                drawPath(
                    path = pathUtama,
                    color = warnaUtama,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
            }
            titik.forEachIndexed { i, item ->
                val pusat = Offset(xUntuk(i), yUntuk(nilaiUtama(item)))
                drawCircle(color = Color.White, radius = 10f, center = pusat)
                drawCircle(color = warnaRisiko(item.statusRisiko), radius = 7f, center = pusat)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTanggalSingkat(titik.first().waktu), style = GayaKeterangan, color = WarnaSekunder)
            if (titik.size > 2) {
                Text(formatTanggalSingkat(titik[titik.size / 2].waktu), style = GayaKeterangan, color = WarnaSekunder)
            }
            Text(formatTanggalSingkat(titik.last().waktu), style = GayaKeterangan, color = WarnaSekunder)
        }
    }
}

@Composable
internal fun KartuRiwayatVital(catatan: CatatanVitalSign) {
    KartuGaris {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${formatWaktuSingkat(catatan.waktu)} · ${catatan.penulisNama.ifBlank { "Pramurukti" }}",
                style = GayaKeterangan, color = WarnaSekunder, modifier = Modifier.weight(1f)
            )
            BadgeRisiko(catatan.statusRisiko)
        }
        Text(
            "TD ${catatan.sistol}/${catatan.diastol} mmHg · Gula darah ${catatan.gulaDarah} mg/dL",
            style = GayaBagian
        )
    }
}

@Composable
internal fun BadgeRisiko(status: StatusRisiko) {
    val warna = warnaRisiko(status)
    Row(
        modifier = Modifier
            .background(warna.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(8.dp).background(warna, CircleShape))
        Text(status.label, style = GayaKeterangan, color = warna, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun KolomMetrik(label: String, nilai: String, modifier: Modifier = Modifier, sub: String? = null) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = GayaKeterangan, color = WarnaSekunder)
        Text(nilai, style = GayaBagian)
        if (sub != null) Text(sub, style = GayaKeterangan, color = TerakotaTua)
    }
}

private fun formatDelta(delta: Int?): String = when {
    delta == null -> "-"
    delta > 0 -> "+$delta"
    else -> "$delta"
}

private fun formatTanggalSingkat(waktu: Long): String =
    SimpleDateFormat("dd/MM", Locale.forLanguageTag("id")).format(Date(waktu))

private fun formatWaktuSingkat(waktu: Long): String =
    SimpleDateFormat("dd MMM · HH.mm", Locale.forLanguageTag("id")).format(Date(waktu))
