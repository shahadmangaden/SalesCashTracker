package com.jash.salescashtracker.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.jash.salescashtracker.R
import com.jash.salescashtracker.ui.theme.Inter
import java.text.DecimalFormat
import java.util.Locale


/** A colour theme. Values come straight from the theme mockups. */
data class Palette(
    val id: String,
    val name: String,
    val bg: List<Color>,                 // page backdrop (top -> bottom)
    val glow: Float,                     // strength of the soft accent glow behind the page
    val darkBg: Boolean,                 // light text / status icons over the backdrop
    val darkCard: Boolean,               // cards are dark (navy / black themes)
    val onBg: Color, val onBgMuted: Color,
    val ink: Color, val muted: Color,    // text on cards
    val primary: Color, val onPrimary: Color,          // selected chips, main button
    val accent: Color, val accent2: Color, val onAccent: Color, val onAccentSub: Color, val accentSoft: Color,
    val card: Color, val sheet: Color, val field: Color,
    val tile: Color, val divider: Color, val track: Color, val ringFill: Color, val due: Color,
    val panel: Color, val panelEdge: Color, val rowEdge: Color, val rowIcon: Color,
    val panelText: Color, val panelTitle: Color, val panelSub: Color,
    val navBg: Color, val navEdge: Color, val navActiveBg: Color, val navActive: Color, val navIdle: Color,
    val shadow: Color, val accentShadow: Color,
)

// ---------- Design tokens: Light / Legal Navy / Arctic Frost (iOS-style redesign) ----------

val LightPalette = Palette(
    id = "light", name = "Light",
    bg = listOf(Color(0xFFF5F5F5), Color(0xFFF5F5F5)), glow = 0f, darkBg = false, darkCard = false,
    onBg = Color(0xFF171717), onBgMuted = Color(0xFF737373),
    ink = Color(0xFF171717), muted = Color(0xFF737373),
    primary = Color(0xFF171717), onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF171717), accent2 = Color(0xFF2E2E2E), onAccent = Color(0xFFFFFFFF), onAccentSub = Color(0xFFA3A3A3), accentSoft = Color(0xFFEDEDED),
    card = Color.White, sheet = Color(0xFFF5F5F5), field = Color(0xFFEFEFEF),
    tile = Color(0xFFEFEFEF), divider = Color(0xFFE5E5E5), track = Color(0xFFE5E5E5), ringFill = Color(0xFF171717), due = Color(0xFFC2410C),
    panel = Color(0xFF171717), panelEdge = Color(0xFF171717), rowEdge = Color(0xFF404040), rowIcon = Color(0xFF404040),
    panelText = Color(0xFFFAFAFA), panelTitle = Color(0xFFFAFAFA), panelSub = Color(0xFFA3A3A3),
    navBg = Color.White.copy(alpha = 0.82f), navEdge = Color.White.copy(alpha = 0.9f), navActiveBg = Color(0xFFEFEFEF),
    navActive = Color(0xFF171717), navIdle = Color(0xFF171717),
    shadow = Color(0xFF000000), accentShadow = Color(0xFF171717),
)
val LegalPalette = Palette(
    id = "legal", name = "Legal Navy",
    bg = listOf(Color(0xFFF8F9FA), Color(0xFFF8F9FA)), glow = 0f, darkBg = false, darkCard = false,
    onBg = Color(0xFF1F2E4A), onBgMuted = Color(0xFF5C6670),
    ink = Color(0xFF1F2E4A), muted = Color(0xFF5C6670),
    primary = Color(0xFF1F2E4A), onPrimary = Color(0xFFF8F9FA),
    accent = Color(0xFF1F2E4A), accent2 = Color(0xFF2D3F60), onAccent = Color(0xFFF8F9FA), onAccentSub = Color(0xFFCED4DA), accentSoft = Color(0xFFE3E8F0),
    card = Color.White, sheet = Color(0xFFF8F9FA), field = Color(0xFFE9ECEF),
    tile = Color(0xFFE9ECEF), divider = Color(0xFFDEE2E6), track = Color(0xFFDEE2E6), ringFill = Color(0xFF1F2E4A), due = Color(0xFFC2410C),
    panel = Color(0xFF343A40), panelEdge = Color(0xFF343A40), rowEdge = Color(0xFF4A5058), rowIcon = Color(0xFF4A5058),
    panelText = Color(0xFFF8F9FA), panelTitle = Color(0xFFF8F9FA), panelSub = Color(0xFFCED4DA),
    navBg = Color.White.copy(alpha = 0.82f), navEdge = Color.White.copy(alpha = 0.9f), navActiveBg = Color(0xFFE9ECEF),
    navActive = Color(0xFF1F2E4A), navIdle = Color(0xFF1F2E4A),
    shadow = Color(0xFF1F2E4A), accentShadow = Color(0xFF1F2E4A),
)
val ArcticPalette = Palette(
    id = "arctic", name = "Arctic Frost",
    bg = listOf(Color(0xFFEAF1F4), Color(0xFFEAF1F4)), glow = 0f, darkBg = false, darkCard = false,
    onBg = Color(0xFF0A2E36), onBgMuted = Color(0xFF557580),
    ink = Color(0xFF0A2E36), muted = Color(0xFF557580),
    primary = Color(0xFF0A2E36), onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF2A5A6D), accent2 = Color(0xFF3B6E82), onAccent = Color(0xFFFFFFFF), onAccentSub = Color(0xFFD2E0E6), accentSoft = Color(0xFFDCE9EE),
    card = Color.White, sheet = Color(0xFFEAF1F4), field = Color(0xFFE1EBEF),
    tile = Color(0xFFE1EBEF), divider = Color(0xFFD2E0E6), track = Color(0xFFD2E0E6), ringFill = Color(0xFF2A5A6D), due = Color(0xFFC2410C),
    panel = Color(0xFF0A2E36), panelEdge = Color(0xFF0A2E36), rowEdge = Color(0xFF1F4A55), rowIcon = Color(0xFF1F4A55),
    panelText = Color(0xFFEAF1F4), panelTitle = Color(0xFFEAF1F4), panelSub = Color(0xFF8FB7C2),
    navBg = Color.White.copy(alpha = 0.82f), navEdge = Color.White.copy(alpha = 0.9f), navActiveBg = Color(0xFFE1EBEF),
    navActive = Color(0xFF2A5A6D), navIdle = Color(0xFF0A2E36),
    shadow = Color(0xFF0A2E36), accentShadow = Color(0xFF2A5A6D),
)

