package com.jash.salescashtracker.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.R
import com.jash.salescashtracker.data.Booking
import com.jash.salescashtracker.data.CashEntry
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.data.Totals
import com.jash.salescashtracker.data.dueAfterCash
import com.jash.salescashtracker.data.statusWith
import com.jash.salescashtracker.ui.theme.Inter
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pill search field (44dp, glass). */
@Composable
fun GlassSearch(value: String, onChange: (String) -> Unit, placeholder: String) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).glass(100.dp, shadow = false).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Lc(R.drawable.lc_search, Glass.Muted, 18.dp)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) T(placeholder, 14.sp, Glass.Muted)
            BasicTextField(
                value, onChange, singleLine = true, modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontFamily = Inter, fontSize = 14.sp, color = Glass.Ink),
                cursorBrush = SolidColor(Glass.Blue),
            )
        }
        if (value.isNotEmpty()) Lc(R.drawable.lc_x, Glass.Muted, 16.dp, Modifier.clip(CircleShape).clickable { onChange("") }, "Clear")
    }
}

/** Heading action that opens the month picker. */
@Composable
fun PeriodMenuButton(state: AppState, @DrawableRes icon: Int) {
    var open by remember { mutableStateOf(false) }
    Box {
        HeadingAction(icon, { open = true }, "Choose month")
        MonthMenu(state, open) { open = false }
    }
}

private val dayShort = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val dayLongYear = DateTimeFormatter.ofPattern("EEEE · d MMM yyyy", Locale.ENGLISH)
private val dayLong = DateTimeFormatter.ofPattern("EEEE · d MMM", Locale.ENGLISH)

/** "TODAY · 16 OCT", "YESTERDAY · 15 OCT", "MONDAY · 13 OCT" */
fun dayLabel(iso: String): String = runCatching {
    val d = LocalDate.parse(iso.take(10))
    val today = LocalDate.now()
    when {
        d == today -> "Today · " + d.format(dayShort)
        d == today.minusDays(1) -> "Yesterday · " + d.format(dayShort)
        d.year == today.year -> d.format(dayLong)
        else -> d.format(dayLongYear)
    }.uppercase(Locale.ENGLISH)
}.getOrDefault(iso)

@Composable
private fun DayHeader(day: String, total: String) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Caps(dayLabel(day), 12.sp, Glass.OnBg, 1.6.sp, Modifier.weight(1f))
        T(total, 12.sp, Glass.OnBgMuted, FontWeight.SemiBold)
    }
}

// ---------- Sales ----------

@Composable
fun SalesScreen(state: AppState) {
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val cash by Repository.cash.collectAsStateWithLifecycle()
    val due = remember(bookings, cash) { dueAfterCash(bookings, cash) }
    val code = LocalCurrency.current
    var search by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val q = search.trim().lowercase()
    val rows = bookings
        .filter { state.inPeriod(it.date) }
        .filter { state.agentFilter.isBlank() || it.agent == state.agentFilter }
        .filter { q.isBlank() || listOf(it.customer, it.agent, it.pnr, it.ref, it.detail, it.contact, it.sector, it.category).any { f -> f.lowercase().contains(q) } }
        .filter { b ->
            val d = due[b.id] ?: (b.balance ?: 0.0)
            when (status) { "" -> true; "DUE" -> d > 0.5; else -> b.statusWith(d) == status }
        }
        .sortedWith(compareByDescending<Booking> { it.date }.thenByDescending { it.createdAt })
    val t = Totals.of(rows, emptyList())
    val groups = rows.groupBy { it.date }

    LazyColumn(contentPadding = PaddingValues(start = 22.dp, end = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            AccentHeader("Sales", state.periodLong, actions = { HeaderMonthButton(state) }) {
                HeaderCard {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            T("Total sales · $code", 13.sp, Glass.Muted, FontWeight.SemiBold)
                            T(amt(t.sales), 30.sp, Glass.Ink, FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            T("Profit", 11.sp, Glass.Muted, FontWeight.SemiBold)
                            T(amt(t.profit), 16.sp, Glass.Ink, FontWeight.Bold)
                        }
                    }
                    T("${t.count} ${if (t.count == 1) "sale" else "sales"} · ${num(t.pax).ifBlank { "0" }} passengers", 12.sp, Glass.Muted)
                }
            }
        }
        item { Box(Modifier.padding(top = 10.dp)) { GlassSearch(search, { search = it }, "Search customer, agent or product") } }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.agentFilter.isNotBlank()) Choice("✕  ${nice(state.agentFilter)}", true, { state.agentFilter = "" }, large = true)
                listOf("" to "All", "DUE" to "Balance due", "PAID" to "Paid", "PARTIAL" to "Partial", "PENDING" to "Pending").forEach { (k, l) ->
                    Choice(l, status == k, { status = k }, large = true)
                }
            }
        }
        if (rows.isEmpty()) item {
            GlassCard(spacing = 4.dp) {
                T("No sales here", 15.sp, Glass.Ink, FontWeight.Bold)
                T(if (bookings.isEmpty()) "Tap + to add your first sale." else "Try another month, search or filter.", 12.sp, Glass.Muted, maxLines = 2)
            }
        }
        groups.forEach { (day, list) ->
            item(key = "h$day") { DayHeader(day, cur(list.sumOf { it.sellingPrice ?: 0.0 }, code)) }
            items(list, key = { it.id }) { b -> SaleCard(b, due[b.id] ?: (b.balance ?: 0.0), code) { state.push(Route.EditSale(b)) } }
        }
        item { TabBarSpacer() }
    }
}

