package com.jash.salescashtracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.data.Booking
import com.jash.salescashtracker.data.CashEntry
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.data.Totals

@Composable
fun GlassSearch(value: String, onChange: (String) -> Unit, placeholder: String) {
    TextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).glass(RoundedCornerShape(22.dp), strong = true, elevation = 4.dp),
        placeholder = { Text(placeholder, color = Glass.Muted) },
        leadingIcon = { Icon(Icons.Filled.Search, null, tint = Glass.Muted) },
        trailingIcon = { if (value.isNotEmpty()) Icon(Icons.Filled.Clear, "Clear", tint = Glass.Muted, modifier = Modifier.clickable { onChange("") }) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

// ---------- Sales ----------

@Composable
fun SalesScreen(state: AppState) {
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val q = search.trim().lowercase()
    val rows = bookings
        .filter { state.inPeriod(it.date) }
        .filter { state.agentFilter.isBlank() || it.agent == state.agentFilter }
        .filter { q.isBlank() || listOf(it.customer, it.agent, it.pnr, it.ref, it.detail, it.contact, it.sector, it.category).any { f -> f.lowercase().contains(q) } }
        .filter { when (status) { "" -> true; "DUE" -> (it.balance ?: 0.0) > 0; else -> it.paymentStatus == status } }
        .sortedWith(compareByDescending<Booking> { it.date }.thenByDescending { it.createdAt })
    val t = Totals.of(rows, emptyList())

    LazyColumn {
        item { PageHeader("Sales", subtitle = "${t.count} bookings · ${money(t.sales)}", actions = { PeriodPill(state) }) }
        item { GlassSearch(search, { search = it }, "Search name, agent, PNR…") }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.agentFilter.isNotBlank()) ChoiceChip("✕  ${state.agentFilter}", true, { state.agentFilter = "" }, color = Color(0xFF5856D6))
                listOf("" to "All", "DUE" to "Balance due", "PAID" to "Paid", "PARTIAL" to "Partial", "PENDING" to "Pending").forEach { (k, l) ->
                    ChoiceChip(l, status == k, { status = k })
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassStat("Profit", money(t.profit), Modifier.weight(1f), valueColor = Color(0xFF1A7F37))
                GlassStat("Balance due", money(t.balance), Modifier.weight(1f), valueColor = if (t.balance > 0) Color(0xFFB25E00) else Glass.Ink)
            }
        }
        if (rows.isEmpty()) item {
            GlassCard(Modifier.padding(16.dp)) {
                Text("No sales here", color = Glass.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(if (bookings.isEmpty()) "Tap + to add your first sale." else "Try another month, search or filter.", color = Glass.Muted)
            }
        }
        rows.groupBy { it.date }.forEach { (day, list) ->
            item(key = "s$day") {
                Text(
                    dayHeader(day) + "   " + money(list.sumOf { it.sellingPrice ?: 0.0 }),
                    color = Glass.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 6.dp),
                )
                GlassCard(Modifier.padding(horizontal = 16.dp), padding = 4.dp) {
                    list.forEachIndexed { i, b ->
                        if (i > 0) RowDivider(16)
                        SaleRow(b) { state.push(Route.EditSale(b)) }
                    }
                }
            }
        }
        item { TabBarSpacer() }
    }
}

@Composable
private fun SaleRow(b: Booking, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(b.customer.ifBlank { "—" }, color = Glass.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOf(b.category, b.detail, b.agent).filter { it.isNotBlank() }.joinToString(" · "), color = Glass.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (b.paymentStatus.isNotBlank() || b.pnr.isNotBlank()) {
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (b.paymentStatus.isNotBlank()) Pill(b.paymentStatus)
                    if (b.pnr.isNotBlank()) Pill(b.pnr, PillKind.Neutral)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(money(b.sellingPrice), color = Glass.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            b.profit?.let { p -> Text((if (p >= 0) "+" else "") + money(p), color = if (p >= 0) Color(0xFF1A7F37) else Color(0xFFC62828), fontSize = 13.sp) }
            val bal = b.balance ?: 0.0
            if (bal > 0) Text("due ${money(bal)}", color = Color(0xFFC62828), fontSize = 12.sp)
        }
    }
}

// ---------- Cash ----------

@Composable
fun CashScreen(state: AppState) {
    val cash by Repository.cash.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }
    val q = search.trim().lowercase()
    val rows = cash.filter { state.inPeriod(it.date) }
        .filter { q.isBlank() || listOf(it.agent, it.mode, it.remarks).any { f -> f.lowercase().contains(q) } }
        .sortedWith(compareByDescending<CashEntry> { it.date }.thenByDescending { it.createdAt })
    val total = rows.sumOf { it.amount }
    val byAgent = rows.groupBy { it.agent.ifBlank { "(no agent)" } }.mapValues { e -> e.value.sumOf { it.amount } }.entries.sortedByDescending { it.value }

    LazyColumn {
        item { PageHeader("Cash Paid", subtitle = "${rows.size} entries", actions = { PeriodPill(state) }) }
        item {
            GlassCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), padding = 18.dp) {
                Text("Total received", color = Glass.Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(money(total), color = Color(0xFF1A7F37), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                if (byAgent.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    byAgent.take(4).forEach { e ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text(e.key, color = Glass.Ink, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1)
                            Text(money(e.value), color = Glass.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        item { GlassSearch(search, { search = it }, "Search agent, mode, remarks…") }
        if (rows.isEmpty()) item {
            GlassCard(Modifier.padding(16.dp)) {
                Text("No cash paid entries", color = Glass.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("Tap + then “Cash paid” to record money received.", color = Glass.Muted)
            }
        }
        rows.groupBy { it.date }.forEach { (day, list) ->
            item(key = "c$day") {
                Text(
                    dayHeader(day) + "   " + money(list.sumOf { it.amount }),
                    color = Glass.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 6.dp),
                )
                GlassCard(Modifier.padding(horizontal = 16.dp), padding = 4.dp) {
                    list.forEachIndexed { i, c ->
                        if (i > 0) RowDivider(68)
                        ActivityRow(Icons.Filled.Check, Color(0xFF34C759), c.agent.ifBlank { "—" },
                            listOf(c.mode, c.remarks).filter { it.isNotBlank() }.joinToString(" · "), money(c.amount), true) {
                            state.push(Route.EditCash(c))
                        }
                    }
                }
            }
        }
        item { TabBarSpacer() }
    }
}
