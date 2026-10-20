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

/** Short day title for list sections: "Today", "Yesterday", "Monday 13 Oct". */
fun daySection(iso: String): String = runCatching {
    val d = LocalDate.parse(iso.take(10)); val today = LocalDate.now()
    when {
        d == today -> "Today"
        d == today.minusDays(1) -> "Yesterday"
        d.year == today.year -> d.format(DateTimeFormatter.ofPattern("EEEE d MMM", Locale.ENGLISH))
        else -> d.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    }
}.getOrDefault(iso)

@Composable
fun SalesScreen(state: AppState) {
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val cash by Repository.cash.collectAsStateWithLifecycle()
    val settings by Repository.settings.collectAsStateWithLifecycle()
    val due = remember(bookings, cash) { dueAfterCash(bookings, cash) }
    val code = LocalCurrency.current
    var search by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }   // "", "PAID", "DUE"
    var monthMenu by remember { mutableStateOf(false) }
    val q = search.trim().lowercase()
    val period = bookings.filter { state.inPeriod(it.date) }.filter { state.agentFilter.isBlank() || it.agent == state.agentFilter }
    fun dueOf(b: Booking) = due[b.id] ?: (b.balance ?: 0.0)
    val rows = period
        .filter { q.isBlank() || listOf(it.customer, it.agent, it.pnr, it.ref, it.detail, it.contact, it.sector, it.category).any { f -> f.lowercase().contains(q) } }
        .filter { b -> when (status) { "PAID" -> dueOf(b) <= 0.5; "DUE" -> dueOf(b) > 0.5; else -> true } }
        .sortedWith(compareByDescending<Booking> { it.date }.thenByDescending { it.createdAt })
    val all = Totals.of(period, emptyList())
    val paidN = period.count { dueOf(it) <= 0.5 }
    val dueTotal = period.sumOf { dueOf(it) }

    IosPage(Scene.Sales) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                LargeTitle("Sales", state.periodLong) {
                    Box { GlassCircle(R.drawable.lc_calendar, "Choose a month") { monthMenu = true }; MonthMenu(state, monthMenu) { monthMenu = false } }
                    AvatarButton(settings.user) { state.push(Route.Settings) }
                }
            }
            item { Box(Modifier.padding(horizontal = 16.dp)) { IosSearch(search, { search = it }, "Search customer, agent or PNR") } }
            item {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    IosSegmented(listOf("" to "All  ${period.size}", "PAID" to "Paid  $paidN", "DUE" to "Due  ${period.size - paidN}"), status, { status = it })
                }
            }
            if (state.agentFilter.isNotBlank()) item {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Choice("✕  ${nice(state.agentFilter)}", true, { state.agentFilter = "" })
                }
            }
            item {
                Row(Modifier.padding(horizontal = 16.dp).enter(0), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Total", cur(all.sales, code), Glass.Yellow, Modifier.weight(1f))
                    StatTile("Profit", cur(all.profit, code), RingGreen, Modifier.weight(1f), Glass.Green)
                    StatTile("Due", cur(dueTotal, code), RingOrange, Modifier.weight(1f), if (dueTotal > 0.5) Glass.Due else Glass.Ink) { status = "DUE" }
                }
            }
            if (rows.isEmpty()) item {
                Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().iosCard().padding(20.dp), contentAlignment = Alignment.Center) {
                    IT(if (bookings.isEmpty()) "Tap + to add your first sale." else "No sales match. Try another month, search or filter.", 15.sp, Glass.Muted, maxLines = 2)
                }
            }
            rows.groupBy { it.date.take(10) }.entries.forEachIndexed { gi, (day, list) ->
                item(key = "s$day") {
                    Column(Modifier.padding(horizontal = 16.dp).enter(gi + 1), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        IosSection(daySection(day), cur(list.sumOf { it.sellingPrice ?: 0.0 }, code))
                        InsetGroup {
                            list.forEachIndexed { i, b ->
                                val d = dueOf(b)
                                IosRow(productIcon(b.category), nice(b.customer),
                                    listOf(sentence(b.category), nice(b.agent)).filter { it.isNotBlank() }.joinToString(" · "),
                                    cur(b.sellingPrice, code), Glass.Ink, if (d > 0.5) "Due ${amt(d)}" else "Paid", if (d > 0.5) Glass.Due else Glass.Green,
                                    last = i == list.lastIndex) { state.push(Route.EditSale(b)) }
                            }
                        }
                    }
                }
            }
            item { TabBarSpacer() }
        }
    }
}

// ---------- Cash ----------

@Composable
fun CashScreen(state: AppState) {
    val cash by Repository.cash.collectAsStateWithLifecycle()
    val settings by Repository.settings.collectAsStateWithLifecycle()
    val code = LocalCurrency.current
    var search by remember { mutableStateOf("") }
    var monthMenu by remember { mutableStateOf(false) }
    val q = search.trim().lowercase()
    val period = cash.filter { state.inPeriod(it.date) }
    val rows = period
        .filter { q.isBlank() || listOf(it.agent, it.mode, it.remarks).any { f -> f.lowercase().contains(q) } }
        .sortedWith(compareByDescending<CashEntry> { it.date }.thenByDescending { it.createdAt })
    val total = period.sumOf { it.amount }
    val agents = period.map { it.agent }.filter { it.isNotBlank() }.distinct().size

    IosPage(Scene.Cash) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                LargeTitle("Cash", state.periodLong) {
                    Box { GlassCircle(R.drawable.lc_calendar, "Choose a month") { monthMenu = true }; MonthMenu(state, monthMenu) { monthMenu = false } }
                    AvatarButton(settings.user) { state.push(Route.Settings) }
                }
            }
            item {
                Column(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().enter(0).iosCard(28.dp, Glass.Yellow).padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IT(if (state.month == "all") "Cash received · all time" else "Cash received this month", 15.sp, Glass.OnAccentSub, FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IT(code, 17.sp, Glass.OnAccentSub, FontWeight.SemiBold, Modifier.padding(bottom = 6.dp)); IT(amt(total), 42.sp, Glass.OnAccent, FontWeight.Bold)
                    }
                    IT("${period.size} ${if (period.size == 1) "payment" else "payments"} · $agents ${if (agents == 1) "agent" else "agents"}", 13.sp, Glass.OnAccentSub, FontWeight.Medium)
                }
            }
            item { Box(Modifier.padding(horizontal = 16.dp)) { IosSearch(search, { search = it }, "Search agent or remarks") } }
            if (rows.isEmpty()) item {
                Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().iosCard().padding(20.dp), contentAlignment = Alignment.Center) {
                    IT("No payments yet. Tap + then “Cash paid”.", 15.sp, Glass.Muted, maxLines = 2)
                }
            }
            rows.groupBy { it.date.take(10) }.entries.forEachIndexed { gi, (day, list) ->
                item(key = "c$day") {
                    Column(Modifier.padding(horizontal = 16.dp).enter(gi + 1), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        IosSection(daySection(day), cur(list.sumOf { it.amount }, code))
                        InsetGroup {
                            list.forEachIndexed { i, c ->
                                IosRow(R.drawable.lc_wallet, nice(c.agent).ifBlank { "No agent" },
                                    listOf(sentence(c.mode), c.remarks).filter { it.isNotBlank() }.joinToString(" · "),
                                    "+" + amt(c.amount), Glass.Green, last = i == list.lastIndex) { state.push(Route.EditCash(c)) }
                            }
                        }
                    }
                }
            }
            item { TabBarSpacer() }
        }
    }
}
