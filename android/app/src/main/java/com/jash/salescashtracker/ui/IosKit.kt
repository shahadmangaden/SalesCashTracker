package com.jash.salescashtracker.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jash.salescashtracker.R
import com.jash.salescashtracker.ui.theme.Inter
import kotlin.math.PI
import kotlin.math.sin

/*
 * iOS 26-style building blocks (large titles, Liquid Glass buttons and tab bar,
 * inset grouped lists, segmented control) plus the moving travel scenes that sit
 * behind every screen's title. Built to match the Figma "iOS redesign" page.
 */

// ---------- Text ----------

/** Text with iOS-like tracking: tighter for big titles, neutral for body. */
@Composable
fun IT(
    text: String, size: TextUnit, color: Color = Glass.Ink, weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier, maxLines: Int = 1, tracking: TextUnit? = null,
) = Text(
    text, modifier = modifier, color = color, fontSize = size, fontWeight = weight, fontFamily = Inter,
    letterSpacing = tracking ?: if (size.value >= 28) (-0.6).sp else if (size.value >= 17) (-0.2).sp else 0.sp,
    maxLines = maxLines, overflow = TextOverflow.Ellipsis,
)

/** Small spaced capitals ("TUESDAY, 20 OCTOBER", "APPEARANCE"). */
@Composable
fun Eyebrow(text: String, color: Color = Glass.Muted, modifier: Modifier = Modifier) =
    IT(text.uppercase(java.util.Locale.ENGLISH), 13.sp, color, FontWeight.SemiBold, modifier, tracking = 1.4.sp)

// ---------- Surfaces ----------

private val Soft = 16.dp

/** White card with a very soft shadow (iOS grouped style). */
fun Modifier.iosCard(radius: Dp = 26.dp, fill: Color? = null): Modifier = this
    .shadow(10.dp, RoundedCornerShape(radius), ambientColor = Glass.Shadow.copy(alpha = 0.05f), spotColor = Glass.Shadow.copy(alpha = 0.08f))
    .clip(RoundedCornerShape(radius))
    .background(fill ?: Glass.Card)

/** "Liquid Glass": translucent white with a bright edge and floating shadow. */
fun Modifier.liquidGlass(shape: androidx.compose.ui.graphics.Shape): Modifier = this
    .shadow(14.dp, shape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.16f))
    .clip(shape)
    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.78f))))
    .border(0.75.dp, Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.35f))), shape)

/** Inset grouped list container. */
@Composable
fun InsetGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.fillMaxWidth().iosCard(), content = content)

/** One row of an inset grouped list: coloured icon square, title + subtitle, trailing value + status, chevron. */
@Composable
fun IosRow(
    @DrawableRes icon: Int, title: String, subtitle: String = "",
    value: String = "", valueColor: Color = Glass.Ink,
    status: String = "", statusColor: Color = Glass.Muted,
    last: Boolean = false, iconBg: Color = Glass.Yellow, chevron: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 62.dp).padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(iconBg), contentAlignment = Alignment.Center) {
            Lc(icon, Glass.OnAccent, 19.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    IT(title.ifBlank { "—" }, 17.sp, Glass.Ink)
                    if (subtitle.isNotBlank()) IT(subtitle, 13.sp, Glass.Muted)
                }
                if (value.isNotBlank() || status.isNotBlank()) Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (value.isNotBlank()) IT(value, 17.sp, valueColor, FontWeight.SemiBold)
                    if (status.isNotBlank()) IT(status, 13.sp, statusColor, FontWeight.Medium)
                }
                if (chevron && onClick != null) { Spacer(Modifier.width(8.dp)); Lc(R.drawable.lc_chevron_right, Glass.Divider, 14.dp) }
            }
            if (!last) Box(Modifier.fillMaxWidth().height(0.5.dp).background(Glass.Divider))
        }
    }
}

/** Section title ("Recent" · "See All"). */
@Composable
fun IosSection(title: String, trailing: String? = null, trailingIsAction: Boolean = false, onTrailing: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IT(title, 20.sp, Glass.Ink, FontWeight.Bold, Modifier.weight(1f))
        if (trailing != null) IT(trailing, if (trailingIsAction) 17.sp else 15.sp, if (trailingIsAction) Glass.Yellow else Glass.Muted,
            if (trailingIsAction) FontWeight.Normal else FontWeight.SemiBold,
            if (onTrailing != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onTrailing).padding(4.dp) else Modifier)
    }
}

