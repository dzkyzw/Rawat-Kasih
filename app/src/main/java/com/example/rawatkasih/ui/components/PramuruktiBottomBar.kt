package com.example.rawatkasih.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rawatkasih.R

internal enum class TabPramurukti(val label: String, @get:DrawableRes val ikon: Int) {
    BERANDA("Beranda", R.drawable.nav_pramurukti_beranda),
    LANSIA("Lansia", R.drawable.nav_pramurukti_lansia),
    CATATAN("Catatan", R.drawable.nav_pramurukti_catatan),
    AKUN("Akun", R.drawable.nav_pramurukti_akun)
}

@Composable
internal fun PramuruktiBottomBar(
    tabAktif: TabPramurukti,
    onPilihTab: ((TabPramurukti) -> Unit)? = null
) {
    val warnaTeks = Color(0xFF111111)
    val warnaItem = NavigationBarItemDefaults.colors(
        selectedTextColor = warnaTeks,
        unselectedTextColor = warnaTeks,
        disabledTextColor = warnaTeks,
        indicatorColor = Color(0xFFE8E8E8)
    )
    NavigationBar(
        modifier = Modifier.navigationBarsPadding().height(72.dp),
        containerColor = Color.White,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        TabPramurukti.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == tabAktif,
                onClick = { onPilihTab?.invoke(tab) },
                enabled = onPilihTab != null,
                icon = {
                    Image(
                        painter = painterResource(tab.ikon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(tab.label, fontSize = 12.sp, lineHeight = 17.sp) },
                colors = warnaItem
            )
        }
    }
}
