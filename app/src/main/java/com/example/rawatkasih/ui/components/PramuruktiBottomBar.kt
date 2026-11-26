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
import androidx.compose.ui.graphics.ColorFilter
import com.example.rawatkasih.ui.theme.Arang
import com.example.rawatkasih.ui.theme.Gading
import com.example.rawatkasih.ui.theme.Sage
import com.example.rawatkasih.ui.theme.SageMuda
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
    tabTersedia: Set<TabPramurukti> = TabPramurukti.entries.toSet(),
    onPilihTab: ((TabPramurukti) -> Unit)? = null
) {
    val warnaTeks = Arang
    val warnaItem = NavigationBarItemDefaults.colors(
        selectedTextColor = warnaTeks,
        unselectedTextColor = warnaTeks,
        disabledTextColor = warnaTeks,
        indicatorColor = SageMuda
    )
    NavigationBar(
        modifier = Modifier.navigationBarsPadding().height(72.dp),
        containerColor = Gading,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        TabPramurukti.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == tabAktif,
                onClick = { onPilihTab?.invoke(tab) },
                enabled = onPilihTab != null && tab in tabTersedia,
                icon = {
                    Image(
                        painter = painterResource(tab.ikon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(if (tab == tabAktif) Sage else Arang)
                    )
                },
                label = { Text(tab.label, fontSize = 12.sp, lineHeight = 17.sp) },
                colors = warnaItem
            )
        }
    }
}
