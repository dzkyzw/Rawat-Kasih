package com.example.rawatkasih.screen

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rawatkasih.ui.components.Gilda
import com.example.rawatkasih.ui.components.GayaIsi
import com.example.rawatkasih.ui.components.WarnaSekunder
import com.example.rawatkasih.ui.components.WarnaTeks
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val WORDMARK = "Rawat Kasih"
private val Masuk = CubicBezierEasing(0.2f, 0f, 0f, 1f) // MD3: datang cepat, mendarat pelan
private val Tenang = CubicBezierEasing(0.4f, 0f, 0.2f, 1f) // benang ditarik merata
private val Keluar = CubicBezierEasing(0.3f, 0f, 1f, 1f)

/**
 * Splash monokrom: huruf "Rawat Kasih" naik satu per satu (jeda 40 ms, total < 0,5 detik), lalu benang logo
 * ditarik dari kaki R ke kanan di bawah nama dan ditutup titik, baru tagline. Sekitar 1,8 detik lalu memudar
 * ke layar berikutnya. Setelan "Hapus animasi" Android dihormati.
 */
@Composable
internal fun SplashScreen(onSelesai: () -> Unit) {
    val context = LocalContext.current
    val tanpaAnimasi = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val huruf = remember { WORDMARK.map { Animatable(if (tanpaAnimasi) 1f else 0f) } }
    val benang = remember { Animatable(if (tanpaAnimasi) 1f else 0f) }
    val tagline = remember { Animatable(if (tanpaAnimasi) 1f else 0f) }
    val keluar = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (tanpaAnimasi) { delay(600); onSelesai(); return@LaunchedEffect }
        delay(200)
        huruf.mapIndexed { i, a -> async { delay(i * 40L); a.animateTo(1f, tween(560, easing = Masuk)) } }
            .also {
                launch { delay(380); benang.animateTo(1f, tween(520, easing = Tenang)) }
                launch { delay(700); tagline.animateTo(1f, tween(420, easing = Masuk)) }
            }
            .awaitAll()
        delay(650)
        keluar.animateTo(1f, tween(280, easing = Keluar))
        onSelesai()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .graphicsLayer { alpha = 1f - keluar.value }
            .semantics { contentDescription = "Rawat Kasih. Merawat dengan sepenuh hati." },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        var baseline by remember { mutableFloatStateOf(0f) }
        Box {
            Row {
                WORDMARK.forEachIndexed { i, ch ->
                    val p = huruf[i].value
                    Text(
                        ch.toString(),
                        style = TextStyle(fontFamily = Gilda, fontSize = 50.sp, lineHeight = 56.sp),
                        color = WarnaTeks,
                        onTextLayout = { if (i == 0) baseline = it.firstBaseline },
                        modifier = Modifier
                            .graphicsLayer { alpha = p; translationY = (1f - p) * 14.dp.toPx() }
                            .blur(((1f - p) * 6f).dp)
                    )
                }
            }
            // Benang logo dalam satuan em Gilda (sama dengan logo_wordmark.xml): dari kaki R, menikung, lalu lurus.
            Canvas(Modifier.matchParentSize()) {
                if (benang.value == 0f) return@Canvas
                val em = 50.sp.toPx()
                val y0 = baseline - 0.002f * em
                val y1 = baseline + 0.096f * em
                val xs = 0.712f * em
                val ujung = size.width - 0.06f * em
                val jalur = Path().apply {
                    moveTo(0.64f * em, y0)
                    lineTo(xs, y0)
                    cubicTo(xs + 0.070f * em, y0, xs + 0.060f * em, y1, xs + 0.130f * em, y1)
                    lineTo(ujung, y1)
                }
                val ukur = PathMeasure().apply { setPath(jalur, false) }
                val sebagian = Path().also { ukur.getSegment(0f, ukur.length * benang.value, it, true) }
                drawPath(sebagian, WarnaTeks, style = Stroke(0.020f * em, cap = StrokeCap.Butt))
                if (benang.value > 0.98f) drawCircle(WarnaTeks, 0.036f * em * ((benang.value - 0.98f) * 50f), Offset(ujung + 0.025f * em, y1))
            }
        }
        Text(
            "Merawat dengan sepenuh hati.",
            style = GayaIsi,
            color = WarnaSekunder,
            modifier = Modifier
                .padding(top = 28.dp)
                .graphicsLayer { alpha = tagline.value; translationY = (1f - tagline.value) * 8.dp.toPx() }
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun SplashPreview() {
    SplashScreen(onSelesai = {})
}
