package com.jash.salescashtracker.ui

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- "Liquid Glass" building blocks ----------

object Glass {
    val Fill = Color.White.copy(alpha = 0.66f)
    val FillStrong = Color.White.copy(alpha = 0.84f)
    val Edge = Color.White.copy(alpha = 0.9f)
    val Shadow = Color(0xFF1B2B55)
    val Ink = Color(0xFF0F1A2E)
    val Muted = Color(0xFF5E6B82)
    val Tint = Color(0xFF0A84FF)
}

/** Soft colourful backdrop the glass sits on (iOS 26 wallpaper feel). */
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

/** Frosted glass surface: translucent white, bright hairline edge, soft floating shadow. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp), strong: Boolean = false, elevation: Dp = 10.dp): Modifier =
    this
        .shadow(elevation, shape, ambientColor = Glass.Shadow.copy(alpha = 0.10f), spotColor = Glass.Shadow.copy(alpha = 0.16f))
        .clip(shape)
        .background(
            Brush.verticalGradient(
                if (strong) listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.80f))
                else listOf(Color.White.copy(alpha = 0.78f), Color.White.copy(alpha = 0.56f)),
            ),
        )
        .border(1.dp, Brush.verticalGradient(listOf(Glass.Edge, Color.White.copy(alpha = 0.25f))), shape)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .glass()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Round glass button with an icon (top-bar actions, close buttons). */
@Composable
fun GlassIconButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Glass.Ink, size: Dp = 44.dp) {
    Box(
        modifier.size(size).glass(CircleShape, strong = true, elevation = 6.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.48f)) }
}

/** Glass pill with text (period picker etc.). */
@Composable
fun GlassPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, leading: ImageVector? = null) {
    Row(
        modifier.height(40.dp).glass(RoundedCornerShape(50), strong = true, elevation = 6.dp).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) { Icon(leading, null, tint = Glass.Tint, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, color = Glass.Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
    }
}

/** Selectable chip used in the quick-add sheets (tap instead of typing). */
@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Glass.Tint) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color else Color.White.copy(alpha = 0.75f))
            .border(1.dp, if (selected) color else Color(0x1A0F1A2E), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(text, color = if (selected) Color.White else Glass.Ink, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
    }
}

/** Big iOS-style page header: large bold title + optional subtitle and trailing actions. */
@Composable
fun PageHeader(title: String, subtitle: String? = null, actions: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (subtitle != null) Text(subtitle.uppercase(), color = Glass.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
            Text(title, color = Glass.Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { actions() }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(start = 24.dp, end = 20.dp, top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Glass.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, color = Glass.Tint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

/** Coloured round icon (category / activity marker). */
@Composable
fun IconDot(icon: ImageVector, color: Color, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.7f)))),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.5f)) }
}

/** Stat tile on glass. */
@Composable
fun GlassStat(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null, valueColor: Color = Glass.Ink, onClick: (() -> Unit)? = null) {
    Column(
        modifier.glass(RoundedCornerShape(22.dp)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(14.dp),
    ) {
        Text(label, color = Glass.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (sub != null) Text(sub, color = Glass.Muted, fontSize = 12.sp, maxLines = 1)
    }
}

/** Simple vertical bar chart drawn on Canvas. */
@Composable
fun BarChart(values: List<Pair<String, Double>>, modifier: Modifier = Modifier, highlightLast: Boolean = true) {
    val max = (values.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            values.forEachIndexed { i, (_, v) ->
                val frac = (v / max).toFloat().coerceIn(0.03f, 1f)
                val on = highlightLast && i == values.lastIndex
                Box(
                    Modifier.weight(1f).fillMaxHeight(frac).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(
                            if (on) Brush.verticalGradient(listOf(Color(0xFF3D9BFF), Color(0xFF0A62FF)))
                            else Brush.verticalGradient(listOf(Color(0xFFB9D6FF), Color(0xFF8DBBFF))),
                        ),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { (label, _) ->
                Text(label, color = Glass.Muted, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

/** Donut chart for a share breakdown. */
@Composable
fun Donut(parts: List<Pair<Color, Double>>, modifier: Modifier = Modifier, stroke: Dp = 18.dp) {
    val total = parts.sumOf { it.second }.coerceAtLeast(0.0001)
    Canvas(modifier) {
        var start = -90f
        val sw = stroke.toPx()
        val inset = sw / 2
        parts.forEach { (c, v) ->
            val sweep = (v / total * 360f).toFloat()
            drawArc(
                c, start, (sweep - 1.5f).coerceAtLeast(0.5f), false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = sw, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            start += sweep
        }
    }
}

val ChartColors = listOf(
    Color(0xFF0A84FF), Color(0xFF34C759), Color(0xFFFF9500), Color(0xFFAF52DE),
    Color(0xFFFF2D55), Color(0xFF5AC8FA), Color(0xFFFFCC00), Color(0xFF8E8E93),
)

