package com.example.rawatkasih.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.example.rawatkasih.ui.theme.Arang
import com.example.rawatkasih.ui.theme.Gading
import com.example.rawatkasih.ui.theme.Quicksand
import com.example.rawatkasih.ui.theme.Sage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val NAMA = "Rawat Kasih"

/**
 * Geometri logo R (sama dengan ikon launcher): goresan setebal [TEBAL] berujung bulat
 * di kotak [LEBAR] × [TINGGI]. Batang digambar dari bawah ke atas, lalu bahu + mangkuk, lalu palang
 * bawah dan kaki keluar bersamaan dari ujung mangkuk.
 */
private object GeometriR {
    const val LEBAR = 254.2f
    const val TINGGI = 379.2f
    const val TEBAL = 49.2f
    val batang = jalur("M24.6,354.6 L24.6,24.6")
    val mangkuk = jalur("M24.6,183.1 C24.6,120.1 63.9,87.7 112.1,87.7 L147.1,87.7 A82.5,82.5 0 0 1 147.1,252.7")
    val palang = jalur("M147.1,252.7 L24.6,252.7")
    val kaki = jalur("M138.9,252.7 L226.9,353.2")
    private fun jalur(d: String): Path = PathParser().parsePathString(d).toPath()
}

/** Kemajuan tiap tahap goresan (0 = belum, 1 = utuh). */
private class Goresan(val batang: Float = 1f, val mangkuk: Float = 1f, val akhir: Float = 1f)

private fun DrawScope.gambarR(warna: Color, g: Goresan) {
    val k = size.height / GeometriR.TINGGI
    val gaya = Stroke(GeometriR.TEBAL * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val skala = Matrix().apply { scale(k, k) }
    translate((size.width - GeometriR.LEBAR * k) / 2f, 0f) {
        fun goresan(jalur: Path, p: Float) {
            if (p <= 0f) return
            val besar = Path().apply { addPath(jalur); transform(skala) }
            val ukur = PathMeasure().apply { setPath(besar, false) }
            val bagian = Path().also { ukur.getSegment(0f, ukur.length * p.coerceAtMost(1f), it, true) }
            drawPath(bagian, warna, style = gaya)
        }
        goresan(GeometriR.batang, g.batang)
        goresan(GeometriR.mangkuk, g.mangkuk)
        goresan(GeometriR.palang, g.akhir)
        goresan(GeometriR.kaki, g.akhir)
    }
}

/** Logo R utuh setinggi [tinggi]. */
@Composable
fun LogoR(tinggi: Dp, modifier: Modifier = Modifier, warna: Color = Sage) {
    Canvas(modifier.height(tinggi).aspectRatio(GeometriR.LEBAR / GeometriR.TINGGI)) { gambarR(warna, Goresan()) }
}

private fun gayaNama(ukuran: TextUnit) =
    TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = ukuran, lineHeight = ukuran * 1.2f)

/** Sedikit melewati lalu kembali: kesan "pop" yang santai, bukan memantul. */
private val Lenting = CubicBezierEasing(0.34f, 1.45f, 0.64f, 1f)
private val Lembut = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val Mulus = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)
private val Mendarat = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
private val Pergi = CubicBezierEasing(0.4f, 0f, 1f, 1f)

/**
 * Splash: sebuah garis lurus tumbuh dari bawah, lalu meliuk menjadi bahu dan mangkuk R; palang dan kaki
 * keluar bersamaan dari ujung mangkuk, logo mengembang sedikit ("pop"), lalu "Rawat Kasih" muncul huruf
 * demi huruf. Bila animasi dimatikan di Pengaturan, logo dan nama langsung tampil utuh.
 */
@Composable
fun SplashScreen(onSelesai: () -> Unit) {
    // "Hapus animasi" di Pengaturan Android = skala durasi 0: logo dan nama langsung tampil utuh.
    val context = LocalContext.current
    val reduce = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val awal = if (reduce) 1f else 0f
    val batang = remember { Animatable(awal) }
    val mangkuk = remember { Animatable(awal) }
    val akhir = remember { Animatable(awal) }
    val pop = remember { Animatable(1f) }
    val huruf = remember { NAMA.map { Animatable(awal) } }
    val keluar = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reduce) { delay(800); onSelesai(); return@LaunchedEffect }
        delay(150)
        batang.animateTo(1f, tween(480, easing = Lembut))
        mangkuk.animateTo(1f, tween(620, easing = Mulus))
        akhir.animateTo(1f, tween(300, easing = Mendarat))
        launch {
            pop.animateTo(1.07f, tween(130, easing = Mendarat))
            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        }
        delay(120)
        huruf.mapIndexed { i, a -> async { delay(i * 38L); a.animateTo(1f, tween(420, easing = Lenting)) } }.awaitAll()
        delay(750)
        keluar.animateTo(1f, tween(280, easing = Pergi))
        onSelesai()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Gading)
            .graphicsLayer { alpha = 1f - keluar.value }
            .clearAndSetSemantics { contentDescription = NAMA },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(
            Modifier
                .height(150.dp)
                .width(150.dp * (GeometriR.LEBAR / GeometriR.TINGGI))
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
        ) { gambarR(Sage, Goresan(batang.value, mangkuk.value, akhir.value)) }
        Row(Modifier.padding(top = 22.dp)) {
            NAMA.forEachIndexed { i, ch ->
                val p = huruf[i].value
                Text(
                    ch.toString(),
                    style = gayaNama(42.sp),
                    color = Arang,
                    modifier = Modifier.graphicsLayer {
                        alpha = p.coerceIn(0f, 1f)
                        translationY = (1f - p) * 16.dp.toPx()
                    },
                )
            }
        }
    }
}
