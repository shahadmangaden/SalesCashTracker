package com.jash.salescashtracker.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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

// ---------- Design tokens (from the Figma file) ----------

object Glass {
    val Ink = Color(0xFF16243D)
    val Muted = Color(0xFF64728B)
    val Blue = Color(0xFF246BFD)
    val Tint = Blue
    val BlueBg = Color(0xFFE6EEFF)
    val Green = Color(0xFF167E65)
    val GreenBg = Color(0xFFE1F4EC)
    val Amber = Color(0xFFA26622)
    val AmberBg = Color(0xFFFFF0D9)
    val Red = Color(0xFFBD5260)
    val RedBg = Color(0xFFFFE9ED)
    val NeutralBg = Color(0xFFE8EDF5)
    val TileBg = Color(0xFFEDF3FF)
    val Divider = Color(0xFFD9E2EF)
    val Track = Color(0xFFDFE7F4)
    val CloseBg = Color(0xFFE8EEF8)
    val Fill = Color.White.copy(alpha = 0.66f)
    val Edge = Color.White.copy(alpha = 0.9f)
    val Shadow = Color(0xFF526F9D)
}

val DonutColors = listOf(
    Color(0xFF246BFD), Color(0xFFA299E9), Color(0xFF7FC9B5), Color(0xFF89B9FA),
    Color(0xFFF2BD95), Color(0xFFB3BFD3), Color(0xFF5AC8B0), Color(0xFFD3A6E8),
)

val AvatarColors = listOf(Color(0xFFDCE8FF), Color(0xFFE9E2FB), Color(0xFFDFF2EA), Color(0xFFFFF0D9))

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
    .replace("ksa", "KSA").replace("uae", "UAE")

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

/** Pastel gradient backdrop with the soft glow from the design. */
/** Colourful soft backdrop the glass sits on (the look from the first Liquid Glass version). */
@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFFEFF3FB))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            fun blob(c: Color, x: Float, y: Float, r: Float) =
                drawCircle(Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center = Offset(x, y), radius = r), r, Offset(x, y))
            blob(Color(0xFF8EC5FF).copy(alpha = 0.75f), w * 0.05f, h * 0.06f, w * 0.85f)
            blob(Color(0xFFC6B6FF).copy(alpha = 0.6f), w * 1.0f, h * 0.22f, w * 0.75f)
            blob(Color(0xFF9FF0DA).copy(alpha = 0.45f), w * 0.15f, h * 0.62f, w * 0.8f)
            blob(Color(0xFFFFD1E3).copy(alpha = 0.45f), w * 0.95f, h * 0.85f, w * 0.8f)
        }
        content()
    }
}

/** Frosted glass: white gradient fill, bright fading edge, soft floating shadow. */
fun Modifier.glass(radius: Dp = 24.dp, alpha: Float = 0.66f, shadow: Boolean = true): Modifier {
    val shape = RoundedCornerShape(radius)
    val top = (alpha + 0.14f).coerceAtMost(0.95f)
    val bottom = (alpha - 0.08f).coerceAtLeast(0.3f)
    return this
        // Soft raised depth (white highlight + navy shade) from the reference designs
        .softShadow(radius, if (shadow) 5.dp else 3.dp, if (shadow) 14.dp else 8.dp, if (shadow) 0.10f else 0.06f, if (shadow) 0.65f else 0.45f)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = top), Color.White.copy(alpha = bottom))))
        .border(1.dp, Brush.verticalGradient(listOf(Glass.Edge, Color.White.copy(alpha = 0.25f))), shape)
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
        T(initials(name), fontSize, Glass.Blue, FontWeight.Bold)
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
            T(title, 32.sp, Glass.Ink, FontWeight.Bold)
            T(subtitle, 12.sp, Glass.Muted)
        }
        if (action != null) HeadingAction(action, onAction, actionDescription)
    }
}

@Composable
fun SectionHeading(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        T(title, 18.sp, Glass.Ink, FontWeight.Bold, Modifier.weight(1f))
        if (action != null) T(action, 12.sp, Glass.Blue, FontWeight.SemiBold,
            if (onAction != null) Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onAction).padding(2.dp) else Modifier)
    }
}