val Palettes = listOf(LightPalette, LegalPalette, ArcticPalette)

/** The active theme; reading it inside composables recomposes them when the theme changes. */
object ThemeState {
    var palette by androidx.compose.runtime.mutableStateOf(LightPalette)
    /** True when the phone's "Remove animations" setting is on (Reduce Motion). */
    var reduceMotion by androidx.compose.runtime.mutableStateOf(false)
    fun load(ctx: android.content.Context) {
        val id = ctx.getSharedPreferences("ui", android.content.Context.MODE_PRIVATE).getString("theme", null)
        palette = Palettes.firstOrNull { it.id == id } ?: LightPalette   // older themes fall back to Light
        reduceMotion = runCatching {
            android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    fun set(ctx: android.content.Context, p: Palette) {
        palette = p
        ctx.getSharedPreferences("ui", android.content.Context.MODE_PRIVATE).edit().putString("theme", p.id).apply()
    }
}

object Glass {
    private val p get() = ThemeState.palette
    val Ink get() = p.ink
    val Muted get() = p.muted
    val OnBg get() = p.onBg
    val OnBgMuted get() = p.onBgMuted
    val Blue get() = p.primary            // primary / selected
    val Tint get() = p.primary
    val Charcoal get() = p.primary
    val OnPrimary get() = p.onPrimary
    val Yellow get() = p.accent           // accent
    val Accent2 get() = p.accent2
    val OnAccent get() = p.onAccent
    val OnAccentSub get() = p.onAccentSub
    val YellowSoft get() = p.accentSoft
    val BlueBg get() = p.accentSoft
    val Card get() = p.card
    val Sheet get() = p.sheet
    val Field get() = p.field
    val Dark get() = p.darkCard
    val Green get() = if (p.darkCard) Color(0xFF4ADE80) else Color(0xFF0E9F6E)
    val GreenBg get() = if (p.darkCard) Color(0xFF4ADE80).copy(alpha = 0.16f) else Color(0xFFDDF5EA)
    val Amber get() = if (p.darkCard) p.due else Color(0xFFB7791F)
    val AmberBg get() = if (p.darkCard) p.due.copy(alpha = 0.16f) else Color(0xFFFDF0D2)
    val Red get() = if (p.darkCard) Color(0xFFF87171) else Color(0xFFDC2626)
    val RedBg get() = if (p.darkCard) Color(0xFFF87171).copy(alpha = 0.16f) else Color(0xFFFDE4E4)
    val Due get() = p.due
    val NeutralBg get() = p.tile
    val TileBg get() = p.tile
    val Divider get() = p.divider
    val Track get() = p.track
    val RingFill get() = p.ringFill
    val CloseBg get() = p.tile
    val Fill get() = p.card.copy(alpha = 0.9f)
    val Edge get() = if (p.darkCard) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.9f)
    val Shadow get() = p.shadow
    val Panel get() = p.panel
    val PanelEdge get() = p.panelEdge
    val RowEdge get() = p.rowEdge
    val RowIcon get() = p.rowIcon
    val PanelText get() = p.panelText
    val PanelTitle get() = p.panelTitle
    val PanelSub get() = p.panelSub
    // Older hero names now map to the dark list panel
    val HeroTop get() = p.panel
    val HeroBottom get() = p.panel
    val HeroText get() = p.panelText
    val HeroMuted get() = p.panelSub
    val HeroTrack get() = p.rowEdge
}

val DonutColors get() = if (ThemeState.palette.darkCard) listOf(
    ThemeState.palette.accent, Color(0xFF8FA3C4), Color(0xFFFFB86B), Color(0xFF5B6B88),
    Color(0xFFE8F5C8), Color(0xFF3A4A68), Color(0xFFC9B79C), Color(0xFF6B7280),
) else listOf(
    Color(0xFFF5C842), Color(0xFF1C1B19), Color(0xFFD9A441), Color(0xFF8A857C),
    Color(0xFFF2D98C), Color(0xFF4A4A4F), Color(0xFFC9B79C), Color(0xFFE8E2D6),
)

val AvatarColors get() = if (ThemeState.palette.darkCard) listOf(ThemeState.palette.accentSoft, ThemeState.palette.tile, Color(0xFF1F3A33), Color(0xFF3A2E22))
    else listOf(Color(0xFFFCEFC2), Color(0xFFECE8E0), Color(0xFFDDF5EA), Color(0xFFF4E3D3))

/** Currency code from the business settings, available everywhere in the UI. */
val LocalCurrency = compositionLocalOf { "AED" }

// ---------- Formatting helpers used by the design ----------

private val wholeFmt = DecimalFormat("#,##0.##")

/** 2400.0 -> "2,400", 12.5 -> "12.5" */
fun amt(v: Double?): String = wholeFmt.format(v ?: 0.0)

/** "AED 2,400" */
fun cur(v: Double?, code: String): String = "$code ${amt(v)}"

/** AL NOOR TRAVEL -> Al Noor Travel */
fun nice(s: String): String = s.trim().lowercase(Locale.ENGLISH).split(Regex("\\s+")).joinToString(" ") { w ->
    if (w.length <= 3 && w.all { it.isLetter() } && w in setOf("ksa", "uae", "dxb", "pnr", "usa", "uk")) w.uppercase()
    else w.replaceFirstChar { it.titlecase(Locale.ENGLISH) }
}

/** FLIGHT TICKET -> Flight ticket */
fun sentence(s: String): String = s.trim().lowercase(Locale.ENGLISH).replaceFirstChar { it.titlecase(Locale.ENGLISH) }
    .replace(Regex("\\bksa\\b", RegexOption.IGNORE_CASE), "KSA").replace(Regex("\\buae\\b", RegexOption.IGNORE_CASE), "UAE")
    .replace("multipple", "multiple", ignoreCase = true)

fun initials(name: String): String = name.trim().split(Regex("[\\s/]+")).filter { it.isNotBlank() && it[0].isLetterOrDigit() }
    .take(2).joinToString("") { it.take(1).uppercase() }.ifBlank { "?" }

@DrawableRes
fun productIcon(category: String): Int {
    val c = category.uppercase()
    return when {
        "FLIGHT" in c || "TICKET" in c -> R.drawable.lc_plane
        "BUS" in c -> R.drawable.lc_bus
        "UMRAH" in c || "HAJJ" in c -> R.drawable.lc_moon_star
        "HOTEL" in c -> R.drawable.lc_hotel
        "INSURANCE" in c -> R.drawable.lc_shield_plus
        "CAR" in c -> R.drawable.lc_car
        "TYPING" in c || "DOCUMENT" in c -> R.drawable.lc_file_text
        "TOUR" in c || "PACKAGE" in c -> R.drawable.lc_package
        "VISA" in c || "ENTRY" in c -> R.drawable.lc_stamp
        else -> R.drawable.lc_ticket
    }
}

// ---------- Surfaces ----------

/** Page backdrop: flat theme colour plus the soft blurred accent glow from the mockups. */
@Composable
fun GlassBackground(forceLight: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    val pal = ThemeState.palette
    val light = forceLight && pal.darkBg
    Box(Modifier.fillMaxSize().background(if (light) Brush.verticalGradient(listOf(Color(0xFFF7F4EE), Color(0xFFEFEBE4))) else Brush.verticalGradient(pal.bg))) {
        if (!light && pal.glow > 0f) Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            fun blob(c: Color, x: Float, y: Float, r: Float) =
                drawCircle(Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center = Offset(x, y), radius = r), r, Offset(x, y))
            // 260px circle, right -110px, top 470px on an 844px artboard, blurred 80px
            blob(pal.accent.copy(alpha = pal.glow), w * 1.05f, h * 0.71f, w * 0.62f)
            blob(pal.accent.copy(alpha = pal.glow * 0.5f), w * -0.1f, h * 0.12f, w * 0.5f)
        }
        content()
    }
}

