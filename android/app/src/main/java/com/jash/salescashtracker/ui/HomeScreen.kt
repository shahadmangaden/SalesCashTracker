package com.jash.salescashtracker.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.R
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.data.Totals
import com.jash.salescashtracker.data.dueAfterCash
import com.jash.salescashtracker.data.statusWith
import com.jash.salescashtracker.ui.theme.Inter
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Home, laid out like the Cream & Ink / Navy & Lime / Black & Yellow mockups:
 * accent header with a folder-tab progress card, then a dark panel listing the latest records.
 */
@Composable
fun HomeScreen(state: AppState) {
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val cash by Repository.cash.collectAsStateWithLifecycle()
    val lists by Repository.lists.collectAsStateWithLifecycle()
    val settings by Repository.settings.collectAsStateWithLifecycle()
    val code = LocalCurrency.current
    val today = LocalDate.now().toString()

    val dueMap = remember(bookings, cash) { dueAfterCash(bookings, cash) }
    fun dueOf(id: String, fallback: Double?) = dueMap[id] ?: (fallback ?: 0.0)

    // "today" or "month" (the month picked in the period menu)
    val todayCount = bookings.count { it.date.startsWith(today) } + cash.count { it.date.startsWith(today) }
    var scope by rememberSaveable { mutableStateOf("") }
    val sc = scope.ifBlank { if (todayCount > 0) "today" else "month" }
    fun inScope(iso: String) = if (sc == "today") iso.startsWith(today) else state.inPeriod(iso)

    val pb = bookings.filter { inScope(it.date) }
    val pc = cash.filter { inScope(it.date) }
    val t = Totals.of(pb, pc)
    val balanceDue = pb.sumOf { dueOf(it.id, it.balance) }
    val paidCount = pb.count { b -> b.statusWith(dueOf(b.id, b.balance)) == "PAID" }
    val dueCount = pb.count { b -> dueOf(b.id, b.balance) > 0.5 }
    val collected = if (t.sales > 0) ((t.sales - balanceDue) / t.sales).coerceIn(0.0, 1.0) else 0.0

    val monthCount = bookings.count { state.inPeriod(it.date) }
    val monthLabel = if (state.month == "all") "All" else runCatching {
        YearMonth.parse(state.month).format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH))
    }.getOrDefault("Month")

    val expiring = bookings.mapNotNull { b -> b.daysLeft(lists.docTypes)?.let { b to it } }
        .filter { (b, d) -> d in 0..7 && b.bookingStatus !in listOf("CANCELLED", "REFUND") }
        .sortedBy { it.second }
    val dueAgents = bookings.groupBy { it.agent.ifBlank { "No agent" } }
        .map { (a, list) -> a to Totals.of(list, cash.filter { it.agent.ifBlank { "No agent" } == a }).remaining }
        .filter { it.second > 0.5 }
    val toCollect = dueAgents.sumOf { it.second }

    // Records list (Unpaid / Paid / Cash)
    var filter by rememberSaveable { mutableStateOf("") }
    val unpaid = pb.filter { dueOf(it.id, it.balance) > 0.5 }
    val paid = pb.filter { b -> b.statusWith(dueOf(b.id, b.balance)) == "PAID" }
    val f = filter.ifBlank { if (unpaid.isNotEmpty()) "unpaid" else if (paid.isNotEmpty()) "paid" else if (pc.isNotEmpty()) "cash" else "unpaid" }
    data class Line(val sort: String, @DrawableRes val icon: Int, val title: String, val sub: String, val amount: String, val tag: String, val open: () -> Unit)
    val lines = when (f) {
        "paid" -> paid.map { b ->
            Line(b.date + b.createdAt, productIcon(b.category), nice(b.customer),
                listOf(sentence(b.category), nice(b.agent)).filter { it.isNotBlank() }.joinToString(" · "),
                amt(b.sellingPrice), "PAID") { state.push(Route.EditSale(b)) }
        }
        "cash" -> pc.map { c ->
            Line(c.date + c.createdAt, R.drawable.lc_wallet, nice(c.agent).ifBlank { "No agent" },
                listOf(sentence(c.mode), dayHeader(c.date)).filter { it.isNotBlank() }.joinToString(" · "),
                "+" + amt(c.amount), "CASH") { state.push(Route.EditCash(c)) }
        }
        else -> unpaid.map { b ->
            Line(b.date + b.createdAt, productIcon(b.category), nice(b.customer),
                listOf(sentence(b.category), nice(b.agent)).filter { it.isNotBlank() }.joinToString(" · "),
                amt(dueOf(b.id, b.balance)), "DUE") { state.push(Route.EditSale(b)) }
        }
    }.sortedByDescending { it.sort }
    val shown = lines.take(8)

    LazyColumn(contentPadding = PaddingValues(bottom = 0.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        item {
            Header(
                state = state, user = settings.user, code = code,
                scope = sc, onScope = { scope = it },
                todayCount = todayCount, monthLabel = monthLabel, monthCount = monthCount,
                t = t, collected = collected, paidCount = paidCount, dueCount = dueCount, balanceDue = balanceDue,
            )
        }

        if (expiring.isNotEmpty() || toCollect > 0.5) item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp).enter(0), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Caps("NEEDS ATTENTION", 13.sp, Glass.OnBg)
                GlassCard(padding = PaddingValues(14.dp), spacing = 12.dp, radius = 24.dp) {
                    if (expiring.isNotEmpty()) {
                        val (b, d) = expiring.first()
                        AttentionRow(R.drawable.lc_calendar_clock, Glass.Due,
                            if (expiring.size == 1) "1 visa expires this week" else "${expiring.size} visas expire this week",
                            "${nice(b.customer)} expires ${whenLeft(d)}") { state.reportsPage = "visas"; state.tab = Tab.Reports }
                    }
                    if (toCollect > 0.5) {
                        AttentionRow(R.drawable.lc_hand_coins, Glass.Ink,
                            "Collect ${cur(toCollect, code)} from ${dueAgents.size} ${if (dueAgents.size == 1) "agent" else "agents"}", null) {
                            state.reportsPage = "agents"; state.tab = Tab.Reports
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Caps(if (sc == "today") "TODAY'S RECORDS" else "RECORDS · ${monthLabel.uppercase()}", 13.sp, Glass.OnBg)
                    OutlineChip(lines.size.toString(), Glass.OnBg)
                    Spacer(Modifier.weight(1f))
                    if (lines.size > shown.size) T("View all", 12.sp, Glass.OnBg, FontWeight.SemiBold,
                        Modifier.clip(RoundedCornerShape(8.dp)).clickable { state.tab = if (f == "cash") Tab.Cash else Tab.Sales }.padding(4.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip("Unpaid · ${unpaid.size}", f == "unpaid") { filter = "unpaid" }
                    FilterChip("Paid · ${paid.size}", f == "paid") { filter = "paid" }
                    FilterChip("Cash · ${pc.size}", f == "cash") { filter = "cash" }
                }
            }
        }

        item {
            val panelShape = RoundedCornerShape(30.dp)
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp).fillMaxWidth()
                    .clip(panelShape).background(Glass.Panel).border(1.dp, Glass.PanelEdge, panelShape).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (shown.isEmpty()) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(16.dp), contentAlignment = Alignment.Center) {
                        T(
                            when {
                                bookings.isEmpty() && cash.isEmpty() -> "Tap + to add your first sale or cash payment."
                                sc == "today" -> "Nothing here today yet."
                                else -> "Nothing here for this month."
                            }, 13.sp, Glass.PanelSub, maxLines = 2,
                        )
                    }
                }
                // key on the filter + scope so the rows re-run their staggered entrance when the list changes
                androidx.compose.runtime.key(f, sc) {
                    shown.forEachIndexed { i, l -> RecordRow(i == 0, l.icon, l.title, l.sub, l.amount, l.tag, i, l.open) }
                }
            }
        }
        item { TabBarSpacer() }
    }
}