/** Small stat tile with a coloured dot. */
@Composable
fun StatTile(label: String, value: String, dot: Color, modifier: Modifier = Modifier, valueColor: Color = Glass.Ink, onClick: (() -> Unit)? = null) {
    Column(
        modifier.iosCard(20.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot)); IT(label, 12.sp, Glass.Muted, FontWeight.SemiBold)
        }
        IT(value, 18.sp, valueColor, FontWeight.Bold)
    }
}

// ---------- Controls ----------

/** 44dp Liquid Glass circle button. */
@Composable
fun GlassCircle(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).pressable(onClick = onClick).liquidGlass(CircleShape).semantics { contentDescription = description; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Lc(icon, Glass.Ink, 20.dp) }
}

/** Tinted circle with initials: opens Settings. */
@Composable
fun AvatarButton(name: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).pressable(onClick = onClick).shadow(8.dp, CircleShape, spotColor = Glass.Yellow.copy(alpha = 0.3f)).clip(CircleShape).background(Glass.Yellow)
            .semantics { contentDescription = "Settings"; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { IT(initials(name.ifBlank { "Me" }), 15.sp, Glass.OnAccent, FontWeight.SemiBold) }
}

/** Large title header: eyebrow line, 34sp bold title, trailing glass buttons. */
@Composable
fun LargeTitle(title: String, eyebrow: String? = null, onTitleClick: (() -> Unit)? = null, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f).then(if (onTitleClick != null) Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onTitleClick) else Modifier)) {
            if (eyebrow != null) Eyebrow(eyebrow)
            IT(title, 34.sp, Glass.Ink, FontWeight.Bold)
        }
        actions()
    }
}

/** iOS segmented control (capsule track, white thumb). */
@Composable
fun IosSegmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(CircleShape).background(Glass.TileBg).padding(3.dp)) {
        options.forEach { (key, label) ->
            val on = key == selected
            val bg by animateColorAsState(if (on) Glass.Card else Color.Transparent, tween(200), label = "seg")
            Box(
                Modifier.weight(1f).heightIn(min = 36.dp)
                    .then(if (on) Modifier.shadow(4.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.1f)) else Modifier)
                    .clip(CircleShape).background(bg).clickable { onSelect(key) }.semantics { this.selected = on },
                contentAlignment = Alignment.Center,
            ) { IT(label, 14.sp, Glass.Ink, if (on) FontWeight.SemiBold else FontWeight.Medium) }
        }
    }
}

/** Capsule search field. */
@Composable
fun IosSearch(value: String, onChange: (String) -> Unit, placeholder: String) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(CircleShape).background(Glass.TileBg).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Lc(R.drawable.lc_search, Glass.Muted, 17.dp)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) IT(placeholder, 17.sp, Glass.Muted)
            BasicTextField(value, onChange, singleLine = true, modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontFamily = Inter, fontSize = 17.sp, color = Glass.Ink), cursorBrush = SolidColor(Glass.Yellow))
        }
        if (value.isNotEmpty()) Lc(R.drawable.lc_x, Glass.Muted, 16.dp, Modifier.clip(CircleShape).clickable { onChange("") }, "Clear")
    }
}

/** Floating Liquid Glass tab bar (4 tabs) with a separate tinted Add button. */
@Composable
fun IosTabBar(state: AppState, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 10.dp).height(62.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.weight(1f).fillMaxHeight().liquidGlass(RoundedCornerShape(31.dp)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Tab.entries.forEach { t ->
                val on = state.tab == t
                val bg by animateColorAsState(if (on) Glass.TileBg else Color.Transparent, tween(200), label = "tabbg")
                val fg by animateColorAsState(if (on) Glass.Yellow else Glass.Ink, tween(200), label = "tabfg")
                Column(
                    Modifier.weight(1f).fillMaxHeight().pressable(scaleTo = 0.92f) { state.tab = t }.semantics { this.selected = on }
                        .clip(RoundedCornerShape(27.dp)).background(bg),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                ) {
                    Lc(t.icon, fg, 22.dp)
                    IT(t.label, 10.sp, fg, if (on) FontWeight.SemiBold else FontWeight.Medium)
                }
            }
        }
        Box(
            Modifier.size(62.dp).pressable(haptic = true, scaleTo = 0.9f) { state.push(Route.QuickAdd) }
                .shadow(14.dp, CircleShape, spotColor = Glass.Yellow.copy(alpha = 0.4f)).clip(CircleShape).background(Glass.Yellow)
                .semantics { contentDescription = "Add sale or payment"; role = Role.Button },
            contentAlignment = Alignment.Center,
        ) { Lc(R.drawable.lc_plus, Glass.OnAccent, 26.dp) }
    }
}