/** Selectable pill ("Choice" in the design). */
@Composable
fun Choice(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, large: Boolean = false) {
    val shape = RoundedCornerShape(100.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) Glass.Blue else Color.White.copy(alpha = 0.61f))
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.53f) else Glass.Edge, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = if (large) 13.dp else 10.dp, vertical = if (large) 9.dp else 7.dp),
    ) {
        T(text, if (large) 13.sp else 12.sp, if (selected) Color.White else Glass.Muted, if (selected) FontWeight.SemiBold else FontWeight.Medium)
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
        "PARTIAL" -> StatusPill("Partial", Glass.Blue, Glass.BlueBg)
        "PENDING" -> StatusPill("Pending", Glass.Muted, Glass.NeutralBg)
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
            .shadow(12.dp, shape, ambientColor = Color(0xFF3B609D).copy(alpha = 0.14f), spotColor = Color(0xFF3B609D).copy(alpha = 0.3f))
            .clip(shape)
            .background(Glass.Blue)
            .clickable(enabled = !busy, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        else {
            if (icon != null) { Lc(icon, Color.White, 20.dp); Spacer(Modifier.width(9.dp)) }
            T(text, 16.sp, Color.White, FontWeight.Bold)
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
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(15.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) FieldLabel(label)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(shape).background(Color.White.copy(alpha = 0.86f))
                .border(1.dp, if (focused) Glass.Blue.copy(alpha = 0.55f) else Glass.Edge, shape).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
        Modifier.fillMaxWidth().heightIn(min = 45.dp).clip(shape).background(Color.White.copy(alpha = 0.5f)).border(1.dp, Glass.Edge, shape).padding(4.dp),
    ) {
        options.forEach { (key, label) ->
            val on = key == selected
            Box(
                Modifier.weight(1f).heightIn(min = 37.dp)
                    .then(if (on) Modifier.shadow(6.dp, shape, ambientColor = Glass.Shadow.copy(alpha = 0.08f), spotColor = Glass.Shadow.copy(alpha = 0.1f)) else Modifier)
                    .clip(shape).background(if (on) Color.White else Color.Transparent).clickable { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) { T(label, 13.sp, if (on) Glass.Blue else Glass.Muted, FontWeight.SemiBold) }
        }
    }
}

/** Six-month sales bars: last bar highlighted blue. */
@Composable
fun BarChart(values: List<Pair<String, Double>>, modifier: Modifier = Modifier) {
    val max = (values.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    Row(modifier.fillMaxWidth().height(96.dp).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, (label, v) ->
            val on = i == values.lastIndex
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                val barMax = 60f
                val h = (barMax * (v / max).toFloat()).coerceAtLeast(6f)
                Box(Modifier.fillMaxWidth().height(h.dp).clip(RoundedCornerShape(6.dp)).background(if (on) Glass.Blue else Color(0xA69BB9F4)))
                Spacer(Modifier.height(6.dp))
                T(label, 10.sp, if (on) Glass.Blue else Glass.Muted, if (on) FontWeight.Bold else FontWeight.Normal)
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
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.9f).dp
    val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = maxH)
            .shadow(14.dp, shape, ambientColor = Color(0xFF3B609D).copy(alpha = 0.14f), spotColor = Color(0xFF3B609D).copy(alpha = 0.2f))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFF7F9FD), Color(0xFFEEF2F9))))
            .border(1.dp, Glass.Edge, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .imePadding()
            .padding(start = 22.dp, end = 22.dp, top = 10.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFA4B1C7)))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            T(title, 26.sp, Glass.Ink, FontWeight.Bold, Modifier.weight(1f))
            Box(Modifier.size(30.dp).clip(CircleShape).background(Glass.CloseBg).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Lc(R.drawable.lc_x, Glass.Ink, 16.dp, description = "Close")
            }
        }
        Spacer(Modifier.height(spacing))
        Column(
            Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())
                .then(if (footer == null) Modifier.navigationBarsPadding() else Modifier).padding(bottom = if (footer == null) 22.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
        if (footer != null) {
            // Pinned action bar: the Save button is always visible, the form scrolls behind it
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 6.dp, bottom = 14.dp),
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
            paint.color = android.graphics.Color.argb((darkAlpha * 255).toInt(), 0x0D, 0x27, 0x50)
            canvas.nativeCanvas.drawRoundRect(o, o, size.width + o, size.height + o, r, r, paint)
            paint.color = android.graphics.Color.argb((lightAlpha * 255).toInt(), 255, 255, 255)
            canvas.nativeCanvas.drawRoundRect(-o, -o, size.width - o, size.height - o, r, r, paint)
        }
    }

