package com.example.rawatkasih

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rawatkasih.data.local.RawatKasihDatabase
import com.example.rawatkasih.data.remote.FirebaseP04
import com.example.rawatkasih.feature.admin.data.AdminRepository
import com.example.rawatkasih.feature.catatan.data.CatatanRepository
import com.example.rawatkasih.feature.obat.data.ObatRepository
import com.example.rawatkasih.feature.obat.data.remote.FirebaseObatRepository
import com.example.rawatkasih.feature.obat.notification.AndroidPengingatObat
import com.example.rawatkasih.feature.penugasan.data.PenugasanRepository
import com.example.rawatkasih.feature.vitalsign.data.VitalSignRepository
import com.example.rawatkasih.feature.kontak.KontakRepository
import com.example.rawatkasih.feature.kunjungan.data.KunjunganRepository
import com.example.rawatkasih.screen.KeluargaRoute
import com.example.rawatkasih.screen.PramuruktiRoute
import com.example.rawatkasih.screen.PanelKunjunganAdmin
import com.example.rawatkasih.ui.keluarga.KontakViewModel
import com.example.rawatkasih.ui.kunjungan.KunjunganViewModel
import androidx.compose.runtime.remember
import com.example.rawatkasih.screen.AdminRoute
import com.example.rawatkasih.screen.HandoverRoute
import com.example.rawatkasih.screen.ObatRoute
import com.example.rawatkasih.screen.ShiftAdminPanel
import com.example.rawatkasih.screen.SplashScreen
import com.example.rawatkasih.screen.VitalAdminPanel
import com.example.rawatkasih.ui.admin.AdminViewModel
import com.example.rawatkasih.ui.catatan.CatatanViewModel
import com.example.rawatkasih.ui.catatan.ShiftAdminViewModel
import com.example.rawatkasih.ui.obat.ObatViewModel
import com.example.rawatkasih.ui.theme.RawatKasihTheme
import com.example.rawatkasih.ui.vitalsign.VitalAdminViewModel
import com.example.rawatkasih.ui.vitalsign.VitalSignViewModel
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.core.content.ContextCompat
import com.example.rawatkasih.feature.galeri.model.FotoFisik
import com.example.rawatkasih.feature.galeri.model.StatusDokumentasi
import com.example.rawatkasih.feature.galeri.notification.AndroidNotifikasiFisik
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rawatkasih.feature.galeri.data.AndroidPemrosesFoto
import com.example.rawatkasih.feature.galeri.data.GaleriRepository
import com.example.rawatkasih.screen.GaleriRoute
import com.example.rawatkasih.screen.PantauGaleriPanel
import com.example.rawatkasih.ui.galeri.GaleriViewModel
import com.example.rawatkasih.ui.galeri.P3Ikon
import com.example.rawatkasih.ui.galeri.P3Tepi
import com.example.rawatkasih.ui.galeri.P3Teks
import com.example.rawatkasih.ui.galeri.P3TopBarKembali
import com.example.rawatkasih.ui.galeri.P3Warna
import com.example.rawatkasih.ui.galeri.PantauGaleriViewModel

class MainActivity : ComponentActivity() {
    private val obatViewModel: ObatViewModel by viewModels {
        viewModelFactory {
            initializer {
                val firebase = FirebaseP04.ambil(applicationContext)
                val sumber = if (firebase != null) {
                    FirebaseObatRepository(firebase.auth, firebase.firestore)
                } else {
                    ObatRepository(RawatKasihDatabase.ambil(applicationContext))
                }
                ObatViewModel(sumber, AndroidPengingatObat(applicationContext))
            }
        }
    }

    private val adminViewModel: AdminViewModel by viewModels {
        viewModelFactory {
            initializer {
                val firebase = checkNotNull(FirebaseP04.ambil(applicationContext))
                AdminViewModel(AdminRepository(firebase.auth, firebase.authPendaftaran, firebase.firestore))
            }
        }
    }

    // Fungsi 2 hanya tersedia dalam mode online (Firebase), sama seperti panel admin.
    private val catatanViewModel: CatatanViewModel by viewModels {
        viewModelFactory {
            initializer {
                val firebase = checkNotNull(FirebaseP04.ambil(applicationContext))
                CatatanViewModel(CatatanRepository(firebase.firestore), PenugasanRepository(firebase.auth, firebase.firestore))
            }
        }
    }

    private val shiftAdminViewModel: ShiftAdminViewModel by viewModels {
        viewModelFactory {
            initializer {
                val firebase = checkNotNull(FirebaseP04.ambil(applicationContext))
                ShiftAdminViewModel(PenugasanRepository(firebase.auth, firebase.firestore), CatatanRepository(firebase.firestore))
            }
        }
    }

