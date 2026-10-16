package com.jash.salescashtracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.data.Totals
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

    val pb = bookings.filter { state.inPeriod(it.date) }
    val pc = cash.filter { state.inPeriod(it.date) }
    val t = Totals.of(pb, pc)
    val cur = settings.currency
    val hour = LocalTime.now().hour
    val greet = when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }
    val firstName = settings.user.split(" ").firstOrNull()?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ""

    // Last 6 months of sales for the chart
    val now = YearMonth.now()
    val mf = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)
    val chart = (5 downTo 0).map { i ->
        val ym = now.minusMonths(i.toLong())
        ym.format(mf) to bookings.filter { it.date.startsWith(ym.toString()) }.sumOf { it.sellingPrice ?: 0.0 }
    }

    val expiring = bookings.mapNotNull { b -> b.daysLeft(lists.docTypes)?.let { b to it } }
        .filter { (b, d) -> d in 0..7 && b.bookingStatus !in listOf("CANCELLED", "REFUND") }
        .sortedBy { it.second }
    val dueByAgent = bookings.groupBy { it.agent.ifBlank { "No agent" } }
        .map { (a, list) -> a to (Totals.of(list, cash.filter { it.agent.ifBlank { "No agent" } == a }).remaining) }
        .filter { it.second > 0.5 }
        .sortedByDescending { it.second }
        .take(3)

    data class Activity(val date: String, val sort: String, val title: String, val sub: String, val amount: String, val isCash: Boolean, val open: () -> Unit)
    val activity = (
        bookings.map { b -> Activity(b.date, b.date + b.createdAt, b.customer, listOf(b.category, b.agent).filter { it.isNotBlank() }.joinToString(" · "), money(b.sellingPrice), false) { state.push(Route.EditSale(b)) } } +
            cash.map { c -> Activity(c.date, c.date + c.createdAt, c.agent.ifBlank { "Cash paid" }, "Cash paid · ${c.mode}", "+" + money(c.amount), true) { state.push(Route.EditCash(c)) } }
        ).sortedByDescending { it.sort }.take(8)

    LazyColumn {
        item {
            PageHeader(
                title = if (firstName.isNotBlank()) "$greet," else greet,
                subtitle = settings.company,
                actions = { SyncChip() },
            )
            if (firstName.isNotBlank()) Text(firstName, color = Glass.Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, bottom = 6.dp).padding(top = 0.dp))
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { PeriodPill(state) }
        }

        // Hero card
        item {
            GlassCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), padding = 18.dp) {
                Text("Total sales", color = Glass.Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(money(t.sales), color = Glass.Ink, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Text(cur, color = Glass.Muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 7.dp))
                }
                Text(
                    "Profit ${money(t.profit)}  ·  ${pct(t.margin)} margin  ·  ${t.count} bookings",
                    color = Color(0xFF1A7F37), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(16.dp))
                BarChart(chart)
            }
        }

        // Quick stats
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassStat("Balance due", money(t.balance), Modifier.weight(1f), sub = "on bookings",
                        valueColor = if (t.balance > 0) Color(0xFFB25E00) else Glass.Ink, onClick = { state.tab = Tab.Sales })
                    GlassStat("Cash paid", money(t.cash), Modifier.weight(1f), sub = "${t.cashCount} entries",
                        valueColor = Color(0xFF1A7F37), onClick = { state.tab = Tab.Cash })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassStat("Net remaining", money(t.remaining), Modifier.weight(1f), sub = "balance − cash",
                        valueColor = if (t.remaining > 0) Color(0xFFC62828) else Glass.Ink, onClick = { state.tab = Tab.Reports })
                    GlassStat("Passengers", num(t.pax), Modifier.weight(1f), sub = "served")
                }
            }
        }

        // Needs attention
        if (expiring.isNotEmpty() || dueByAgent.isNotEmpty()) {
            item { SectionTitle("Needs attention") }
            item {
                GlassCard(Modifier.padding(horizontal = 16.dp), padding = 4.dp) {
                    expiring.take(3).forEachIndexed { i, (b, d) ->
                        if (i > 0) RowDivider(68)
                        ActivityRow(Icons.Filled.Warning, Color(0xFFFF3B30), b.customer, "${b.docType} visa expires ${if (d == 0L) "today" else "in $d days"}", "", false) {
                            state.push(Route.EditSale(b))
                        }
                    }
                    dueByAgent.forEachIndexed { i, (a, amt) ->
                        if (i > 0 || expiring.isNotEmpty()) RowDivider(68)
                        ActivityRow(Icons.Filled.Person, Color(0xFFFF9500), a, "Still to collect", money(amt), false) {
                            state.reportsPage = "agents"; state.tab = Tab.Reports
                        }
                    }
                }
            }
        }

        // Recent activity
        item { SectionTitle("Recent activity", action = if (activity.isNotEmpty()) "See all" else null, onAction = { state.tab = Tab.Sales }) }
        item {
            if (activity.isEmpty()) {
                GlassCard(Modifier.padding(horizontal = 16.dp)) {
                    Text("You're starting fresh", color = Glass.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Tap the blue + button to add your first sale or cash payment. It only takes a few taps.", color = Glass.Muted, fontSize = 15.sp)
                }
            } else {
                GlassCard(Modifier.padding(horizontal = 16.dp), padding = 4.dp) {
                    activity.forEachIndexed { i, a ->
                        if (i > 0) RowDivider(68)
                        ActivityRow(
                            if (a.isCash) Icons.Filled.Check else Icons.Filled.ShoppingCart,
                            if (a.isCash) Color(0xFF34C759) else Color(0xFF0A84FF),
                            a.title, "${dayHeader(a.date)} · ${a.sub}", a.amount, a.isCash, a.open,
                        )
                    }
                }
            }
        }
        item { TabBarSpacer() }
    }
}

@Composable
fun ActivityRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title: String,
    sub: String,
    amount: String,
    positive: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickableRow(onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconDot(icon, color)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title.ifBlank { "—" }, color = Glass.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = Glass.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (amount.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(amount, color = if (positive) Color(0xFF1A7F37) else Glass.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

fun Modifier.clickableRow(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)

