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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.data.Booking
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.data.Totals
import com.jash.salescashtracker.ui.theme.AppColors

@Composable
fun SalesScreen(state: AppState) {
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    val q = search.trim().lowercase()
    val rows = bookings
        .filter { state.inPeriod(it.date) }
        .filter { state.agentFilter.isBlank() || it.agent == state.agentFilter }
        .filter {
            q.isBlank() || listOf(it.customer, it.agent, it.pnr, it.ref, it.detail, it.contact, it.sector, it.category)
                .any { f -> f.lowercase().contains(q) }
        }
        .filter {
            when (status) {
                "" -> true
                "DUE" -> (it.balance ?: 0.0) > 0
                else -> it.paymentStatus == status
            }
        }
        .sortedWith(compareByDescending<Booking> { it.date }.thenByDescending { it.createdAt })
    val t = Totals.of(rows, emptyList())

    LargeTitlePage(
        title = "Sales",
        actions = { PeriodButton(state) },
        floating = {
            FloatingActionButton(onClick = { state.push(Route.EditBooking(null)) }, containerColor = AppColors.Blue, contentColor = Color.White) {
                Icon(Icons.Filled.Add, "New sale")
            }
        },
    ) { pad ->
        LazyColumn(contentPadding = pad) {
            item { SearchField(search, { search = it }, "Search customer, agent, PNR…") }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.agentFilter.isNotBlank()) {
                        FilterChip(
                            selected = true,
                            onClick = { state.agentFilter = "" },
                            label = { Text(state.agentFilter) },
                            trailingIcon = { Icon(Icons.Filled.Close, "Clear agent", Modifier.padding(0.dp)) },
                        )
                    }
                    listOf("" to "All", "PAID" to "Paid", "PARTIAL" to "Partial", "PENDING" to "Pending", "DUE" to "Has balance").forEach { (k, l) ->
                        FilterChip(
                            selected = status == k,
                            onClick = { status = k },
                            label = { Text(l) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppColors.Blue,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                            ),
                        )
                    }
                }
            }
            item {
                Text(
                    "${t.count} bookings · ${num(t.pax)} pax · sales ${money(t.sales)} · profit ${money(t.profit)} · due ${money(t.balance)}",
                    style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp),
                )
            }
            if (rows.isEmpty()) {
                item { EmptyState("No sales here", if (bookings.isEmpty()) "Tap + to add your first sale." else "Try another month, search or filter.") }
            }
            val byDay = rows.groupBy { it.date }
            byDay.forEach { (day, list) ->
                item(key = "h$day") {
                    Text(
                        dayHeader(day).uppercase() + "  ·  " + money(list.sumOf { it.sellingPrice ?: 0.0 }),
                        style = MaterialTheme.typography.labelMedium, color = AppColors.Secondary,
                        modifier = Modifier.padding(start = 32.dp, top = 14.dp, bottom = 6.dp),
                    )
                }
                item(key = "g$day") {
                    Surface(shape = RoundedCornerShape(14.dp), color = AppColors.Card, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Column {
                            list.forEachIndexed { i, b ->
                                if (i > 0) RowDivider()
                                BookingRow(b) { state.push(Route.EditBooking(b)) }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }
}

@Composable
private fun BookingRow(b: Booking, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(b.customer.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(b.category, b.detail, b.agent).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                if (b.paymentStatus.isNotBlank()) Pill(b.paymentStatus)
                if (b.bookingStatus.isNotBlank()) Pill(b.bookingStatus)
                if (b.pnr.isNotBlank()) Pill(b.pnr, PillKind.Neutral)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(money(b.sellingPrice), style = MaterialTheme.typography.titleSmall)
            val p = b.profit
            if (p != null) Text(
                (if (p >= 0) "+" else "") + money(p),
                style = MaterialTheme.typography.bodySmall,
                color = if (p >= 0) AppColors.GreenText else AppColors.RedText,
            )
            val bal = b.balance ?: 0.0
            if (bal > 0) Text("due ${money(bal)}", style = MaterialTheme.typography.bodySmall, color = AppColors.RedText)
        }
    }
}