/** Frosted glass: white gradient fill, bright fading edge, soft floating shadow. */
fun Modifier.glass(radius: Dp = 24.dp, alpha: Float = 0.66f, shadow: Boolean = true): Modifier {
    // iOS-style card: solid white surface with a very soft shadow
    val shape = RoundedCornerShape(radius)
    return this
        .shadow(if (shadow) 10.dp else 4.dp, shape, ambientColor = Glass.Shadow.copy(alpha = 0.05f), spotColor = Glass.Shadow.copy(alpha = 0.08f))
        .clip(shape)
        .background(Glass.Card)
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(18.dp),
    spacing: Dp = 0.dp,
    radius: Dp = 24.dp,
    alpha: Float = 0.66f,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .glass(radius, alpha)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

// ---------- Text ----------

@Composable
fun T(
    text: String,
    size: TextUnit,
    color: Color = Glass.Ink,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    align: TextAlign? = null,
) = Text(
    text, modifier = modifier, color = color, fontSize = size, fontWeight = weight, fontFamily = Inter,
    lineHeight = 1.3.em, maxLines = maxLines, overflow = TextOverflow.Ellipsis, textAlign = align,
)

// ---------- Icons ----------

@Composable
fun Lc(@DrawableRes id: Int, tint: Color = Glass.Ink, size: Dp = 20.dp, modifier: Modifier = Modifier, description: String? = null) =
    Icon(painterResource(id), description, tint = tint, modifier = modifier.size(size))

@Composable
fun IconTile(@DrawableRes id: Int, bg: Color = Glass.TileBg, tint: Color = Glass.Blue, size: Dp = 36.dp, radius: Dp = 12.dp, iconSize: Dp = 20.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(radius)).background(bg), contentAlignment = Alignment.Center) {
        Lc(id, tint, iconSize)
    }
}