    // Fungsi 3 · Monitoring & analisis tren vital sign medis (mode online Firebase).
    private val vitalSignViewModel: VitalSignViewModel by viewModels {
        viewModelFactory {
            initializer {
                val firebase = checkNotNull(FirebaseP04.ambil(applicationContext))
                VitalSignViewModel(VitalSignRepository(firebase.firestore), PenugasanRepository(firebase.auth, firebase.firestore))
            }
        }
    }

    // Fungsi 4 · Galeri fisik & P3K (hanya mode online).
    private val galeriViewModel: GaleriViewModel by viewModels {
        viewModelFactory {
            initializer {
                val firebase = checkNotNull(FirebaseP04.ambil(applicationContext))
                GaleriViewModel(
                    GaleriRepository(firebase.firestore),
                    PenugasanRepository(firebase.auth, firebase.firestore),
                    AndroidPemrosesFoto(applicationContext)
                )
            }
        }
    }

    private val vitalAdminViewModel: VitalAdminViewModel by viewModels {
        viewModelFactory {
            initializer {
                val firebase = checkNotNull(FirebaseP04.ambil(applicationContext))
                VitalAdminViewModel(VitalSignRepository(firebase.firestore))
            }
        }
    }

    private val pantauGaleriViewModel: PantauGaleriViewModel by viewModels {
        viewModelFactory {
            initializer {
                val firebase = checkNotNull(FirebaseP04.ambil(applicationContext))
                PantauGaleriViewModel(GaleriRepository(firebase.firestore), {
                    firebase.auth.currentUser?.let { it.uid to it.email.orEmpty() }
                })
            }
        }
    }

    private val notifikasiFisik by lazy { AndroidNotifikasiFisik(applicationContext) }

    // Kerangka keluarga: kunjungan keluarga dan nomor WhatsApp panti/keluarga (hanya mode online).
    private val kunjunganViewModel: KunjunganViewModel by viewModels {
        viewModelFactory {
            initializer {
                val firebase = checkNotNull(FirebaseP04.ambil(applicationContext))
                KunjunganViewModel(KunjunganRepository(firebase.auth, firebase.firestore))
            }
        }
    }

