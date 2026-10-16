package com.jash.salescashtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.data.Booking
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.data.Totals
import com.jash.salescashtracker.ui.theme.AppColors
import java.time.LocalDate

@Composable
fun DashboardScreen(state: AppState) {
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val cash by Repository.cash.collectAsStateWithLifecycle()
    val lists by Repository.lists.collectAsStateWithLifecycle()
    val settings by Repository.settings.collectAsStateWithLifecycle()

    val pb = bookings.filter { state.inPeriod(it.date) }
    val pc = cash.filter { state.inPeriod(it.date) }
    val t = Totals.of(pb, pc)
    val cur = settings.currency
    val period = if (state.month == "all") "All months" else monthName(state.month)
    val expiringSoon = bookings.count { b ->
        val left = b.daysLeft(lists.docTypes)
        left != null && left in 0..7 && b.bookingStatus !in listOf("CANCELLED", "REFUND")
    }

    LargeTitlePage(
        title = settings.company.ifBlank { "Dashboard" },
        actions = { SyncBadge(); PeriodButton(state) },
        floating = {
            ExtendedFloatingActionButton(
                onClick = { state.push(Route.EditBooking(null)) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("New sale") },
                containerColor = AppColors.Blue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
            )
        },
    ) { pad ->
        LazyColumn(contentPadding = pad) {
            if (bookings.isEmpty() && cash.isEmpty()) {
                item {
                    GroupedSection {
                        EmptyState(
                            "You're starting fresh",
                            "Tap “New sale” to record your first booking, or add cash paid from the Cash Paid tab. Totals, profit and balances fill in automatically.",
                        )
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard("Total sales", money(t.sales), Modifier.weight(1f), sub = "$cur · $period")
                        KpiCard("Profit", money(t.profit), Modifier.weight(1f), sub = "Margin ${pct(t.margin)}", valueColor = AppColors.GreenText)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard("Bookings", t.count.toString(), Modifier.weight(1f), sub = "${num(t.pax)} pax")
                        KpiCard("Net cost", money(t.cost), Modifier.weight(1f), sub = cur)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard("Paid in rows", money(t.paid), Modifier.weight(1f), sub = cur)
                        KpiCard("Unpaid balance", money(t.balance), Modifier.weight(1f), sub = "Balance due",
                            valueColor = if (t.balance > 0) AppColors.OrangeText else AppColors.Label)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard("Cash paid (log)", money(t.cash), Modifier.weight(1f), sub = "${t.cashCount} entries", valueColor = AppColors.GreenText)
                        KpiCard("Net remaining", money(t.remaining), Modifier.weight(1f), sub = "Balance − cash",
                            valueColor = if (t.remaining > 0) AppColors.RedText else AppColors.Label)
                    }
                }
            }
            if (expiringSoon > 0) {
                item {
                    GroupedSection {
                        ListRow(
                            "$expiringSoon visa${if (expiringSoon > 1) "s" else ""} expiring within 7 days",
                            leading = { IconBadge(AppColors.Red) { Icon(Icons.Filled.Warning, null, tint = Color.White) } },
                            chevron = true,
                            onClick = { state.push(Route.Expiry) },
                        )
                    }
                }
            }

            // Month by month
            val months = (bookings.map { it.date.take(7) } + cash.map { it.date.take(7) }).filter { it.length == 7 }.distinct().sortedDescending()
            if (months.isNotEmpty()) {
                item {
                    GroupedSection(title = "Month by month", footer = "Tap a month to focus every screen on it.") {
                        months.forEachIndexed { i, m ->
                            val mt = Totals.of(bookings.filter { it.date.startsWith(m) }, cash.filter { it.date.startsWith(m) })
                            if (i > 0) RowDivider()
                            ListRow(
                                monthName(m),
                                subtitle = "${mt.count} bookings · profit ${money(mt.profit)} · balance ${money(mt.balance)} · cash ${money(mt.cash)}",
                                value = money(mt.sales),
                                valueColor = if (state.month == m) AppColors.Blue else AppColors.Label,
                                onClick = { state.month = if (state.month == m) "all" else m },
                            )
                        }
                    }
                }
            }

            item { BreakdownSection("Sales by product · $period", pb) { it.category } }
            item { BreakdownSection("Sales by customer type · $period", pb) { it.customerType } }
            item { CashChecker() }

            val recent = cash.sortedByDescending { it.date }.take(6)
            if (recent.isNotEmpty()) {
                item {
                    GroupedSection(title = "Latest cash paid") {
                        recent.forEachIndexed { i, c ->
                            if (i > 0) RowDivider()
                            ListRow(c.agent.ifBlank { "—" }, subtitle = "${fdate(c.date)} · ${c.mode}", value = money(c.amount),
                                valueColor = AppColors.GreenText, onClick = { state.push(Route.EditCash(c)) })
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }
}

@Composable
private fun BreakdownSection(title: String, bookings: List<Booking>, key: (Booking) -> String) {
    val groups = bookings.groupBy { key(it).ifBlank { "(blank)" } }
        .map { (k, v) -> Triple(k, v.sumOf { it.sellingPrice ?: 0.0 }, v) }
        .sortedByDescending { it.second }
    if (groups.isEmpty()) return
    val max = groups.maxOf { it.second }.coerceAtLeast(1.0)
    GroupedSection(title = title) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            groups.forEach { (name, sales, list) ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(money(sales), style = MaterialTheme.typography.titleSmall)
                    }
                    Text("${list.size} · profit ${money(list.sumOf { it.profit ?: 0.0 })}", style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(AppColors.Fill)) {
                        Box(
                            Modifier.fillMaxWidth((sales / max).toFloat().coerceIn(0.02f, 1f)).fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp)).background(AppColors.Blue),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CashChecker() {
    val cash by Repository.cash.collectAsStateWithLifecycle()
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val lists by Repository.lists.collectAsStateWithLifecycle()
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var agent by remember { mutableStateOf("") }
    val agents = (lists.agents + cash.map { it.agent } + bookings.map { it.agent }).filter { it.isNotBlank() }.distinct().sorted()
    val onDate = cash.filter { it.date == date }
    val byAgent = cash.filter { agent.isNotBlank() && it.agent == agent }
    val both = onDate.filter { agent.isBlank() || it.agent == agent }

    GroupedSection(title = "Cash paid checker", footer = "Check any day and/or agent across all months.") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DateField("Date", date, { date = it })
            DropdownField("Agent / customer (optional)", agent, agents, { agent = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcField("On ${fshort(date)} (${onDate.size})", money(onDate.sumOf { it.amount }), Modifier.weight(1f), AppColors.GreenText)
                CalcField("Agent, all time", if (agent.isBlank()) "–" else money(byAgent.sumOf { it.amount }), Modifier.weight(1f), AppColors.GreenText)
                CalcField("Date + agent", money(both.sumOf { it.amount }), Modifier.weight(1f), AppColors.GreenText)
            }
        }
    }
}