@Composable
fun InitialsAvatar(name: String, index: Int, size: Dp = 37.dp, radius: Dp = 13.dp, fontSize: TextUnit = 12.sp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(radius)).background(AvatarColors[index % AvatarColors.size]), contentAlignment = Alignment.Center) {
        T(initials(name), fontSize, Glass.Ink, FontWeight.Bold)
    }
}

// ---------- Controls ----------

/** Round 42dp glass button at the right of page headings. */
@Composable
fun HeadingAction(@DrawableRes id: Int, onClick: () -> Unit, description: String? = null) {
    Box(
        Modifier.size(42.dp).glass(100.dp, shadow = false).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Lc(id, Glass.Ink, 20.dp, description = description) }
}

@Composable
fun PageHeading(title: String, subtitle: String, @DrawableRes action: Int? = null, onAction: () -> Unit = {}, actionDescription: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            T(title, 32.sp, Glass.OnBg, FontWeight.Bold)
            T(subtitle, 12.sp, Glass.OnBgMuted)
        }
        if (action != null) HeadingAction(action, onAction, actionDescription)
    }
}

@Composable
fun SectionHeading(title: String, action: String? = null, onAction: (() -> Unit)? = null) =
    IosSection(title, action, onAction != null, onAction)

/** Selectable pill ("Choice" in the design). */
@Composable
fun Choice(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, large: Boolean = false) {
    val shape = RoundedCornerShape(100.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) Glass.Charcoal else Glass.Card)
            .border(1.dp, if (selected) Glass.Charcoal else Glass.Edge, shape)
            .clickable(onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = if (large) 14.dp else 12.dp, vertical = if (large) 10.dp else 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        T(text, 13.sp, if (selected) Glass.OnPrimary else Glass.Ink, if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
fun StatusPill(text: String, fg: Color, bg: Color, size: TextUnit = 10.sp) {
    Box(Modifier.clip(RoundedCornerShape(100.dp)).background(bg).padding(horizontal = 9.dp, vertical = 5.dp)) {
        T(text, size, fg, FontWeight.SemiBold)
    }
}

/** Payment pill for a sale: Paid / Partial / Pending / Balance due. */
@Composable
fun PaymentPill(status: String, due: Double) {
    when (status.uppercase()) {
        "PAID" -> StatusPill("Paid", Glass.Green, Glass.GreenBg)
        "PARTIAL" -> StatusPill("Partial", Glass.Amber, Glass.AmberBg)
        "PENDING" -> StatusPill("Awaiting", Glass.Red, Glass.RedBg)
        "REFUNDED" -> StatusPill("Refunded", Glass.Red, Glass.RedBg)
        else -> if (due > 0.5) StatusPill("Balance due", Glass.Amber, Glass.AmberBg)
    }
}

@Composable
fun PrimaryButton(text: String, @DrawableRes icon: Int?, onClick: () -> Unit, modifier: Modifier = Modifier, busy: Boolean = false) {
    val shape = RoundedCornerShape(100.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(12.dp, shape, ambientColor = Glass.Shadow.copy(alpha = 0.14f), spotColor = Glass.Shadow.copy(alpha = 0.3f))
            .clip(shape)
            .background(Glass.Blue)
            .clickable(enabled = !busy, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) CircularProgressIndicator(color = Glass.OnPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        else {
            if (icon != null) { Lc(icon, Glass.OnPrimary, 20.dp); Spacer(Modifier.width(9.dp)) }
            T(text, 16.sp, Glass.OnPrimary, FontWeight.Bold)
        }
    }
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) = T(text, 12.sp, Glass.Muted, FontWeight.SemiBold, modifier.fillMaxWidth())

/** Text input from the design: label above, white 86% field, 46dp tall, 15dp corners, icon on the right. */
@Composable
fun GlassInput(
    label: String?,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    @DrawableRes icon: Int? = null,
    onIcon: (() -> Unit)? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    caps: KeyboardCapitalization = KeyboardCapitalization.None,
    password: Boolean = false,
    textSize: TextUnit = 14.sp,
    bold: Boolean = false,
    @DrawableRes leading: Int? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(15.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) FieldLabel(label)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(shape).recessed(shape)
                .border(if (focused) 1.5.dp else 1.dp, if (focused) Glass.Yellow else Glass.Edge, shape).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (leading != null) Lc(leading, Glass.Muted, 18.dp)
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder.isNotEmpty()) T(placeholder, textSize, Glass.Muted.copy(alpha = 0.7f))
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    interactionSource = interaction,
                    textStyle = TextStyle(fontFamily = Inter, fontSize = textSize, color = Glass.Ink, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal),
                    cursorBrush = SolidColor(Glass.Blue),
                    visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboard, capitalization = caps),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (icon != null) Lc(icon, Glass.Muted, 18.dp, if (onIcon != null) Modifier.clip(CircleShape).clickable(onClick = onIcon) else Modifier)
        }
    }
}

