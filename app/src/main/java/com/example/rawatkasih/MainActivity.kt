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
import com.example.rawatkasih.screen.AdminRoute
import com.example.rawatkasih.screen.CatatanRoute
import com.example.rawatkasih.screen.HandoverRoute
import com.example.rawatkasih.screen.ObatRoute
import com.example.rawatkasih.screen.ShiftAdminPanel
import com.example.rawatkasih.screen.SplashScreen
import com.example.rawatkasih.ui.admin.AdminViewModel
import com.example.rawatkasih.ui.catatan.CatatanViewModel
import com.example.rawatkasih.ui.catatan.ShiftAdminViewModel
import com.example.rawatkasih.ui.obat.ObatViewModel
import com.example.rawatkasih.ui.theme.RawatKasihTheme

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.WHITE)
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
        HandoverRoute(catatanViewModel) {
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
                        panelShift = { admin, aktif -> ShiftAdminPanel(shiftAdminViewModel, admin, aktif, onBerubah = adminViewModel::muat) }
                    )
                },
                catatanContent = { kembali, bukaProfil -> CatatanRoute(catatanViewModel, kembali, bukaProfil) }
            )
        }
    }
}