@Composable
private fun Header(
    state: AppState, user: String, code: String,
    scope: String, onScope: (String) -> Unit,
    todayCount: Int, monthLabel: String, monthCount: Int,
    t: Totals, collected: Double, paidCount: Int, dueCount: Int, balanceDue: Double,
) {
    val pal = ThemeState.palette
    var monthMenu by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)).background(pal.accent)
            .statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Search · date · profile
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).pressable { state.tab = Tab.Sales }.clip(CircleShape).background(Color.White.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) { Lc(R.drawable.lc_search, pal.onAccent, 20.dp, description = "Search sales") }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Caps(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE · d MMM", Locale.ENGLISH)).uppercase(), 13.sp, pal.onAccent, 2.4.sp)
            }
            Box(
                Modifier.size(44.dp).pressable { state.tab = Tab.Settings }.clip(CircleShape).background(pal.onAccent),
                contentAlignment = Alignment.Center,
            ) { T(initials(user.ifBlank { "Me" }), 15.sp, pal.accent, FontWeight.Bold) }
        }

        // Folder tabs + progress card
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                FolderTab("Today", todayCount.toString(), scope == "today") { onScope("today") }
                Box {
                    FolderTab("$monthLabel ⌄", monthCount.toString(), scope == "month") {
                        if (scope == "month") monthMenu = true else onScope("month")
                    }
                    MonthMenu(state, monthMenu) { monthMenu = false; onScope("month") }
                }
            }
            val first = scope == "today"
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = if (first) 0.dp else 22.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp))
                    .background(pal.card).clickable { state.reportsPage = "agents"; state.tab = Tab.Reports }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        T(if (first) "Today's sales · $code" else "${state.periodLong} · $code", 13.sp, pal.muted, FontWeight.SemiBold)
                        T(amt(t.sales), 30.sp, pal.ink, FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        T("Profit", 11.sp, pal.muted, FontWeight.SemiBold)
                        T(amt(t.profit), 16.sp, pal.ink, FontWeight.Bold)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RingStat(collected.toFloat(), t.count.toString(), "SALES", pal.ringFill, Modifier.weight(1f))
                    RingStat(if (t.count > 0) paidCount / t.count.toFloat() else 0f, paidCount.toString(), "PAID", pal.ringFill, Modifier.weight(1f))
                    RingStat(if (t.count > 0) dueCount / t.count.toFloat() else 0f, dueCount.toString(), "DUE", pal.due, Modifier.weight(1f))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(pal.divider))
                Row(Modifier.fillMaxWidth()) {
                    MiniFigure("Balance due", cur(balanceDue, code), if (balanceDue > 0.5) pal.due else pal.ink, Modifier.weight(1f)) { state.tab = Tab.Sales }
                    MiniFigure("Cash paid", cur(t.cash, code), pal.ink, Modifier.weight(1f)) { state.tab = Tab.Cash }
                }
            }
        }
    }
}

