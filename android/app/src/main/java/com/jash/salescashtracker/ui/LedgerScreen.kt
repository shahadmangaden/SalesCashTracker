package com.jash.salescashtracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.data.Booking
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.data.Totals
import com.jash.salescashtracker.ui.theme.AppColors

@Composable
fun LedgerScreen(state: AppState) {
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val cash by Repository.cash.collectAsStateWithLifecycle()
    val pb = bookings.filter { state.inPeriod(it.date) }
    val pc = cash.filter { state.inPeriod(it.date) }
    val names = (pb.map { it.agent.ifBlank { "(no agent)" } } + pc.map { it.agent.ifBlank { "(no agent)" } }).distinct()
    val rows = names.map { a ->
        a to Totals.of(pb.filter { it.agent.ifBlank { "(no agent)" } == a }, pc.filter { it.agent.ifBlank { "(no agent)" } == a })
    }.sortedByDescending { it.second.sales }
    val all = Totals.of(pb, pc)

    LargeTitlePage(title = "Agent Ledger", actions = { PeriodButton(state) }) { pad ->
        LazyColumn(contentPadding = pad) {
            item {
                GroupedSection(footer = "Net remaining = balance due on bookings − cash paid in the log. Negative means the agent has paid ahead.") {
                    LedgerTotalsRow("All agents", all, bold = true)
                }
            }
            if (rows.isEmpty()) item { EmptyState("No activity", "Nothing recorded in this period.") }
            item {
                if (rows.isNotEmpty()) GroupedSection(title = "Tap an agent to see their sales") {
                    rows.forEachIndexed { i, (name, t) ->
                        if (i > 0) RowDivider()
                        Column(Modifier.clickable {
                            state.agentFilter = if (name == "(no agent)") "" else name
                            state.tab = Tab.Sales
                        }) { LedgerTotalsRow(name, t) }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun LedgerTotalsRow(name: String, t: Totals, bold: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                money(t.remaining),
                style = MaterialTheme.typography.titleSmall,
                color = when { t.remaining > 0 -> AppColors.RedText; t.remaining < 0 -> AppColors.GreenText; else -> AppColors.Label },
            )
        }
        Text(
            "${t.count} bookings · ${num(t.pax)} pax · sales ${money(t.sales)} · cost ${money(t.cost)}",
            style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary,
        )
        Text(
            "paid in rows ${money(t.paid)} · due ${money(t.balance)} · cash ${money(t.cash)} · profit ${money(t.profit)}",
            style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryScreen(onBack: () -> Unit, onOpen: (Booking) -> Unit) {
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val lists by Repository.lists.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf("upcoming") }
    val rows = bookings.mapNotNull { b -> b.daysLeft(lists.docTypes)?.let { b to it } }
        .filter { (_, left) -> when (filter) { "upcoming" -> left >= 0; "expired" -> left < 0; else -> true } }
        .sortedBy { if (filter == "expired") -it.second else it.second }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = AppColors.GroupedBg,
        topBar = {
            LargeTopAppBar(
                title = { Text("Visa Expiry") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = AppColors.GroupedBg, scrolledContainerColor = Color(0xFFF9F9F9)),
            )
        },
    ) { pad ->
        LazyColumn(contentPadding = pad) {
            item { SegmentedChips(listOf("upcoming" to "Active", "expired" to "Expired", "all" to "All"), filter, { filter = it }) }
            if (rows.isEmpty()) item {
                EmptyState("Nothing to show", "Add an Entry date and a timed visa type (like “30 DAYS”) on a sale to track its expiry here.")
            }
            item {
                if (rows.isNotEmpty()) GroupedSection {
                    rows.forEachIndexed { i, (b, left) ->
                        if (i > 0) RowDivider()
                        ListRow(
                            b.customer,
                            subtitle = "${b.docType} · entry ${fshort(b.entryDate)} · expires ${b.expireDate(lists.docTypes)?.let { fshort(it) } ?: ""}" +
                                (if (b.contact.isNotBlank()) " · ${b.contact}" else ""),
                            trailing = {
                                Pill(
                                    if (left < 0) "expired ${-left}d ago" else "$left days",
                                    when { left < 0 -> PillKind.Neutral; left <= 7 -> PillKind.Bad; left <= 15 -> PillKind.Warn; else -> PillKind.Good },
                                )
                            },
                            onClick = { onOpen(b) },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

