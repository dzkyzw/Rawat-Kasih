package com.example.rawatkasih.ui.galeri

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rawatkasih.R

/**
 * Design system "Prototype3" (Figma RawatKasih · Fondasi) untuk Fungsi 4: krem hangat, kartu putih radius 16,
 * pil status pastel bertitik, tombol hijau segar, Gabarito untuk judul & angka, Figtree untuk teks.
 */
internal object P3Warna {
    val Paper = Color(0xFFF9F4EC)
    val Paper2 = Color(0xFFEEE9DF)
    val Paper3 = Color(0xFFE2DBD1)
    val Raised = Color(0xFFFFFDF9)
    val Rule = Color(0xFFE4DFD7)
    val RuleStrong = Color(0xFFC3BDB3)
    val Muted = Color(0xFF6F6860)
    val Ink2 = Color(0xFF423C35)
    val Ink = Color(0xFF1E1A15)
    val Accent = Color(0xFFDE6B4F)
    val AccentInk = Color(0xFFA33E25)
    val AccentWash = Color(0xFFFFE7DE)
    val Ok = Color(0xFF21763C)
    val OkWash = Color(0xFFD7F5D7)
    val WarnInk = Color(0xFF875814)
    val Warn = Color(0xFFE3AD4B)
    val WarnWash = Color(0xFFFCF0CF)
    val Info = Color(0xFF7955A0)
    val InfoWash = Color(0xFFF2E0FB)
    val OnInk = Color(0xFFF9F4EC)
    val Brand = Color(0xFF71EF66)
    val BrandDeep = Color(0xFF0A4027)
    val BrandInk = Color(0xFF09672E)
}

private val Gabarito = FontFamily(
    Font(R.font.p3_gabarito_semibold, FontWeight.SemiBold),
    Font(R.font.p3_gabarito_bold, FontWeight.Bold)
)
private val Figtree = FontFamily(
    Font(R.font.p3_figtree_regular, FontWeight.Normal),
    Font(R.font.p3_figtree_semibold, FontWeight.SemiBold)
)
private val Fraunces = FontFamily(Font(R.font.p3_fraunces_semibold, FontWeight.SemiBold))

/** Gaya teks P3 sesuai text style Figma (letter spacing persen dikonversi ke sp). */
internal object P3Teks {
    val Wordmark = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.22).sp, color = P3Warna.BrandDeep)
    val Title = TextStyle(fontFamily = Gabarito, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.3).sp, color = P3Warna.Ink)
    val Heading = TextStyle(fontFamily = Gabarito, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.1).sp, color = P3Warna.Ink)
    val Data = TextStyle(fontFamily = Gabarito, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp, color = P3Warna.Ink)
    val Button = TextStyle(fontFamily = Gabarito, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.9.sp, color = P3Warna.Ink)
    val Subhead = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, color = P3Warna.Ink)
    val Body = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, color = P3Warna.Ink)
    val BodySmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = P3Warna.Ink2)
    val Label = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp, color = P3Warna.Ink)
    val Caption = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, color = P3Warna.Muted)
    val Micro = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 14.sp, color = P3Warna.Muted)
    val Overline = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.96.sp, color = P3Warna.Muted)
    val DataS = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.72.sp, color = P3Warna.Muted)
}

internal object P3Bentuk {
    val Md = RoundedCornerShape(12.dp)
    val Lg = RoundedCornerShape(16.dp)
}

/** Jarak tepi layar P3 (space/lg). */
internal val P3Tepi = 20.dp

@Composable
internal fun P3Ikon(@DrawableRes ikon: Int, modifier: Modifier = Modifier, ukuran: Dp = 24.dp, warna: Color = P3Warna.Ink) {
    Icon(painterResource(ikon), contentDescription = null, modifier = modifier.size(ukuran), tint = warna)
}

/** Top bar "Back": panah + judul rata kiri + aksi teks opsional. */
@Composable
internal fun P3TopBarKembali(judul: String, onKembali: () -> Unit, aksi: String? = null, onAksi: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp, end = P3Tepi),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .clickable(role = Role.Button, onClick = onKembali)
                .semantics { contentDescription = "Kembali" },
            contentAlignment = Alignment.Center
        ) { P3Ikon(R.drawable.p3_ic_arrow_left) }
        Text(judul, Modifier.weight(1f).semantics { heading() }, style = P3Teks.Heading, maxLines = 1)
        if (aksi != null && onAksi != null) {
            Text(aksi, Modifier.clickable(role = Role.Button, onClick = onAksi).padding(vertical = 8.dp), style = P3Teks.Label.copy(color = P3Warna.BrandInk))
        }
    }
}

