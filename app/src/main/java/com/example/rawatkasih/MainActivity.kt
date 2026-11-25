package com.example.rawatkasih

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rawatkasih.data.local.RawatKasihDatabase
import com.example.rawatkasih.data.remote.FirebaseP04
import com.example.rawatkasih.feature.admin.data.AdminRepository
import com.example.rawatkasih.feature.obat.data.ObatRepository
import com.example.rawatkasih.feature.obat.data.remote.FirebaseObatRepository
import com.example.rawatkasih.feature.obat.notification.AndroidPengingatObat
import com.example.rawatkasih.screen.AdminRoute
import com.example.rawatkasih.screen.ObatRoute
import com.example.rawatkasih.ui.admin.AdminViewModel
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.WHITE)
        )
        setContent {
            RawatKasihTheme {
                ObatRoute(obatViewModel) { bukaProfil ->
                    AdminRoute(adminViewModel, onProfil = bukaProfil, onKeluar = {
                        adminViewModel.reset()
                        obatViewModel.keluar()
                    })
                }
            }
        }
    }
}
