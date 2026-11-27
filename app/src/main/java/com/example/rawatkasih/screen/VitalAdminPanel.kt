package com.example.rawatkasih.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rawatkasih.feature.vitalsign.model.StatusRisiko
import com.example.rawatkasih.ui.admin.AdminUiState
import com.example.rawatkasih.ui.components.BentukKartu
import com.example.rawatkasih.ui.components.GayaBagian
import com.example.rawatkasih.ui.components.GayaIsi
import com.example.rawatkasih.ui.components.GayaKeterangan
import com.example.rawatkasih.ui.components.KartuGaris
import com.example.rawatkasih.ui.components.WarnaSekunder
import com.example.rawatkasih.ui.theme.MerahHangat
import com.example.rawatkasih.ui.theme.Sage
import com.example.rawatkasih.ui.theme.Terakota
import com.example.rawatkasih.ui.vitalsign.VitalAdminViewModel

/**
 * Tab "Vital" di halaman Pengelola Panti (Fungsi 3): meninjau rekapitulasi status risiko
 * serta tren 7 hari tekanan darah & gula darah seluruh lansia panti secara kolektif.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VitalAdminPanel(
    viewModel: VitalAdminViewModel,
    admin: AdminUiState,
    aktif: Boolean
) {
    LaunchedEffect(admin.lansia) {
        viewModel.sinkronLansia(admin.lansia.map { it.id })
    }
    val rekap by viewModel.rekap.collectAsStateWithLifecycle()
    val lansiaDipilih by viewModel.lansiaAktif.collectAsStateWithLifecycle()

    Text("Rekap vital sign panti", Modifier.semantics { heading() }, style = GayaBagian)
    if (admin.lansia.isEmpty()) {
        Text("Tambahkan profil lansia di tab Lansia dulu.", style = GayaIsi, color = WarnaSekunder)
        return
    }

    val semuaRingkasan = admin.lansia.mapNotNull { rekap[it.id]?.ringkasan?.takeIf { r -> r.terbaru != null } }
    val jumlahBahaya = semuaRingkasan.count { it.statusEfektif == StatusRisiko.BAHAYA }
    val jumlahWaspada = semuaRingkasan.count { it.statusEfektif == StatusRisiko.WASPADA }
    val jumlahNormal = semuaRingkasan.count { it.statusEfektif == StatusRisiko.NORMAL }

    KartuGaris {
        Text("Ringkasan kolektif (${semuaRingkasan.size}/${admin.lansia.size} lansia terdata)", style = GayaBagian)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Bahaya: $jumlahBahaya", style = GayaIsi, color = MerahHangat, fontWeight = FontWeight.Bold)
            Text("Waspada: $jumlahWaspada", style = GayaIsi, color = Terakota, fontWeight = FontWeight.Bold)
            Text("Normal: $jumlahNormal", style = GayaIsi, color = Sage, fontWeight = FontWeight.Bold)
        }
    }

    // Urutkan lansia dengan risiko tertinggi di atas agar pengelola panti langsung melihat prioritas.
    val terurut = admin.lansia.sortedBy { lansia ->
        val r = rekap[lansia.id]?.ringkasan
        when {
            r?.terbaru == null -> 3
            r.statusEfektif == StatusRisiko.BAHAYA -> 0
            r.statusEfektif == StatusRisiko.WASPADA -> 1
            else -> 2
        }
    }

    terurut.forEach { lansia ->
        val data = rekap[lansia.id]
        val ringkasan = data?.ringkasan
        val terbaru = ringkasan?.terbaru
        KartuGaris(onClick = { if (aktif) viewModel.pilihLansia(lansia.id) }) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(lansia.nama, style = GayaBagian, modifier = Modifier.weight(1f))
                if (terbaru != null) BadgeRisiko(ringkasan.statusEfektif)
            }
            if (terbaru == null) {
                Text("Belum ada pemeriksaan vital sign.", style = GayaIsi, color = WarnaSekunder)
            } else {
                Text(
                    "TD ${terbaru.sistol}/${terbaru.diastol} mmHg · GD ${terbaru.gulaDarah} mg/dL",
                    style = GayaIsi
                )
                if (ringkasan.daftarPeringatan.isNotEmpty()) {
                    Text(ringkasan.daftarPeringatan.first(), style = GayaKeterangan, color = warnaRisiko(ringkasan.statusEfektif))
                }
            }
        }
    }

    Text("Detail grafik per lansia", Modifier.padding(top = 12.dp).semantics { heading() }, style = GayaBagian)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        admin.lansia.forEach { l ->
            FilterChip(
                selected = l.id == lansiaDipilih,
                onClick = { viewModel.pilihLansia(l.id) },
                label = { Text(l.nama) },
                enabled = aktif,
                shape = BentukKartu
            )
        }
    }

    val lansiaTarget = admin.lansia.firstOrNull { it.id == lansiaDipilih } ?: return
    val detail = rekap[lansiaTarget.id] ?: return
    KartuStatusRisikoVital(lansiaTarget.nama, detail.ringkasan, untukKeluarga = false)
    if (detail.ringkasan.terbaru != null) {
        KartuRingkasanTren7Hari(detail.ringkasan)
        KartuGrafikTekananDarah(detail.ringkasan)
        KartuGrafikGulaDarah(detail.ringkasan)
    }
    Text("Riwayat ${lansiaTarget.nama}", Modifier.padding(top = 8.dp).semantics { heading() }, style = GayaBagian)
    if (detail.riwayat.isEmpty()) {
        Text("Belum ada riwayat pemeriksaan.", style = GayaIsi, color = WarnaSekunder)
    } else {
        detail.riwayat.take(10).forEach { KartuRiwayatVital(it) }
    }
}