// ---------- Activity-style rings ----------

/** Three concentric progress rings (collected / paid / due) that sweep in. */
@Composable
fun ActivityRings(fractions: List<Float>, colors: List<Color>, size: Dp = 104.dp, width: Dp = 11.dp) {
    val anim = fractions.map { remember { Animatable(0f) } }
    fractions.forEachIndexed { i, f ->
        LaunchedEffect(f) {
            if (ThemeState.reduceMotion) anim[i].snapTo(f.coerceIn(0f, 1f))
            else { kotlinx.coroutines.delay(150L + i * 100L); anim[i].animateTo(f.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing)) }
        }
    }
    Canvas(Modifier.size(size)) {
        val sw = width.toPx(); val gap = sw * 1.15f
        fractions.indices.forEach { i ->
            val inset = sw / 2 + i * (sw + gap - sw * 0.15f)
            val tl = Offset(inset, inset); val sz = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            if (sz.width <= 0f) return@forEach
            drawArc(colors[i].copy(alpha = 0.22f), 0f, 360f, false, tl, sz, style = Stroke(sw))
            if (anim[i].value > 0.001f) drawArc(colors[i], -90f, 360f * anim[i].value, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
        }
    }
}

// ---------- Moving travel scenes ----------

enum class Scene { Home, Sales, Cash, Reports, Settings, Sheet }

/**
 * Soft travel illustration behind a screen title: a plane flies a dashed route,
 * clouds drift, pins pulse, coins rise, a globe turns, a compass swings.
 * 6-second loop; frozen (static) when the phone's "Remove animations" is on.
 */
