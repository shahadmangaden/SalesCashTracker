package com.jash.salescashtracker.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.data.CashEntry
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.ui.theme.AppColors
import java.time.LocalDate
import java.util.UUID

@Composable
fun CashScreen(state: AppState) {
    val cash by Repository.cash.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }
    val q = search.trim().lowercase()
    val rows = cash.filter { state.inPeriod(it.date) }
        .filter { q.isBlank() || listOf(it.agent, it.mode, it.remarks).any { f -> f.lowercase().contains(q) } }
        .sortedWith(compareByDescending<CashEntry> { it.date }.thenByDescending { it.createdAt })
    val byAgent = rows.groupBy { it.agent.ifBlank { "(blank)" } }.mapValues { e -> e.value.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }

    LargeTitlePage(
        title = "Cash Paid",
        actions = { PeriodButton(state) },
        floating = {
            FloatingActionButton(onClick = { state.push(Route.EditCash(null)) }, containerColor = AppColors.Blue, contentColor = Color.White) {
                Icon(Icons.Filled.Add, "Record cash paid")
            }
        },
    ) { pad ->
        LazyColumn(contentPadding = pad) {
            item { SearchField(search, { search = it }, "Search agent, mode, remarks…") }
            item {
                GroupedSection {
                    ListRow("Total cash paid", subtitle = "${rows.size} entries", value = money(rows.sumOf { it.amount }), valueColor = AppColors.GreenText)
                }
            }
            if (rows.isEmpty()) {
                item { EmptyState("No cash paid entries", "Tap + to record cash received from an agent or customer.") }
            }
            rows.groupBy { it.date }.forEach { (day, list) ->
                item(key = "c$day") {
                    GroupedSection(title = dayHeader(day) + "  ·  " + money(list.sumOf { it.amount })) {
                        list.forEachIndexed { i, c ->
                            if (i > 0) RowDivider()
                            ListRow(
                                c.agent.ifBlank { "—" },
                                subtitle = listOf(c.mode, c.remarks).filter { it.isNotBlank() }.joinToString(" · "),
                                value = money(c.amount),
                                valueColor = AppColors.GreenText,
                                onClick = { state.push(Route.EditCash(c)) },
                            )
                        }
                    }
                }
            }
            if (byAgent.isNotEmpty()) {
                item {
                    GroupedSection(title = "By agent") {
                        byAgent.forEachIndexed { i, e ->
                            if (i > 0) RowDivider()
                            ListRow(e.key, value = money(e.value))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashForm(original: CashEntry?, onClose: () -> Unit) {
    val lists by Repository.lists.collectAsStateWithLifecycle()
    val all by Repository.cash.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val existing = original != null && all.any { it.id == original.id }
    val c0 = original ?: CashEntry()

    var date by remember { mutableStateOf(c0.date.ifBlank { LocalDate.now().toString() }) }
    var amount by remember { mutableStateOf(if (existing) num(c0.amount) else "") }
    var agent by remember { mutableStateOf(c0.agent) }
    var mode by remember { mutableStateOf(c0.mode.ifBlank { "CASH" }) }
    var remarks by remember { mutableStateOf(c0.remarks) }
    var error by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    var savedCount by remember { mutableStateOf(0) }

    fun save(again: Boolean) {
        val a = amount.replace(",", "").trim().toDoubleOrNull()
        if (a == null) { error = "Enter the amount."; return }
        Repository.saveCash(
            c0.copy(
                id = if (existing) c0.id else UUID.randomUUID().toString(),
                date = date, amount = a, agent = agent.trim().uppercase(), mode = mode, remarks = remarks.trim(),
            ),
        )
        Toast.makeText(ctx, "Cash paid saved", Toast.LENGTH_SHORT).show()
        if (again) { amount = ""; remarks = ""; error = ""; savedCount++ } else onClose()
    }

    Scaffold(
        containerColor = AppColors.GroupedBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (existing) "Edit cash paid" else "Record cash paid", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onClose) { Text("Cancel") } },
                actions = { TextButton(onClick = { save(false) }) { Text("Save", fontWeight = FontWeight.Bold) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFF9F9F9)),
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState())) {
            if (error.isNotEmpty()) Text(error, color = AppColors.RedText, modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp))
            if (savedCount > 0) Text("$savedCount saved — add the next one", color = AppColors.GreenText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp))
            GroupedSection {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateField("Cash paid date", date, { date = it })
                    FormField("Amount", amount, { amount = it; error = "" }, keyboard = KeyboardType.Decimal, required = true)
                    DropdownField("Agent / customer", agent, lists.agents, { agent = it }, editable = true)
                    DropdownField("Payment mode", mode, lists.paymentModes, { mode = it })
                    FormField("Remarks / receipt ref", remarks, { remarks = it }, caps = false, singleLine = false)
                }
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!existing) {
                    OutlinedButton(onClick = { save(true) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("Save & add another")
                    }
                } else {
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete entry", color = AppColors.RedText) }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this cash entry?") },
            text = { Text("${c0.agent} · ${fdate(c0.date)} · ${money(c0.amount)}") },
            confirmButton = {
                TextButton(onClick = { Repository.deleteCash(c0.id); confirmDelete = false; onClose() }) { Text("Delete", color = AppColors.RedText) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
