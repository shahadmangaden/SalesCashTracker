package com.jash.salescashtracker.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.R
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.data.Totals
import com.jash.salescashtracker.data.dueAfterCash
import com.jash.salescashtracker.data.statusWith
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Ring colours (Apple Activity-style): collected, paid, due. */
val RingGreen = Color(0xFF34C759)
val RingOrange = Color(0xFFFF9F43)

/**
 * Home ("Today"): large title, week strip, hero card with activity rings,
 * three stat tiles, a visa alert and the latest records as an inset grouped list.
 */
@Composable
fun HomeScreen(state: AppState) {
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val cash by Repository.cash.collectAsStateWithLifecycle()
    val lists by Repository.lists.collectAsStateWithLifecycle()
    val settings by Repository.settings.collectAsStateWithLifecycle()
    val code = LocalCurrency.current
    val todayD = LocalDate.now()
    val today = todayD.toString()

    val dueMap = remember(bookings, cash) { dueAfterCash(bookings, cash) }
    fun dueOf(id: String, fallback: Double?) = dueMap[id] ?: (fallback ?: 0.0)

    // A single day ("yyyy-MM-dd") or "month" (the month picked from the calendar button)
    var scope by rememberSaveable { mutableStateOf(today) }
    val day = runCatching { LocalDate.parse(scope) }.getOrNull()
    fun inScope(iso: String) = if (day != null) iso.startsWith(scope) else state.inPeriod(iso)
    val busy = remember(bookings, cash) { (bookings.map { it.date.take(10) } + cash.map { it.date.take(10) }).toSet() }

    val pb = bookings.filter { inScope(it.date) }
    val pc = cash.filter { inScope(it.date) }
    val t = Totals.of(pb, pc)
    val balanceDue = pb.sumOf { dueOf(it.id, it.balance) }
    val paidCount = pb.count { b -> b.statusWith(dueOf(b.id, b.balance)) == "PAID" }
    val dueCount = pb.count { b -> dueOf(b.id, b.balance) > 0.5 }
    val collected = if (t.sales > 0) ((t.sales - balanceDue) / t.sales).coerceIn(0.0, 1.0) else 0.0

    val expiring = bookings.mapNotNull { b -> b.daysLeft(lists.docTypes)?.let { b to it } }
        .filter { (b, d) -> d in 0..7 && b.bookingStatus !in listOf("CANCELLED", "REFUND") }
        .sortedBy { it.second }

    data class Line(val sort: String, @DrawableRes val icon: Int, val title: String, val sub: String, val value: String, val valueColor: Color, val status: String, val statusColor: Color, val open: () -> Unit)
    val lines = (pb.map { b ->
        val d = dueOf(b.id, b.balance)
        Line(b.date + b.createdAt, productIcon(b.category), nice(b.customer),
            listOf(sentence(b.category), nice(b.agent)).filter { it.isNotBlank() }.joinToString(" · "),
            cur(b.sellingPrice, code), Glass.Ink, if (d > 0.5) "Due ${amt(d)}" else "Paid", if (d > 0.5) Glass.Due else Glass.Green) { state.push(Route.EditSale(b)) }
    } + pc.map { c ->
        Line(c.date + c.createdAt, R.drawable.lc_wallet, nice(c.agent).ifBlank { "No agent" }, "Cash paid · " + sentence(c.mode).ifBlank { "Cash" },
            "+" + amt(c.amount), Glass.Green, "Received", Glass.Muted) { state.push(Route.EditCash(c)) }
    }).sortedByDescending { it.sort }

    val title = when {
        day == null -> state.periodLong.substringBefore(" ")
        day == todayD -> "Today"
        day == todayD.minusDays(1) -> "Yesterday"
        else -> day.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))
    }
    val eyebrow = if (day != null) day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)) else "Whole month · " + state.periodLong.substringAfter(" ")
    var monthMenu by remember { mutableStateOf(false) }

    IosPage(Scene.Home) {
        LazyColumn(contentPadding = PaddingValues(bottom = 0.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                LargeTitle(title, eyebrow) {
                    Box {
                        GlassCircle(R.drawable.lc_calendar, "Choose a month") { monthMenu = true }
                        MonthMenu(state, monthMenu) { monthMenu = false; scope = "month" }
                    }
                    AvatarButton(settings.user) { state.push(Route.Settings) }
                }
            }
            item { WeekStrip(day ?: todayD, day != null, busy) { scope = it.toString(); state.month = it.toString().take(7) } }
            item {
                Row(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().enter(0)
                        .shadow(18.dp, RoundedCornerShape(28.dp), spotColor = Glass.Yellow.copy(alpha = 0.35f))
                        .clip(RoundedCornerShape(28.dp)).background(Glass.Yellow)
                        .clickable { state.reportsPage = "agents"; state.tab = Tab.Reports }.padding(start = 20.dp, end = 18.dp, top = 20.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        IT(when { day == todayD -> "Today’s sales"; day != null -> "Sales · " + day.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)); else -> "Sales this month" },
                            15.sp, Glass.OnAccentSub, FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IT(code, 17.sp, Glass.OnAccentSub, FontWeight.SemiBold, Modifier.padding(bottom = 6.dp))
                            IT(amt(t.sales), 42.sp, Glass.OnAccent, FontWeight.Bold)
                        }
                        Box(Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            IT((if (t.profit >= 0) "▲ Profit " else "▼ Loss ") + cur(kotlin.math.abs(t.profit), code), 13.sp, Glass.OnAccent, FontWeight.SemiBold)
                        }
                    }
                    Box(Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "${Math.round(collected * 100)}% collected, $paidCount paid, $dueCount due"
                    }) {
                        ActivityRings(
                            listOf(collected.toFloat(), if (t.count > 0) paidCount / t.count.toFloat() else 0f, if (t.count > 0) dueCount / t.count.toFloat() else 0f),
                            listOf(RingGreen, Glass.OnAccent, RingOrange),
                        )
                    }
                }
            }
            item {
                Row(Modifier.padding(horizontal = 16.dp).enter(1), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Collected", "${Math.round(collected * 100)}%", RingGreen, Modifier.weight(1f))
                    StatTile("Paid", "$paidCount of ${t.count}", Glass.Muted, Modifier.weight(1f)) { state.tab = Tab.Sales }
                    StatTile("Due", cur(balanceDue, code), RingOrange, Modifier.weight(1f), if (balanceDue > 0.5) Glass.Due else Glass.Ink) { state.tab = Tab.Sales }
                }
            }
            if (expiring.isNotEmpty()) item {
                val (b, d) = expiring.first()
                Row(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().enter(2).iosCard(22.dp)
                        .clickable { state.reportsPage = "visas"; state.tab = Tab.Reports }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(Glass.Due), contentAlignment = Alignment.Center) { Lc(R.drawable.lc_stamp, Color.White, 19.dp) }
                    Column(Modifier.weight(1f)) {
                        IT(if (expiring.size == 1) "1 visa expires this week" else "${expiring.size} visas expire this week", 15.sp, Glass.Ink, FontWeight.SemiBold)
                        IT("${nice(b.customer)} — ${whenLeft(d)}", 13.sp, Glass.Muted)
                    }
                    Lc(R.drawable.lc_chevron_right, Glass.Muted, 14.dp)
                }
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    IosSection(if (day == todayD) "Recent" else "Records", if (lines.size > 5) "See All" else null, true) { state.tab = Tab.Sales }
                    if (lines.isEmpty()) {
                        Box(Modifier.fillMaxWidth().iosCard().padding(20.dp), contentAlignment = Alignment.Center) {
                            IT(if (bookings.isEmpty() && cash.isEmpty()) "Tap + to add your first sale or cash payment." else "Nothing recorded here yet.", 15.sp, Glass.Muted, maxLines = 2)
                        }
                    } else InsetGroup(Modifier.enter(3)) {
                        val shown = lines.take(5)
                        shown.forEachIndexed { i, l ->
                            IosRow(l.icon, l.title, l.sub, l.value, l.valueColor, l.status, l.statusColor, last = i == shown.lastIndex, onClick = l.open)
                        }
                    }
                }
            }
            item { TabBarSpacer() }
        }
    }
}