private val Raised = Brush.linearGradient(listOf(Color(0xFF3D9BFF), Color(0xFF0A62FF)))

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
        Modifier.fillMaxWidth().horizontalScroll(scroll).padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val pill = RoundedCornerShape(26.dp)
        Box(
            Modifier.size(width = 52.dp, height = 68.dp).softShadow(26.dp, 4.dp, 10.dp, 0.12f).clip(pill)
                .background(Color.White.copy(alpha = 0.9f)).border(1.dp, Glass.Edge, pill).clickable(onClick = onPickOlder),
            contentAlignment = Alignment.Center,
        ) { Lc(R.drawable.lc_calendar, Glass.Blue, 22.dp, description = "Pick an older date") }
        days.forEach { d ->
            val on = d == sel
            Column(
                Modifier.size(width = 52.dp, height = if (on) 76.dp else 68.dp)
                    .softShadow(26.dp, if (on) 6.dp else 4.dp, 12.dp, if (on) 0.22f else 0.10f)
                    .clip(pill)
                    .background(if (on) Raised else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.75f))))
                    .border(1.dp, if (on) Color.White.copy(alpha = 0.5f) else Glass.Edge, pill)
                    .clickable { onSelect(d.toString()) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            ) {
                T(d.dayOfMonth.toString(), 20.sp, if (on) Color.White else Glass.Ink, FontWeight.Bold)
                T(if (d == today) "Today" else d.format(stripDay), 10.sp, if (on) Color.White.copy(alpha = 0.9f) else Glass.Muted, FontWeight.Medium)
            }
        }
    }
}

// ---------- Product tiles ----------

/** Products as raised icon tiles, three per row. */
@Composable
fun ProductGrid(options: List<String>, selected: String, onSelect: (String) -> Unit, extra: (@Composable (Modifier) -> Unit)? = null) {
    val cells: List<String?> = options + (if (extra != null) listOf(null) else emptyList())
    Column(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cells.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { o ->
                    val m = Modifier.weight(1f).heightIn(min = 84.dp)
                    if (o == null) extra?.invoke(m) else {
                        val on = o == selected
                        val shape = RoundedCornerShape(20.dp)
                        Column(
                            m.softShadow(20.dp, if (on) 6.dp else 4.dp, 12.dp, if (on) 0.22f else 0.10f).clip(shape)
                                .background(if (on) Raised else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.72f))))
                                .border(1.dp, if (on) Color.White.copy(alpha = 0.5f) else Glass.Edge, shape)
                                .clickable { onSelect(o) }.padding(horizontal = 6.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                        ) {
                            Box(
                                Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(if (on) Color.White.copy(alpha = 0.22f) else Glass.TileBg),
                                contentAlignment = Alignment.Center,
                            ) { Lc(productIcon(o), if (on) Color.White else Glass.Blue, 20.dp) }
                            T(sentence(o), 11.sp, if (on) Color.White else Glass.Ink, FontWeight.SemiBold, maxLines = 2, align = TextAlign.Center)
                        }
                    }
                }
                repeat(3 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** Primary button with the raised blue gradient and soft depth. */
@Composable
fun SubmitButton(text: String, @DrawableRes icon: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(100.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).softShadow(100.dp, 6.dp, 16.dp, 0.25f, 0.8f).clip(shape).background(Raised)
            .border(1.dp, Color.White.copy(alpha = 0.45f), shape).clickable(onClick = onClick).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) { Lc(icon, Color.White, 18.dp) }
            Spacer(Modifier.width(10.dp))
        }
        T(text, 17.sp, Color.White, FontWeight.Bold)
    }
}