/** Top bar "Home": wordmark RawatKasih + titik hijau, avatar inisial akun di kanan. */
@Composable
internal fun P3TopBarBeranda(inisial: String, onKembali: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(start = if (onKembali != null) 4.dp else P3Tepi, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onKembali != null) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onKembali)
                    .semantics { contentDescription = "Kembali" },
                contentAlignment = Alignment.Center
            ) { P3Ikon(R.drawable.p3_ic_arrow_left) }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("RawatKasih", style = P3Teks.Wordmark)
            Box(Modifier.padding(top = 4.dp).size(8.dp).background(P3Warna.Brand, RoundedCornerShape(4.dp)))
        }
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(32.dp).background(P3Warna.Raised, CircleShape).border(1.dp, P3Warna.Rule, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(inisial, style = P3Teks.Data.copy(color = P3Warna.BrandDeep)) }
        }
    }
}

@Composable
internal fun P3JudulHalaman(judul: String, meta: String? = null, overline: Boolean = true) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(judul, Modifier.semantics { heading() }, style = P3Teks.Title)
        if (meta != null) Text(if (overline) meta.uppercase() else meta, style = P3Teks.Overline)
    }
}

/** P3 / Choice: pil pilihan; aktif = blok brand-deep dengan centang. */
@Composable
internal fun P3Pilihan(label: String, terpilih: Boolean, onClick: () -> Unit, enabled: Boolean = true, tinggi: Dp = 36.dp) {
    Row(
        Modifier.height(tinggi).clip(CircleShape)
            .background(if (terpilih) P3Warna.BrandDeep else P3Warna.Paper2)
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (terpilih) P3Ikon(R.drawable.p3_ic_check, ukuran = 16.dp, warna = P3Warna.OnInk)
        Text(label, style = P3Teks.Label.copy(color = if (terpilih) P3Warna.OnInk else P3Warna.Ink))
    }
}

/** P3 / Field: label di atas, isian bergaris bawah, keterangan di bawah; galat = garis aksen 2dp. */
@Composable
internal fun P3Field(
    label: String,
    nilai: String,
    onNilai: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keterangan: String? = null,
    galat: String? = null,
    onKlik: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val interaksi = remember { MutableInteractionSource() }
    val fokus by interaksi.collectIsFocusedAsState()
    val garis = when {
        galat != null -> P3Warna.Accent
        fokus -> P3Warna.Ink
        else -> P3Warna.RuleStrong
    }
    val tebal = if (galat != null || fokus) 2.dp else 1.5.dp
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = P3Teks.BodySmall.copy(color = P3Warna.Muted))
        Row(
            Modifier.fillMaxWidth().height(46.dp)
                .then(if (onKlik != null) Modifier.clickable(role = Role.DropdownList, onClick = onKlik) else Modifier)
                .drawBehind {
                    val y = size.height - tebal.toPx() / 2
                    drawLine(garis, Offset(0f, y), Offset(size.width, y), tebal.toPx())
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                if (onNilai != null) {
                    BasicTextField(
                        value = nilai,
                        onValueChange = onNilai,
                        singleLine = true,
                        textStyle = P3Teks.Body,
                        cursorBrush = SolidColor(P3Warna.Ink),
                        interactionSource = interaksi,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (nilai.isEmpty()) Text(placeholder, style = P3Teks.Body.copy(color = P3Warna.Muted))
                } else {
                    Text(nilai.ifEmpty { placeholder }, style = P3Teks.Body.copy(color = if (nilai.isEmpty()) P3Warna.Muted else P3Warna.Ink))
                }
            }
            trailing?.invoke()
        }
        when {
            galat != null -> Text(galat, style = P3Teks.Caption.copy(color = P3Warna.AccentInk))
            keterangan != null -> Text(keterangan, style = P3Teks.Caption)
        }
    }
}