/** Two-line metric tile used on Home. */
@Composable
fun MetricTile(@DrawableRes icon: Int, label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Glass.Ink, onClick: (() -> Unit)? = null) {
    Row(
        modifier.heightIn(min = 58.dp).glass(19.dp, shadow = false).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Lc(icon, Glass.Blue, 18.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            T(label, 10.sp, Glass.Muted)
            T(value, 19.sp, valueColor, FontWeight.Bold)
        }
    }
}

/** Segmented control (Reports). */
@Composable
fun Segments(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val shape = RoundedCornerShape(100.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 45.dp).clip(shape).background(Glass.Field).border(1.dp, Glass.Edge, shape).padding(4.dp),
    ) {
        options.forEach { (key, label) ->
            val on = key == selected
            Box(
                Modifier.weight(1f).heightIn(min = 37.dp)
                    .then(if (on) Modifier.shadow(6.dp, shape, ambientColor = Glass.Shadow.copy(alpha = 0.08f), spotColor = Glass.Shadow.copy(alpha = 0.1f)) else Modifier)
                    .clip(shape).background(if (on) Glass.Card else Color.Transparent).clickable { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) { T(label, 13.sp, if (on) Glass.Ink else Glass.Muted, FontWeight.SemiBold) }
        }
    }
}

/** Six-month sales bars: last bar highlighted blue. */
@Composable
fun BarChart(values: List<Pair<String, Double>>, modifier: Modifier = Modifier, dark: Boolean = false) {
    val max = (values.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    Row(modifier.fillMaxWidth().height(96.dp).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, (label, v) ->
            val on = i == values.lastIndex
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                val barMax = 60f
                val h = (barMax * (v / max).toFloat()).coerceAtLeast(6f)
                Box(Modifier.fillMaxWidth().height(h.dp).clip(RoundedCornerShape(6.dp)).background(if (on) Glass.Yellow else if (dark) Glass.HeroTrack else Glass.Track))
                Spacer(Modifier.height(6.dp))
                T(label, 10.sp, if (on) (if (dark) Glass.Yellow else Glass.Ink) else if (dark) Glass.HeroMuted else Glass.Muted, if (on) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun Donut(parts: List<Pair<Color, Double>>, modifier: Modifier = Modifier, stroke: Dp = 26.dp) {
    val total = parts.sumOf { it.second }.coerceAtLeast(0.0001)
    Canvas(modifier) {
        val sw = stroke.toPx()
        val inset = sw / 2
        if (parts.isEmpty()) {
            drawArc(Glass.Track, 0f, 360f, false, Offset(inset, inset), Size(size.width - sw, size.height - sw), style = Stroke(sw))
            return@Canvas
        }
        var start = -90f
        val gap = if (parts.size > 1) 1.2f else 0f
        parts.forEach { (c, v) ->
            val sweep = (v / total * 360f).toFloat()
            drawArc(c, start, (sweep - gap).coerceAtLeast(0.4f), false, Offset(inset, inset), Size(size.width - sw, size.height - sw), style = Stroke(sw))
            start += sweep
        }
    }
}

@Composable
fun ProgressLine(fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp)).background(Glass.Track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(4.dp).clip(RoundedCornerShape(4.dp)).background(Glass.Blue))
    }
}

// ---------- Bottom sheet ----------

/** Sheet from the design: 93% white, 32dp top corners, grip, big title and a round close button. */
@Composable
fun SheetFrame(
    title: String,
    onClose: () -> Unit,
    spacing: Dp = 10.dp,
    onConfirm: (() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.93f).dp
    val shape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = maxH)
            .shadow(14.dp, shape, ambientColor = Glass.Shadow.copy(alpha = 0.14f), spotColor = Glass.Shadow.copy(alpha = 0.2f))
            .clip(shape)
            .background(Glass.Sheet)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .imePadding()
            .padding(top = 10.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(4.dp)).background(Glass.Muted.copy(alpha = 0.45f)))
        }
        Spacer(Modifier.height(10.dp))
        // iOS sheet toolbar: close (glass) · centred title · confirm (tinted)
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GlassCircle(R.drawable.lc_x, "Close", onClose)
                Spacer(Modifier.weight(1f))
                if (onConfirm != null) Box(
                    Modifier.size(44.dp).pressable(haptic = true, onClick = onConfirm).clip(CircleShape).background(Glass.Yellow),
                    contentAlignment = Alignment.Center,
                ) { Lc(R.drawable.lc_check, Glass.OnAccent, 20.dp, description = "Save") }
            }
            IT(title, 17.sp, Glass.Ink, FontWeight.SemiBold)
        }
        Spacer(Modifier.height(spacing))
        Column(
            Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())
                .then(if (footer == null) Modifier.navigationBarsPadding() else Modifier)
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = if (footer == null) 22.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
        if (footer != null) {
            // Pinned action bar: the Save button is always visible, the form scrolls behind it
            Box(Modifier.fillMaxWidth().height(1.dp).background(Glass.Divider))
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = footer,
            )
        }
    }
}