    private val kontakViewModel: KontakViewModel by viewModels {
        viewModelFactory {
            initializer {
                val firebase = checkNotNull(FirebaseP04.ambil(applicationContext))
                KontakViewModel(KontakRepository(firebase.auth, firebase.firestore))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, 0xFFF9F7F2.toInt()),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, 0xFFF9F7F2.toInt())
        )
        setContent {
            RawatKasihTheme {
                var splash by rememberSaveable { mutableStateOf(true) }
                Crossfade(splash, animationSpec = tween(300), label = "splash") { tampilSplash ->
                    if (tampilSplash) SplashScreen { splash = false } else Aplikasi()
                }
            }
        }
    }

    @Composable
    private fun Aplikasi() {
        if (!obatViewModel.modeOnline) {
            ObatRoute(obatViewModel, adminContent = {})
            return
        }
        val obatState by obatViewModel.state.collectAsStateWithLifecycle()
        val sesiFlow = remember {
            checkNotNull(FirebaseP04.ambil(applicationContext)).let { PenugasanRepository(it.auth, it.firestore) }.amatiSesi()
        }
        val sesi by sesiFlow.collectAsStateWithLifecycle(null)
        HandoverRoute(catatanViewModel) {
            DenganGaleriFisik {
            // Keluarga dan pramurukti memakai kerangka dengan bottom navigation; admin tetap di AdminRoute.
            if (!obatState.perluMasuk && obatState.peranAkun == "PRAMURUKTI") {
                PramuruktiRoute(obatViewModel, catatanViewModel, vitalSignViewModel, galeriViewModel)
                return@DenganGaleriFisik
            }
            if (!obatState.perluMasuk && obatState.peranAkun == "KELUARGA") {
                KeluargaRoute(
                    lansiaId = sesi?.lansiaIds?.firstOrNull(),
                    uid = sesi?.uid.orEmpty(),
                    obat = obatViewModel,
                    vital = vitalSignViewModel,
                    galeri = galeriViewModel,
                    kunjungan = kunjunganViewModel,
                    kontak = kontakViewModel
                )
                return@DenganGaleriFisik
            }
            ObatRoute(
                obatViewModel,
                adminContent = { bukaProfil ->
                    AdminRoute(
                        adminViewModel,
                        onProfil = bukaProfil,
                        onKeluar = {
                            adminViewModel.reset()
                            obatViewModel.keluar()
                        },
                        panelShift = { admin, aktif -> ShiftAdminPanel(shiftAdminViewModel, admin, aktif, onBerubah = adminViewModel::muat) },
                        panelVital = { admin, aktif -> VitalAdminPanel(vitalAdminViewModel, admin, aktif) },
                        panelKunjungan = { admin, aktif -> PanelKunjunganAdmin(kunjunganViewModel, kontakViewModel, admin, aktif) }
                    )
                }
            )
            }
        }
    }

    /**
     * Fungsi 4 · pintu masuk Galeri fisik tanpa mengubah layar milik fungsi lain: tombol kecil di pojok kanan bawah.
     * Pramurukti/keluarga membuka galeri; admin membuka panel pantau galeri fisik.
     */
    @Composable
    private fun DenganGaleriFisik(konten: @Composable () -> Unit) {
        val obat by obatViewModel.state.collectAsStateWithLifecycle()
        val peran = obat.peranAkun.takeIf { !obat.perluMasuk }
        val bisaBuka = peran in setOf("PRAMURUKTI", "KELUARGA", "ADMIN")
        var terbuka by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(bisaBuka) { if (!bisaBuka) terbuka = false }

        // Alert perlu dokter (pengelola) dan kabar dokumentasi baru (keluarga), flowchart 2.2.4.
        val pantau = when (peran) {
            "ADMIN" -> PantauanFisik.admin(adminViewModel, pantauGaleriViewModel)
            "PRAMURUKTI", "KELUARGA" -> PantauanFisik.galeri(galeriViewModel)
            else -> PantauanFisik()
        }
        LaunchedEffect(peran, pantau.dimuat, pantau.untukNotifikasi) {
            if (pantau.dimuat && (peran == "ADMIN" || peran == "KELUARGA")) {
                notifikasiFisik.periksa(peran, pantau.untukNotifikasi, pantau.namaLansia)
            }
        }
        val izinNotifikasi = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(peran) {
            if ((peran == "ADMIN" || peran == "KELUARGA") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) izinNotifikasi.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Layar fungsi lain tetap tersusun di bawah supaya posisinya (mis. sedang di Catatan) tidak hilang saat galeri ditutup.
        Box(Modifier.fillMaxSize()) {
            konten()
            when {
                terbuka && peran == "ADMIN" -> PantauFisikAdmin { terbuka = false }
                terbuka && bisaBuka -> GaleriRoute(galeriViewModel, null) { terbuka = false }
                bisaBuka && peran == "ADMIN" -> {
                    // Di atas bottom bar pramurukti (72dp) supaya tidak menutupi tab; angka = dokumentasi perlu dokter.
                    BadgedBox(
                        badge = {
                            if (pantau.perluDokter > 0) Badge(containerColor = P3Warna.Accent, contentColor = P3Warna.Raised) { Text("${pantau.perluDokter}") }
                        },
                        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = 88.dp)
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = { terbuka = true },
                            icon = { P3Ikon(R.drawable.p3_ic_camera, ukuran = 20.dp) },
                            text = { Text("Galeri fisik", style = P3Teks.Label) },
                            containerColor = P3Warna.Brand,
                            contentColor = P3Warna.Ink
                        )
                    }
                }
            }
        }
    }

    /** Ringkasan untuk tombol Galeri & notifikasi Fungsi 4, sesuai peran yang sedang masuk. */
    private class PantauanFisik(
        val dimuat: Boolean = false,
        val perluDokter: Int = 0,
        /** Admin: daftar perlu dokter. Keluarga: semua dokumentasi. */
        val untukNotifikasi: List<FotoFisik> = emptyList(),
        val namaLansia: (String) -> String = { "Lansia" }
    ) {
        companion object {
            @Composable
            fun admin(adminVm: AdminViewModel, pantauVm: PantauGaleriViewModel): PantauanFisik {
                val admin by adminVm.state.collectAsStateWithLifecycle()
                val p by pantauVm.state.collectAsStateWithLifecycle()
                val nama = admin.lansia.associate { it.id to it.nama }
                LaunchedEffect(nama) { if (nama.isNotEmpty()) pantauVm.mulai(nama) }
                return PantauanFisik(p.dimuat && nama.isNotEmpty(), p.perluDokter.size, p.perluDokter) { nama[it] ?: "Lansia" }
            }

            @Composable
            fun galeri(vm: GaleriViewModel): PantauanFisik {
                val g by vm.state.collectAsStateWithLifecycle()
                return PantauanFisik(g.dimuat, g.foto.count { it.status == StatusDokumentasi.PERLU_DOKTER }, g.foto, g::namaLansia)
            }
        }
    }

    @Composable
    private fun PantauFisikAdmin(onKembali: () -> Unit) {
        val admin by adminViewModel.state.collectAsStateWithLifecycle()
        BackHandler(onBack = onKembali)
        Scaffold(
            containerColor = P3Warna.Paper,
            topBar = { Box(Modifier.statusBarsPadding()) { P3TopBarKembali("Pantau galeri fisik", onKembali) } }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = P3Tepi, vertical = 8.dp)) {
                PantauGaleriPanel(pantauGaleriViewModel, admin, aktif = !admin.sedangMemuat && !admin.sedangMenyimpan)
            }
        }
    }
}