/** Textarea P3: kartu raised radius 16, padding 14, tinggi minimal 104. */
@Composable
internal fun P3AreaTeks(nilai: String, onNilai: (String) -> Unit, placeholder: String, galat: Boolean = false) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 104.dp).clip(P3Bentuk.Lg).background(P3Warna.Raised)
            .then(if (galat) Modifier.border(2.dp, P3Warna.Accent, P3Bentuk.Lg) else Modifier)
            .padding(14.dp)
    ) {
        BasicTextField(
            value = nilai,
            onValueChange = onNilai,
            textStyle = P3Teks.Body,
            cursorBrush = SolidColor(P3Warna.Ink),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )
        if (nilai.isEmpty()) Text(placeholder, style = P3Teks.Body.copy(color = P3Warna.Muted))
    }
}

internal enum class P3Nada { INFO, WARN, BAHAYA, OK, NETRAL }

/** P3 / Banner: pemberitahuan inline dengan latar wash, tanpa garis samping. */
@Composable
internal fun P3Banner(nada: P3Nada, judul: String, isi: String? = null, onClick: (() -> Unit)? = null) {
    val (latar, ikon, warnaIkon) = when (nada) {
        P3Nada.INFO -> Triple(P3Warna.InfoWash, R.drawable.p3_ic_info, P3Warna.Info)
        P3Nada.WARN -> Triple(P3Warna.WarnWash, R.drawable.p3_ic_alert, P3Warna.WarnInk)
        P3Nada.BAHAYA -> Triple(P3Warna.AccentWash, R.drawable.p3_ic_alert, P3Warna.AccentInk)
        P3Nada.OK -> Triple(P3Warna.OkWash, R.drawable.p3_ic_check, P3Warna.Ok)
        P3Nada.NETRAL -> Triple(P3Warna.Paper2, R.drawable.p3_ic_info, P3Warna.Muted)
    }
    Row(
        Modifier.fillMaxWidth().clip(P3Bentuk.Lg).background(latar)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        P3Ikon(ikon, ukuran = 20.dp, warna = warnaIkon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(judul, style = P3Teks.Label)
            if (isi != null) Text(isi, style = P3Teks.BodySmall)
        }
        if (onClick != null) P3Ikon(R.drawable.p3_ic_chevron_right, ukuran = 20.dp)
    }
}

/** P3 / Button: aksi lebar penuh, label kapital di tengah dengan ikon arah di kanan. */
@Composable
internal fun P3Tombol(
    label: String,
    onClick: () -> Unit,
    @DrawableRes ikon: Int? = R.drawable.p3_ic_check,
    enabled: Boolean = true,
    memuat: Boolean = false
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(P3Bentuk.Md)
            .background(if (enabled) P3Warna.Brand else P3Warna.Paper3)
            .clickable(enabled = enabled && !memuat, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label.uppercase(), style = P3Teks.Button.copy(color = if (enabled) P3Warna.Ink else P3Warna.Muted))
        when {
            memuat -> CircularProgressIndicator(Modifier.size(18.dp), color = P3Warna.Ink, strokeWidth = 2.dp)
            ikon != null -> P3Ikon(ikon, ukuran = 18.dp, warna = if (enabled) P3Warna.Ink else P3Warna.Muted)
        }
    }
}

/** P3 / Status: pil pastel dengan titik dan label. */
@Composable
internal fun P3Status(label: String, nada: P3Nada) {
    val (latar, teks) = when (nada) {
        P3Nada.OK -> P3Warna.OkWash to P3Warna.Ok
        P3Nada.INFO -> P3Warna.InfoWash to P3Warna.Info
        P3Nada.WARN -> P3Warna.WarnWash to P3Warna.WarnInk
        P3Nada.BAHAYA -> P3Warna.AccentWash to P3Warna.AccentInk
        P3Nada.NETRAL -> P3Warna.Paper2 to P3Warna.Muted
    }
    Row(
        Modifier.height(22.dp).background(latar, CircleShape).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).background(teks, CircleShape))
        Text(label, style = P3Teks.Micro.copy(color = teks))
    }
}

/** Kartu raised radius 16 (satu lapis, tanpa kartu di dalam kartu). */
@Composable
internal fun P3Kartu(modifier: Modifier = Modifier, padding: Dp = 16.dp, isi: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth().clip(P3Bentuk.Lg).background(P3Warna.Raised).padding(padding)) { isi() }
}