@Composable
fun TravelScene(scene: Scene, modifier: Modifier = Modifier, height: Dp = 280.dp) {
    val tint = Glass.Yellow
    val reduce = ThemeState.reduceMotion
    val inf = rememberInfiniteTransition(label = "scene")
    val anim by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart), label = "p")
    val p = if (reduce) 0.4f else anim
    val wave = sin(p * 2 * PI).toFloat()   // -1..1, for bobbing / swinging
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxWidth().height(height)) {
        val w = 393f
        val k = maxWidth.value / w
        fun o(x: Float, y: Float): Modifier = Modifier.offset((x * k).dp, y.dp)
        Canvas(Modifier.matchParentSize()) {
            val sx = size.width / w; val sy = size.height / 280f
            fun cloud(cx: Float, cy: Float, r: Float, a: Float) {
                val c = tint.copy(alpha = a)
                drawCircle(c, r * sx, Offset(cx * sx, cy * sy)); drawCircle(c, r * 0.75f * sx, Offset((cx - r * 0.9f) * sx, (cy + r * 0.25f) * sy))
                drawCircle(c, r * 0.8f * sx, Offset((cx + r * 0.95f) * sx, (cy + r * 0.2f) * sy))
            }
            val dash = PathEffect.dashPathEffect(floatArrayOf(3f * sx, 14f * sx), 0f)
            when (scene) {
                Scene.Home, Scene.Sales, Scene.Sheet -> {
                    val path = Path().apply { moveTo(-10f * sx, 230f * sy); cubicTo(110f * sx, 60f * sy, 280f * sx, 50f * sy, 410f * sx, 150f * sy) }
                    drawPath(path, tint.copy(alpha = 0.32f), style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash))
                    if (scene != Scene.Sheet) { cloud(240f + 40f * p, 95f, 22f, 0.07f); cloud(70f - 30f * p, 150f, 15f, 0.06f) }
                }
                Scene.Cash -> {
                    val path = Path().apply { moveTo(-10f * sx, 200f * sy); cubicTo(100f * sx, 120f * sy, 230f * sx, 180f * sy, 410f * sx, 80f * sy) }
                    drawPath(path, tint.copy(alpha = 0.25f), style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash))
                }
                Scene.Reports -> {
                    val path = Path().apply { moveTo(160f * sx, 170f * sy); cubicTo(210f * sx, 60f * sy, 300f * sx, 60f * sy, 350f * sx, 110f * sy) }
                    drawPath(path, tint.copy(alpha = 0.3f), style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash))
                }
                Scene.Settings -> cloud(150f + 50f * p, 70f, 20f, 0.06f)
            }
        }
        // Positioned icons (offsets are in the 393 x 280 design space, scaled to the screen width)
        when (scene) {
            Scene.Home, Scene.Sales, Scene.Sheet -> {
                // point on the cubic route
                val t = p
                fun bez(a: Float, b: Float, c: Float, d: Float) = (1 - t) * (1 - t) * (1 - t) * a + 3 * (1 - t) * (1 - t) * t * b + 3 * (1 - t) * t * t * c + t * t * t * d
                val x = bez(-10f, 110f, 280f, 410f); val y = bez(230f, 60f, 50f, 150f)
                val angle = -25f + 50f * t
                Lc(R.drawable.lc_plane, tint.copy(alpha = 0.55f), 24.dp, o(x - 12f, y - 12f).graphicsLayer { rotationZ = angle + 45f })
                if (scene == Scene.Home) {
                    val pulse = 1f + 0.35f * maxOf(0f, sin(p * 2 * PI * 2).toFloat())
                    Lc(R.drawable.lc_map_pin, tint.copy(alpha = 0.45f), 16.dp, o(355f, 118f).graphicsLayer { scaleX = pulse; scaleY = pulse })
                }
                if (scene == Scene.Sales) {
                    Lc(R.drawable.lc_ticket, tint.copy(alpha = 0.10f), 44.dp, o(260f, 60f - 12f * wave).graphicsLayer { rotationZ = 14f + 6f * wave })
                    Lc(R.drawable.lc_ticket, tint.copy(alpha = 0.12f), 30.dp, o(190f, 110f + 8f * wave).graphicsLayer { rotationZ = -10f })
                }
            }
            Scene.Cash -> {
                Lc(R.drawable.lc_wallet, tint.copy(alpha = 0.07f), 80.dp, o(250f, 40f + 8f * wave).graphicsLayer { rotationZ = -8f + 5f * wave })
                listOf(Triple(60f, 210f, 30), Triple(130f, 240f, 22), Triple(300f, 180f, 34), Triple(240f, 250f, 20)).forEachIndexed { i, (cx, cy, sz) ->
                    val q = (p + i * 0.22f) % 1f
                    val a = if (q < 0.1f) q * 10f else if (q > 0.85f) (1f - q) / 0.15f else 1f
                    Lc(R.drawable.lc_coins, tint.copy(alpha = 0.18f * a), sz.dp, o(cx, cy - 140f * q).graphicsLayer { rotationZ = (if (i % 2 == 0) -40f else 40f) * q })
                }
            }
            Scene.Reports -> {
                Lc(R.drawable.lc_globe, tint.copy(alpha = 0.07f), 220.dp, o(190f, -40f).graphicsLayer { rotationZ = -45f * p })
                listOf(225f to 40f, 320f to 92f, 262f to 140f).forEachIndexed { i, (px, py) ->
                    val ph = ((p * 3f) - i).let { if (it in 0f..1f) sin(it * PI).toFloat() else 0f }
                    val sc = 1f + 0.4f * ph
                    Lc(R.drawable.lc_map_pin, tint.copy(alpha = 0.4f), 18.dp, o(px, py).graphicsLayer { scaleX = sc; scaleY = sc })
                }
            }
            Scene.Settings -> {
                Lc(R.drawable.lc_compass, tint.copy(alpha = 0.07f), 170.dp, o(230f, -10f).graphicsLayer { rotationZ = 20f * wave })
                Lc(R.drawable.lc_luggage, tint.copy(alpha = 0.12f), 46.dp, o(30f, 160f - 8f * kotlin.math.abs(wave)).graphicsLayer { rotationZ = 8f })
            }
        }
    }
}

/** Page scaffold for a tab: grouped background with the travel scene pinned behind the content. */
@Composable
fun IosPage(scene: Scene, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxWidth().fillMaxHeight().background(ThemeState.palette.bg.first())) {
        TravelScene(scene)
        content()
    }
}