// ---------- Soft (neumorphic) depth ----------

/** Two soft shadows - a white highlight top-left and a navy shade bottom-right - for a raised, touchable feel. */
fun Modifier.softShadow(radius: Dp, offset: Dp = 6.dp, blur: Dp = 12.dp, darkAlpha: Float = 0.16f, lightAlpha: Float = 0.9f): Modifier =
    this.drawBehind {
        val r = radius.toPx(); val o = offset.toPx(); val b = blur.toPx()
        drawIntoCanvas { canvas ->
            val paint = Paint().asFrameworkPaint().apply {
                isAntiAlias = true
                maskFilter = android.graphics.BlurMaskFilter(b, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
            val sc = Glass.Shadow
            val da = if (ThemeState.palette.darkCard) (darkAlpha * 2.2f).coerceAtMost(0.6f) else darkAlpha
            paint.color = android.graphics.Color.argb((da * 255).toInt(), (sc.red * 255).toInt(), (sc.green * 255).toInt(), (sc.blue * 255).toInt())
            canvas.nativeCanvas.drawRoundRect(o, o, size.width + o, size.height + o, r, r, paint)
            val la = if (ThemeState.palette.darkCard) 0f else lightAlpha
            paint.color = android.graphics.Color.argb((la * 255).toInt(), 255, 255, 255)
            if (la > 0f) canvas.nativeCanvas.drawRoundRect(-o, -o, size.width - o, size.height - o, r, r, paint)
        }
    }

private val Raised get() = Brush.linearGradient(listOf(Glass.Yellow, Glass.Accent2))

// ---------- Date strip (pick a day like a calendar row) ----------

private val stripDay = java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.ENGLISH)

/** Last two weeks as tappable day pills (today on the right) plus a calendar button for older dates. */
@Composable
fun DateStrip(selected: String, onSelect: (String) -> Unit, onPickOlder: () -> Unit) {
    val today = java.time.LocalDate.now()
    val sel = runCatching { java.time.LocalDate.parse(selected.take(10)) }.getOrNull() ?: today
    val days = (13 downTo 0).map { today.minusDays(it.toLong()) }.let { if (sel !in it) listOf(sel) + it else it }
    val scroll = rememberScrollState()
    androidx.compose.runtime.LaunchedEffect(Unit) { scroll.scrollTo(scroll.maxValue) }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(scroll).padding(start = 6.dp, end = 14.dp, top = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val pill = RoundedCornerShape(999.dp)
        Box(
            Modifier.size(width = 52.dp, height = 78.dp).softShadow(26.dp, 3.dp, 10.dp, 0.08f, 0.45f).clip(pill)
                .background(Glass.Card).border(1.dp, Glass.Edge, pill).clickable(onClick = onPickOlder),
            contentAlignment = Alignment.Center,
        ) { Lc(R.drawable.lc_calendar, Glass.Blue, 22.dp, description = "Pick an older date") }
        days.forEach { d ->
            val on = d == sel
            val scale by androidx.compose.animation.core.animateFloatAsState(
                if (on) 1.08f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 400f), label = "day",
            )
            Column(
                Modifier.size(width = 52.dp, height = 78.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                    .softShadow(26.dp, if (on) 5.dp else 3.dp, 10.dp, if (on) 0.22f else 0.08f, 0.45f)
                    .clip(pill)
                    .background(if (on) Raised else Brush.verticalGradient(listOf(Glass.Card, Glass.Card.copy(alpha = 0.85f))))
                    .border(1.dp, if (on) Glass.Yellow else Glass.Edge, pill)
                    .clickable { onSelect(d.toString()) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            ) {
                T(d.dayOfMonth.toString(), 18.sp, if (on) Glass.OnAccent else Glass.Ink, FontWeight.Bold)
                T(if (d == today) "Today" else d.format(stripDay), 10.sp, if (on) Glass.OnAccent else Glass.Muted, if (on) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

// ---------- Product bento grid ----------

/** Products as a 2-column bento grid: round icon badge top-left, label below, check badge when selected. */
@Composable
fun ProductGrid(options: List<String>, selected: String, onSelect: (String) -> Unit, extra: (@Composable (Modifier) -> Unit)? = null) {
    // iOS-style product tiles, four per row: icon over label; the chosen one is filled with the theme tint
    val cells: List<String?> = options + (if (extra != null) listOf(null) else emptyList())
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cells.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { o ->
                    val m = Modifier.weight(1f).heightIn(min = 76.dp)
                    if (o == null) extra?.invoke(m) else {
                        val on = o == selected
                        val shape = RoundedCornerShape(20.dp)
                        Column(
                            m.pressable(scaleTo = 0.95f) { onSelect(o) }.iosCard(20.dp, if (on) Glass.Yellow else Glass.Card)
                                .semantics { this.selected = on }.padding(horizontal = 4.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                        ) {
                            Lc(productIcon(o), if (on) Glass.OnAccent else Glass.Ink, 22.dp)
                            T(sentence(o), 12.sp, if (on) Glass.OnAccent else Glass.Ink, if (on) FontWeight.SemiBold else FontWeight.Medium, maxLines = 2, align = TextAlign.Center)
                        }
                    }
                }
                repeat(4 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** Pressed-in (recessed) surface: darker top edge fading to the fill, like a soft inner shadow. */
fun Modifier.recessed(shape: androidx.compose.ui.graphics.Shape, fill: Color = Color.Unspecified): Modifier {
    val dark = ThemeState.palette.darkCard
    val f = if (fill == Color.Unspecified) ThemeState.palette.field else fill
    return this.background(f, shape).drawWithContent {
        drawContent()
        drawRect(Brush.verticalGradient(listOf(if (dark) Color(0x40000000) else Color(0x24302A20), Color.Transparent), endY = 14.dp.toPx()))
        drawRect(Brush.horizontalGradient(listOf(if (dark) Color(0x26000000) else Color(0x14302A20), Color.Transparent), endX = 10.dp.toPx()))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, if (dark) Color(0x0FFFFFFF) else Color(0x66FFFFFF)), startY = size.height - 8.dp.toPx()))
    }
}

/** Main save button: charcoal with a mustard check badge and soft depth. */
@Composable
fun SubmitButton(text: String, @DrawableRes icon: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(100.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).pressable(onClick = onClick, haptic = true)
            .softShadow(100.dp, 6.dp, 16.dp, 0.28f, 0.8f).clip(shape)
            .background(Glass.Charcoal).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            val same = Glass.Charcoal == Glass.Yellow
            Box(Modifier.size(28.dp).clip(CircleShape).background(if (same) Glass.OnPrimary else Glass.Yellow), contentAlignment = Alignment.Center) {
                Lc(icon, if (same) Glass.Yellow else Glass.OnAccent, 17.dp)
            }
            Spacer(Modifier.width(10.dp))
        }
        T(text, 17.sp, Glass.OnPrimary, FontWeight.Bold)
    }
}

/** Folder-tab segmented control with coloured status dots. */
@Composable
fun StatusTabs(options: List<Triple<String, String, Color>>, selected: String, onSelect: (String) -> Unit) {
    val track = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(track).recessed(track).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (key, label, dot) ->
            val on = key == selected
            val tab = RoundedCornerShape(14.dp)
            Row(
                Modifier.weight(1f).heightIn(min = 48.dp)
                    .then(if (on) Modifier.softShadow(14.dp, 3.dp, 8.dp, 0.14f, 0.6f) else Modifier)
                    .clip(tab).background(if (on) Glass.Card else Color.Transparent).clickable { onSelect(key) }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                Spacer(Modifier.width(6.dp))
                T(label, 12.sp, if (on) Glass.Ink else Glass.Muted, if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 2)
            }
        }
    }
}

/** Payment-mode icon cards, four per row. */
@Composable
fun ModeCards(options: List<Pair<String, Int>>, selected: String, onSelect: (String) -> Unit, label: (String) -> String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (key, icon) ->
                    val on = key == selected
                    val shape = RoundedCornerShape(16.dp)
                    Column(
                        Modifier.weight(1f).heightIn(min = 78.dp)
                            .then(if (on) Modifier else Modifier.softShadow(16.dp, 4.dp, 10.dp, 0.10f, 0.9f))
                            .clip(shape)
                            .then(if (on) Modifier.recessed(shape, Glass.YellowSoft) else Modifier.background(Glass.Card))
                            .border(if (on) 2.dp else 1.dp, if (on) Glass.Charcoal else Glass.Edge, shape)
                            .clickable { onSelect(key) }.padding(horizontal = 4.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                    ) {
                        Box(Modifier.size(30.dp).clip(CircleShape).background(if (on) Glass.Yellow else Glass.TileBg), contentAlignment = Alignment.Center) {
                            Lc(icon, if (on) Glass.OnAccent else Glass.Ink, 16.dp)
                        }
                        T(label(key), 10.sp, Glass.Ink, if (on) FontWeight.Bold else FontWeight.Medium, maxLines = 2, align = TextAlign.Center)
                    }
                }
                repeat(4 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** Chip with a dashed outline (used for "+ Add"). */
@Composable
fun DashedChip(text: String, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(100.dp)).clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    Glass.Muted, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                    style = Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                )
            }.padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) { T(text, 13.sp, Glass.Ink, FontWeight.SemiBold) }
}

/** Interlocking folder tabs sitting on a white card (Paid / Unpaid / Cash on Home). */
@Composable
fun FolderTabs(tabs: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val idx = tabs.indexOfFirst { it.first == selected }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            tabs.forEach { (key, label) ->
                val on = key == selected
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                        .background(if (on) Glass.Card else Glass.Card.copy(alpha = 0.55f))
                        .clickable { onSelect(key) }.heightIn(min = 48.dp).padding(vertical = if (on) 14.dp else 11.dp),
                    contentAlignment = Alignment.Center,
                ) { T(label, if (on) 14.sp else 12.sp, if (on) Glass.Ink else Glass.Muted, if (on) FontWeight.Bold else FontWeight.Medium) }
            }
        }
        val shape = RoundedCornerShape(
            topStart = if (idx == 0) 0.dp else 24.dp, topEnd = if (idx == tabs.lastIndex) 0.dp else 24.dp,
            bottomStart = 24.dp, bottomEnd = 24.dp,
        )
        Column(
            Modifier.fillMaxWidth().softShadow(24.dp, 5.dp, 14.dp, 0.10f, 0.5f).clip(shape).background(Glass.Card).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

// ---------- Motion (Animations & Motion Graphics guide) ----------

/** Press compression: scales to 96% while held (spring, ~180 ms), with optional haptic tick. Reduce Motion skips the scale. */
fun Modifier.pressable(enabled: Boolean = true, haptic: Boolean = false, scaleTo: Float = 0.96f, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val reduce = ThemeState.reduceMotion
    val scale by androidx.compose.animation.core.animateFloatAsState(
        if (pressed && !reduce) scaleTo else 1f,
        androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 900f), label = "press",
    )
    val hf = androidx.compose.ui.platform.LocalHapticFeedback.current
    this.graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, enabled = enabled) {
            if (haptic) hf.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onClick()
        }
}