/** P3 / Row dengan kotak ikon bulat 40, teks, dan chevron. */
@Composable
internal fun P3BarisAksi(@DrawableRes ikon: Int, judul: String, sub: String?, onClick: () -> Unit, nadaIkon: Color = P3Warna.Paper2) {
    Row(
        Modifier.fillMaxWidth().clip(P3Bentuk.Lg).background(P3Warna.Raised)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).background(nadaIkon, CircleShape), contentAlignment = Alignment.Center) {
            P3Ikon(ikon, ukuran = 20.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(judul, style = P3Teks.Subhead)
            if (sub != null) Text(sub, style = P3Teks.BodySmall.copy(color = P3Warna.Muted))
        }
        P3Ikon(R.drawable.p3_ic_chevron_right, ukuran = 20.dp)
    }
}

/** Judul bagian (P3 / Heading) di atas daftar. */
@Composable
internal fun P3KepalaBagian(teks: String, modifier: Modifier = Modifier) {
    Text(teks, modifier.fillMaxWidth().semantics { heading() }, style = P3Teks.Heading)
}

/** Catatan kaki: ikon info 16 + teks caption. */
@Composable
internal fun P3CatatanKaki(teks: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        P3Ikon(R.drawable.p3_ic_info, Modifier.padding(top = 1.dp), ukuran = 16.dp, warna = P3Warna.Muted)
        Text(teks, style = P3Teks.Caption)
    }
}

/** P3 / Toast: pil brand-deep dengan titik hijau dan aksi opsional. */
@Composable
internal fun P3Toast(pesan: String, modifier: Modifier = Modifier, aksi: String? = null, onAksi: (() -> Unit)? = null) {
    Surface(modifier.fillMaxWidth(), shape = P3Bentuk.Lg, color = P3Warna.BrandDeep, shadowElevation = 6.dp) {
        Row(
            Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(P3Warna.Brand, CircleShape))
            Text(pesan, Modifier.weight(1f), style = P3Teks.BodySmall.copy(color = P3Warna.OnInk))
            if (aksi != null && onAksi != null) {
                Text(aksi.uppercase(), Modifier.clickable(role = Role.Button, onClick = onAksi).padding(4.dp), style = P3Teks.Micro.copy(color = P3Warna.Brand, letterSpacing = 0.72.sp))
            }
        }
    }
}

/** P3 / Photo slot kosong: blok paper-3 dengan ikon gambar dan label. */
@Composable
internal fun P3SlotKosong(label: String, modifier: Modifier) {
    Column(
        modifier.clip(P3Bentuk.Lg).background(P3Warna.Paper3),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        P3Ikon(R.drawable.p3_ic_image, ukuran = 20.dp, warna = P3Warna.Muted)
        Text(label, style = P3Teks.Micro)
    }
}

/** Slot aksi foto bergaris putus-putus ("Ambil foto" tegas, "Dari galeri" lembut). */
@Composable
internal fun P3SlotAksi(@DrawableRes ikon: Int, label: String, tegas: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val warna = if (tegas) P3Warna.Ink else P3Warna.RuleStrong
    val tebal = if (tegas) 1.5.dp else 1.dp
    Column(
        Modifier.size(104.dp).clip(P3Bentuk.Lg)
            .drawBehind {
                val px = tebal.toPx()
                drawRoundRect(
                    color = warna,
                    topLeft = Offset(px / 2, px / 2),
                    size = Size(size.width - px, size.height - px),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = px, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
                    )
                )
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        P3Ikon(ikon, ukuran = 22.dp, warna = if (tegas) P3Warna.Ink else P3Warna.Muted)
        Text(label, style = P3Teks.Micro.copy(color = if (tegas) P3Warna.Ink else P3Warna.Muted))
    }
}

/** Avatar inisial bulat (dipakai kartu konteks tindak lanjut). */
@Composable
internal fun P3Avatar(inisial: String, ukuran: Dp = 40.dp, latar: Color = P3Warna.OkWash) {
    Box(Modifier.size(ukuran).background(latar, CircleShape), contentAlignment = Alignment.Center) {
        Text(inisial, style = P3Teks.Data.copy(color = P3Warna.BrandDeep))
    }
}

@Composable
internal fun P3Jarak(tinggi: Dp) = Spacer(Modifier.height(tinggi))

@Composable
internal fun P3JarakLebar(lebar: Dp) = Spacer(Modifier.width(lebar))

/** Garis tipis pemisah antarbaris dalam satu kartu. */
@Composable
internal fun P3Garis() = Box(Modifier.fillMaxWidth().height(1.dp).background(P3Warna.Rule))