/** A folder tab on the accent header: the selected one joins the white card below it. */
@Composable
private fun FolderTab(label: String, count: String, on: Boolean, onClick: () -> Unit) {
    val pal = ThemeState.palette
    val bg by androidx.compose.animation.animateColorAsState(if (on) pal.card else Color.Transparent, tween(200), label = "tab")
    Row(
        Modifier.clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)).background(bg).clickable(onClick = onClick)
            .heightIn(min = 48.dp).padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        T(label, 24.sp, if (on) pal.ink else pal.onAccentSub, if (on) FontWeight.SemiBold else FontWeight.Medium)
        if (on) Box(Modifier.clip(RoundedCornerShape(8.dp)).background(pal.tile).padding(horizontal = 8.dp, vertical = 3.dp)) {
            T(count, 11.sp, pal.ink, FontWeight.Bold)
        } else OutlineChip(count, pal.onAccentSub)
    }
}

/** 40dp progress ring with the number inside and a spaced caption (sweeps in over ~500 ms). */
@Composable
private fun RingStat(fraction: Float, value: String, label: String, fill: Color, modifier: Modifier = Modifier) {
    val pal = ThemeState.palette
    val p = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        if (ThemeState.reduceMotion) p.snapTo(fraction) else p.animateTo(fraction.coerceIn(0f, 1f), tween(520, easing = FastOutSlowInEasing))
    }
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = "$value ${label.lowercase()}" },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(40.dp)) {
                val sw = 3.dp.toPx()
                val tl = Offset(sw / 2 + 1.5.dp.toPx(), sw / 2 + 1.5.dp.toPx())
                val sz = Size(size.width - sw - 3.dp.toPx(), size.height - sw - 3.dp.toPx())
                drawArc(pal.track, -90f, 360f, false, tl, sz, style = Stroke(sw))
                if (p.value > 0f) drawArc(fill, -90f, 360f * p.value, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            }
            T(value, 14.sp, pal.ink, FontWeight.Bold)
        }
        Caps(label, 11.sp, pal.ink, 0.8.sp)
    }
}