/** Staggered list entrance: fade + 12dp rise, 60 ms apart (capped), ease-out. Immediate with Reduce Motion. */
fun Modifier.enter(index: Int): Modifier = composed {
    val reduce = ThemeState.reduceMotion
    val p = remember { androidx.compose.animation.core.Animatable(if (reduce) 1f else 0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!reduce) {
            kotlinx.coroutines.delay(60L * index.coerceAtMost(6))
            p.animateTo(1f, androidx.compose.animation.core.tween(320, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        }
    }
    val rise = with(androidx.compose.ui.platform.LocalDensity.current) { 12.dp.toPx() }
    this.graphicsLayer { alpha = p.value; translationY = (1f - p.value) * rise }
}

// ---------- Accent page header (matches Home) ----------

/** Lets a header inside a padded list reach the screen edges. */
fun Modifier.bleed(horizontal: Dp): Modifier = this.layout { m, c ->
    val extra = (horizontal * 2).roundToPx()
    val max = if (c.hasBoundedWidth) c.maxWidth + extra else c.maxWidth
    val p = m.measure(c.copy(minWidth = max, maxWidth = max))
    layout(p.width - extra, p.height) { p.place(-extra / 2, 0) }
}

/** Yellow (accent) header with rounded bottom corners: big title, subtitle, round buttons, then optional content (e.g. a white summary card). */
@Composable
fun AccentHeader(
    title: String,
    subtitle: String,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val pal = ThemeState.palette
    Column(
        Modifier.fillMaxWidth().bleed(22.dp)
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)).background(pal.accent)
            .statusBarsPadding().padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = if (content != null) 22.dp else 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                T(subtitle, 15.sp, pal.onAccentSub, FontWeight.Medium)
                T(title, 32.sp, pal.onAccent, FontWeight.Bold)
            }
            actions()
        }
        if (content != null) content()
    }
}

/** 44dp round button on the accent header. dark = filled with the ink colour (like the profile badge on Home). */
@Composable
fun HeaderButton(@DrawableRes icon: Int, description: String, dark: Boolean = false, onClick: () -> Unit) {
    val pal = ThemeState.palette
    Box(
        Modifier.size(44.dp).pressable(onClick = onClick).clip(CircleShape)
            .background(if (dark) pal.onAccent else Color.White.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) { Lc(icon, if (dark) pal.accent else pal.onAccent, 20.dp, description = description) }
}

/** Header button that opens the month picker. */
@Composable
fun HeaderMonthButton(state: AppState) {
    var open by remember { androidx.compose.runtime.mutableStateOf(false) }
    Box {
        HeaderButton(R.drawable.lc_calendar, "Choose month") { open = true }
        MonthMenu(state, open) { open = false }
    }
}

/** White summary card used inside the accent header. */
@Composable
fun HeaderCard(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val pal = ThemeState.palette
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = pal.shadow.copy(alpha = 0.10f), spotColor = pal.shadow.copy(alpha = 0.16f))
            .clip(shape).background(pal.card)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}
