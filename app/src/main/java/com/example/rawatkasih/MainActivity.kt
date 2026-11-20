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
import com.example.rawatkasih.feature.obat.data.ObatRepository
import com.example.rawatkasih.screen.ObatRoute
import com.example.rawatkasih.ui.obat.ObatViewModel
import com.example.rawatkasih.ui.theme.RawatKasihTheme

class MainActivity : ComponentActivity() {
    private val obatViewModel: ObatViewModel by viewModels {
        viewModelFactory {
            initializer { ObatViewModel(ObatRepository(RawatKasihDatabase.ambil(applicationContext))) }
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
                ObatRoute(obatViewModel)
            }
        }
    }
}
