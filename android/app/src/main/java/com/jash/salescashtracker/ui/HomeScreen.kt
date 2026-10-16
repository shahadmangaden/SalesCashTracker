package com.jash.salescashtracker.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.R
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.data.Totals
import com.jash.salescashtracker.data.dueAfterCash
import com.jash.salescashtracker.data.statusWith
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.remember
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(state: AppState) {
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val cash by Repository.cash.collectAsStateWithLifecycle()
    val lists by Repository.lists.collectAsStateWithLifecycle()
    val settings by Repository.settings.collectAsStateWithLifecycle()
    val code = LocalCurrency.current

    val pb = bookings.filter { state.inPeriod(it.date) }
    val pc = cash.filter { state.inPeriod(it.date) }
    val t = Totals.of(pb, pc)
    // Balance still due on this period's sales after agents' cash payments are applied
    val dueMap = remember(bookings, cash) { dueAfterCash(bookings, cash) }
    val balanceDue = pb.sumOf { dueMap[it.id] ?: (it.balance ?: 0.0) }
    var homeTab by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("unpaid") }
    val hour = LocalTime.now().hour
    val greet = when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }
    val firstName = nice(settings.user.split(" ").firstOrNull() ?: "")

    // Six months of sales ending at the selected month
    val end = if (state.month == "all") YearMonth.now() else runCatching { YearMonth.parse(state.month) }.getOrDefault(YearMonth.now())
    val mf = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
    fun monthSales(ym: YearMonth) = bookings.filter { it.date.startsWith(ym.toString()) }.sumOf { it.sellingPrice ?: 0.0 }
    val chart = (5 downTo 0).map { i -> end.minusMonths(i.toLong()).let { it.format(mf) to monthSales(it) } }
    val growth: Double? = if (state.month == "all") null else {
        val prev = monthSales(end.minusMonths(1))
        if (prev > 0) (t.sales - prev) / prev else null
    }

    val expiring = bookings.mapNotNull { b -> b.daysLeft(lists.docTypes)?.let { b to it } }
        .filter { (b, d) -> d in 0..7 && b.bookingStatus !in listOf("CANCELLED", "REFUND") }
        .sortedBy { it.second }
    val dueAgents = bookings.groupBy { it.agent.ifBlank { "No agent" } }
        .map { (a, list) -> a to Totals.of(list, cash.filter { it.agent.ifBlank { "No agent" } == a }).remaining }
        .filter { it.second > 0.5 }
    val toCollect = dueAgents.sumOf { it.second }

    data class Activity(val sort: String, @DrawableRes val icon: Int, val cash: Boolean, val title: String, val sub: String, val amount: String, val open: () -> Unit)
    val activity = (
        bookings.map { b ->
            Activity(b.date + b.createdAt, productIcon(b.category), false,
                listOf(nice(b.customer), sentence(b.category)).filter { it.isNotBlank() }.joinToString(" · "),
                listOf(nice(b.agent), dayHeader(b.date)).filter { it.isNotBlank() }.joinToString(" · "),
                cur(b.sellingPrice, code)) { state.push(Route.EditSale(b)) }
        } + cash.map { c ->
            Activity(c.date + c.createdAt, R.drawable.lc_wallet, true,
                "Cash paid · ${nice(c.agent).ifBlank { "No agent" }}",
                listOf(sentence(c.mode), dayHeader(c.date)).filter { it.isNotBlank() }.joinToString(" · "),
                "+" + cur(c.amount, code)) { state.push(Route.EditCash(c)) }
        }
        ).sortedByDescending { it.sort }.take(6)

    LazyColumn(contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    T(if (firstName.isNotBlank()) "$greet, $firstName" else greet, 13.sp, Glass.Muted)
                    T("Your overview", 30.sp, Glass.Ink, FontWeight.Bold)
                }
                PeriodChip(state)
            }
        }

        // Hero card (charcoal bento card with a collection ring)
        item {
            val collected = if (t.sales > 0) ((t.sales - balanceDue) / t.sales).coerceIn(0.0, 1.0) else 0.0
            val shape = RoundedCornerShape(28.dp)
            Column(
                Modifier.fillMaxWidth().softShadow(28.dp, 8.dp, 20.dp, 0.25f, 0.7f).clip(shape)
                    .background(Brush.linearGradient(listOf(Color(0xFF2A2A2E), Color(0xFF18181B))))
                    .clickable { state.tab = Tab.Reports }.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        T("Total sales · $code", 13.sp, Color.White.copy(alpha = 0.65f), FontWeight.SemiBold)
                        T(amt(t.sales), 38.sp, Color.White, FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            T("Profit ${cur(t.profit, code)}", 12.sp, Glass.Yellow, FontWeight.SemiBold)
                            if (growth != null) StatusPill(if (growth >= 0) "↗ ${pct(growth)}" else "↘ ${pct(-growth)}", Glass.Ink, Glass.Yellow, 10.sp)
                        }
                    }
                    Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Canvas(Modifier.size(78.dp)) {
                            val sw = 8.dp.toPx()
                            val tl = androidx.compose.ui.geometry.Offset(sw / 2, sw / 2)
                            val sz = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw)
                            drawArc(Color.White.copy(alpha = 0.14f), -90f, 360f, false, tl, sz, style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                            drawArc(Glass.Yellow, -90f, (360f * collected).toFloat(), false, tl, sz,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            T("${Math.round(collected * 100)}%", 16.sp, Color.White, FontWeight.Bold)
                            T("collected", 9.sp, Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
                BarChart(chart, dark = true)
            }
        }

        // Key figures
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricTile(R.drawable.lc_hand_coins, "Balance due · $code", amt(balanceDue), Modifier.weight(1f)) { state.tab = Tab.Sales }
                    MetricTile(R.drawable.lc_wallet, "Cash paid · $code", amt(t.cash), Modifier.weight(1f)) { state.tab = Tab.Cash }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricTile(R.drawable.lc_landmark, "Net remaining · $code", amt(t.remaining), Modifier.weight(1f)) {
                        state.reportsPage = "agents"; state.tab = Tab.Reports
                    }
                    MetricTile(R.drawable.lc_users, "Passengers", num(t.pax).ifBlank { "0" }, Modifier.weight(1f))
                }
            }
        }

        // Needs attention
        if (expiring.isNotEmpty() || toCollect > 0.5) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeading("Needs attention", "View all") { state.reportsPage = if (expiring.isNotEmpty()) "visas" else "agents"; state.tab = Tab.Reports }
                    GlassCard(padding = PaddingValues(13.dp), spacing = 12.dp) {
                        if (expiring.isNotEmpty()) {
                            val (b, d) = expiring.first()
                            AttentionRow(R.drawable.lc_calendar_clock, Glass.Amber,
                                if (expiring.size == 1) "1 visa expires this week" else "${expiring.size} visas expire this week",
                                "${nice(b.customer)} expires ${whenLeft(d)}") { state.reportsPage = "visas"; state.tab = Tab.Reports }
                        }
                        if (toCollect > 0.5) {
                            AttentionRow(R.drawable.lc_hand_coins, Glass.Blue,
                                "Collect ${cur(toCollect, code)} from ${dueAgents.size} ${if (dueAgents.size == 1) "agent" else "agents"}", null) {
                                state.reportsPage = "agents"; state.tab = Tab.Reports
                            }
                        }
                    }
                }
            }
        }

        // Paid / Unpaid / Cash folder tabs with a timeline
        item {
            val paidSales = pb.filter { b -> b.statusWith(dueMap[b.id] ?: (b.balance ?: 0.0)) == "PAID" }
            val unpaidSales = pb.filter { b -> (dueMap[b.id] ?: (b.balance ?: 0.0)) > 0.5 }
            data class Line(val sort: String, val date: String, @DrawableRes val icon: Int, val title: String, val sub: String, val amount: String, val color: Color, val open: () -> Unit)
            val lines = when (homeTab) {
                "paid" -> paidSales.map { b -> Line(b.date + b.createdAt, b.date, productIcon(b.category), nice(b.customer), listOf(sentence(b.category), nice(b.agent)).filter { it.isNotBlank() }.joinToString(" · "), cur(b.sellingPrice, code), Glass.Ink) { state.push(Route.EditSale(b)) } }
                "cash" -> pc.map { c -> Line(c.date + c.createdAt, c.date, R.drawable.lc_wallet, nice(c.agent).ifBlank { "No agent" }, sentence(c.mode), "+" + cur(c.amount, code), Glass.Green) { state.push(Route.EditCash(c)) } }
                else -> unpaidSales.map { b -> Line(b.date + b.createdAt, b.date, productIcon(b.category), nice(b.customer), "Due ${cur(dueMap[b.id] ?: 0.0, code)} · ${nice(b.agent)}", cur(b.sellingPrice, code), Glass.Red) { state.push(Route.EditSale(b)) } }
            }.sortedByDescending { it.sort }.take(8)
            FolderTabs(
                listOf("paid" to "Paid · ${paidSales.size}", "unpaid" to "Unpaid · ${unpaidSales.size}", "cash" to "Cash · ${pc.size}"),
                homeTab, { homeTab = it },
            ) {
                if (lines.isEmpty()) T(if (bookings.isEmpty()) "Tap + to add your first sale or cash payment." else "Nothing here for this month.", 12.sp, Glass.Muted, maxLines = 2, modifier = Modifier.padding(vertical = 8.dp))
                lines.forEachIndexed { i, l ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = l.open), verticalAlignment = Alignment.Top) {
                        // timeline: date + dashed connector
                        Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            T(runCatching { java.time.LocalDate.parse(l.date.take(10)).dayOfMonth.toString() }.getOrDefault(""), 15.sp, Glass.Ink, FontWeight.Bold)
                            T(runCatching { java.time.LocalDate.parse(l.date.take(10)).format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)) }.getOrDefault(""), 10.sp, Glass.Muted)
                            if (i < lines.lastIndex) androidx.compose.foundation.Canvas(Modifier.width(2.dp).height(18.dp)) {
                                drawLine(Glass.Divider, androidx.compose.ui.geometry.Offset(size.width / 2, 0f), androidx.compose.ui.geometry.Offset(size.width / 2, size.height),
                                    strokeWidth = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                            }
                        }
                        Row(Modifier.weight(1f).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(34.dp).clip(CircleShape).background(Glass.TileBg), contentAlignment = Alignment.Center) { Lc(l.icon, Glass.Ink, 17.dp) }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                T(l.title.ifBlank { "—" }, 13.sp, Glass.Ink, FontWeight.SemiBold)
                                T(l.sub, 10.sp, Glass.Muted)
                            }
                            T(l.amount, 13.sp, l.color, FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item { TabBarSpacer() }
    }
}

@Composable
private fun AttentionRow(@DrawableRes icon: Int, tint: Color, title: String, sub: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Lc(icon, tint, 19.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            T(title, 13.sp, Glass.Ink, FontWeight.SemiBold)
            if (sub != null) T(sub, 11.sp, Glass.Muted)
        }
        Lc(R.drawable.lc_chevron_right, Glass.Ink, 16.dp)
    }
}

/** 0 -> "today", 1 -> "tomorrow", n -> "in n days" */
fun whenLeft(d: Long): String = when (d) { 0L -> "today"; 1L -> "tomorrow"; else -> "in $d days" }

/** 0.15 -> "15%", 0.125 -> "12.5%" */
fun pctWhole(v: Double): String {
    val p = v * 100
    return if (Math.abs(p - Math.round(p)) < 0.05) "${Math.round(p)}%" else String.format(Locale.ENGLISH, "%.1f%%", p)
}