@Composable
private fun SaleCard(b: Booking, due: Double, code: String, onClick: () -> Unit) {
    GlassCard(padding = PaddingValues(14.dp), spacing = 10.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconTile(productIcon(b.category))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                T(nice(b.customer).ifBlank { "—" }, 14.sp, Glass.Ink, FontWeight.Bold)
                T(listOf(sentence(b.category), b.detail.let { if (it.isBlank()) b.sector else it }.let { nice(it) })
                    .filter { it.isNotBlank() }.joinToString(" · "), 11.sp, Glass.Muted)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                T(cur(b.sellingPrice, code), 14.sp, Glass.Ink, FontWeight.Bold)
                b.profit?.let { p -> T("Profit ${amt(p)}", 10.sp, if (p >= 0) Glass.Green else Glass.Red) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(nice(b.agent).ifBlank { "No agent" }, 11.sp, Glass.Muted, modifier = Modifier.weight(1f))
            PaymentPill(b.statusWith(due), due)
        }
    }
}

// ---------- Cash ----------

@Composable
fun CashScreen(state: AppState) {
    val cash by Repository.cash.collectAsStateWithLifecycle()
    val code = LocalCurrency.current
    var search by remember { mutableStateOf("") }
    val q = search.trim().lowercase()
    val period = cash.filter { state.inPeriod(it.date) }
    val rows = period
        .filter { q.isBlank() || listOf(it.agent, it.mode, it.remarks).any { f -> f.lowercase().contains(q) } }
        .sortedWith(compareByDescending<CashEntry> { it.date }.thenByDescending { it.createdAt })
    val total = period.sumOf { it.amount }
    val agents = period.map { it.agent }.filter { it.isNotBlank() }.distinct().size
    val monthWord = if (state.month == "all") "all time" else runCatching {
        YearMonth.parse(state.month).format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH))
    }.getOrDefault(state.month)

    LazyColumn(contentPadding = PaddingValues(start = 22.dp, end = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            AccentHeader("Cash", state.periodLong, actions = { HeaderMonthButton(state) }) {
                HeaderCard {
                    T("Cash paid · ${monthWord.replaceFirstChar { it.uppercase() }} · $code", 13.sp, Glass.Muted, FontWeight.SemiBold)
                    T(amt(total), 30.sp, Glass.Ink, FontWeight.Bold)
                    T("${period.size} ${if (period.size == 1) "payment" else "payments"} across $agents ${if (agents == 1) "agent" else "agents"}", 12.sp, Glass.Muted)
                }
            }
        }
        item { Box(Modifier.padding(top = 10.dp)) { GlassSearch(search, { search = it }, "Search agent, mode or remarks") } }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Caps("RECENT PAYMENTS", 13.sp, Glass.OnBg, modifier = Modifier.weight(1f))
                T("+ Add payment", 13.sp, Glass.OnBg, FontWeight.SemiBold, Modifier.clip(RoundedCornerShape(8.dp)).clickable { state.push(Route.EditCash(null)) }.padding(4.dp))
            }
        }
        if (rows.isEmpty()) item {
            GlassCard(spacing = 4.dp) {
                T("No payments yet", 15.sp, Glass.Ink, FontWeight.Bold)
                T("Tap + then “Cash paid” to record money received from an agent.", 12.sp, Glass.Muted, maxLines = 2)
            }
        }
        rows.groupBy { it.date }.forEach { (day, list) ->
            item(key = "h$day") { DayHeader(day, cur(list.sumOf { it.amount }, code)) }
            items(list, key = { it.id }) { c ->
                GlassCard(padding = PaddingValues(14.dp), onClick = { state.push(Route.EditCash(c)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconTile(R.drawable.lc_wallet, Glass.GreenBg, Glass.Green)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            T(nice(c.agent).ifBlank { "No agent" }, 14.sp, Glass.Ink, FontWeight.Bold)
                            T(listOf(sentence(c.mode), c.remarks).filter { it.isNotBlank() }.joinToString(" · "), 11.sp, Glass.Muted)
                        }
                        T(cur(c.amount, code), 14.sp, Glass.Green, FontWeight.Bold)
                    }
                }
            }
        }
        item { TabBarSpacer() }
    }
}