/** Seven days ending tomorrow (or the chosen day + 1): weekday letter, number circle, dot when there are records. */
@Composable
private fun WeekStrip(selected: LocalDate, active: Boolean, busy: Set<String>, onPick: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val end = if (selected.isBefore(today.minusDays(5))) selected.plusDays(1) else today.plusDays(1)
    val days = (6 downTo 0).map { end.minusDays(it.toLong()) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        days.forEach { d ->
            val on = active && d == selected
            val future = d.isAfter(today)
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable(enabled = !future) { onPick(d) }.padding(vertical = 4.dp)
                    .semantics { this.selected = on },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IT(d.format(DateTimeFormatter.ofPattern("EEEEE", Locale.ENGLISH)), 13.sp, Glass.Muted, FontWeight.SemiBold)
                Box(Modifier.size(38.dp).clip(CircleShape).background(if (on) Glass.Yellow else Color.Transparent), contentAlignment = Alignment.Center) {
                    IT(d.dayOfMonth.toString(), 19.sp, if (on) Glass.OnAccent else if (future) Glass.Muted else Glass.Ink, if (on) FontWeight.Bold else FontWeight.Medium)
                }
                Box(Modifier.size(5.dp).clip(CircleShape).background(if (d.toString() in busy) Glass.Yellow else Color.Transparent))
            }
        }
    }
}

/** 0 -> "today", 1 -> "tomorrow", n -> "in n days" */
fun whenLeft(d: Long): String = when (d) { 0L -> "today"; 1L -> "tomorrow"; else -> "in $d days" }

/** 0.15 -> "15%", 0.125 -> "12.5%" */
fun pctWhole(v: Double): String {
    val p = v * 100
    return if (Math.abs(p - Math.round(p)) < 0.05) "${Math.round(p)}%" else String.format(Locale.ENGLISH, "%.1f%%", p)
}

/** Upper-case caption with letter spacing (kept for other screens). */
@Composable
fun Caps(text: String, size: TextUnit, color: Color, spacing: TextUnit = 2.sp, modifier: Modifier = Modifier) =
    IT(text, size, color, FontWeight.Bold, modifier, tracking = spacing)