@Composable
private fun MiniFigure(label: String, value: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        T(label, 11.sp, ThemeState.palette.muted, FontWeight.SemiBold)
        T(value, 15.sp, color, FontWeight.Bold)
    }
}

/** A row in the dark panel. The first row is the highlighted accent card, like "NEXT" in the mockup. */
@Composable
private fun RecordRow(highlight: Boolean, @DrawableRes icon: Int, title: String, sub: String, amount: String, tag: String, index: Int, onClick: () -> Unit) {
    val pal = ThemeState.palette
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().enter(index).pressable(scaleTo = 0.98f, onClick = onClick)
            .then(if (highlight) Modifier.clip(shape).background(pal.accent) else Modifier.clip(shape).border(1.dp, pal.rowEdge, shape))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(if (highlight) Color.White.copy(alpha = 0.5f) else pal.rowIcon),
            contentAlignment = Alignment.Center,
        ) { Lc(icon, if (highlight) pal.onAccent else pal.panelText, 20.dp) }
        Column(Modifier.weight(1f)) {
            T(title.ifBlank { "—" }, 15.sp, if (highlight) pal.onAccent else pal.panelTitle, if (highlight) FontWeight.Bold else FontWeight.SemiBold)
            T(sub, 12.sp, if (highlight) pal.onAccentSub else pal.panelSub, if (highlight) FontWeight.Medium else FontWeight.Normal)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            T(amount, 17.sp, if (highlight) pal.onAccent else pal.panelText, if (highlight) FontWeight.Bold else FontWeight.SemiBold)
            Caps(tag, 10.sp, if (highlight) pal.onAccent else pal.panelSub, 1.sp)
        }
    }
}

/** Small selectable chip above the panel. */
@Composable
private fun FilterChip(text: String, on: Boolean, onClick: () -> Unit) {
    val pal = ThemeState.palette
    val shape = RoundedCornerShape(100.dp)
    val bg by androidx.compose.animation.animateColorAsState(if (on) pal.primary else pal.card, tween(200), label = "chip")
    Box(
        Modifier.pressable(scaleTo = 0.95f, onClick = onClick).clip(shape).background(bg).border(1.dp, if (on) pal.primary else Glass.Edge, shape)
            .heightIn(min = 36.dp).padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { T(text, 13.sp, if (on) pal.onPrimary else pal.ink, if (on) FontWeight.Bold else FontWeight.Medium) }
}

@Composable
private fun OutlineChip(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(7.dp)).border(1.dp, color, RoundedCornerShape(7.dp)).padding(horizontal = 7.dp, vertical = 2.dp)) {
        T(text, 11.sp, color, FontWeight.Bold)
    }
}

/** Upper-case caption with letter spacing (section titles, ring labels). */
@Composable
fun Caps(text: String, size: TextUnit, color: Color, spacing: TextUnit = 2.sp, modifier: Modifier = Modifier) = Text(
    text, modifier = modifier, color = color, fontSize = size, fontWeight = FontWeight.Bold, fontFamily = Inter,
    letterSpacing = spacing, maxLines = 1, overflow = TextOverflow.Ellipsis,
)

@Composable
private fun AttentionRow(@DrawableRes icon: Int, tint: Color, title: String, sub: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Glass.TileBg), contentAlignment = Alignment.Center) { Lc(icon, tint, 18.dp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            T(title, 13.sp, Glass.Ink, FontWeight.SemiBold)
            if (sub != null) T(sub, 11.sp, Glass.Muted)
        }
        Lc(R.drawable.lc_chevron_right, Glass.Muted, 16.dp)
    }
}

/** 0 -> "today", 1 -> "tomorrow", n -> "in n days" */
fun whenLeft(d: Long): String = when (d) { 0L -> "today"; 1L -> "tomorrow"; else -> "in $d days" }

/** 0.15 -> "15%", 0.125 -> "12.5%" */
fun pctWhole(v: Double): String {
    val p = v * 100
    return if (Math.abs(p - Math.round(p)) < 0.05) "${Math.round(p)}%" else String.format(Locale.ENGLISH, "%.1f%%", p)
}
