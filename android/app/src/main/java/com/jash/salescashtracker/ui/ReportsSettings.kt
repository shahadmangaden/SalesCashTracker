package com.jash.salescashtracker.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.BuildConfig
import com.jash.salescashtracker.data.DocType
import com.jash.salescashtracker.data.Lists
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.data.SyncState
import com.jash.salescashtracker.data.Totals

// ---------- Reports ----------

@Composable
fun ReportsScreen(state: AppState) {
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val cash by Repository.cash.collectAsStateWithLifecycle()
    val lists by Repository.lists.collectAsStateWithLifecycle()
    val pb = bookings.filter { state.inPeriod(it.date) }
    val pc = cash.filter { state.inPeriod(it.date) }

    LazyColumn {
        item { PageHeader("Reports", actions = { PeriodPill(state) }) }
        item {
            SegmentedChips(listOf("agents" to "Agents", "products" to "Products", "visas" to "Visa expiry"), state.reportsPage, { state.reportsPage = it },
                Modifier.glass(RoundedCornerShape(14.dp), strong = true, elevation = 4.dp))
        }
        when (state.reportsPage) {
            "products" -> {
                val groups = pb.groupBy { it.category.ifBlank { "(blank)" } }
                    .map { (k, v) -> Triple(k, v.sumOf { it.sellingPrice ?: 0.0 }, v) }.sortedByDescending { it.second }
                val types = pb.groupBy { it.customerType.ifBlank { "(blank)" } }
                    .map { (k, v) -> k to v.sumOf { it.sellingPrice ?: 0.0 } }.sortedByDescending { it.second }
                item {
                    GlassCard(Modifier.padding(16.dp), padding = 18.dp) {
                        if (groups.isEmpty()) Text("No sales in this period", color = Glass.Muted)
                        else Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.Center) {
                                Donut(groups.mapIndexed { i, g -> ChartColors[i % ChartColors.size] to g.second }, Modifier.size(132.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Sales", color = Glass.Muted, fontSize = 12.sp)
                                    Text(money(groups.sumOf { it.second }), color = Glass.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                groups.take(6).forEachIndexed { i, g ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(10.dp).clip(CircleShape).background(ChartColors[i % ChartColors.size]))
                                        Spacer(Modifier.width(6.dp))
                                        Text(g.first, color = Glass.Ink, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
                item { SectionTitle("By product") }
                item {
                    GlassCard(Modifier.padding(horizontal = 16.dp), padding = 4.dp) {
                        groups.forEachIndexed { i, (name, sales, list) ->
                            if (i > 0) RowDivider(16)
                            ReportRow(name, "${list.size} sales · profit ${money(list.sumOf { it.profit ?: 0.0 })}", money(sales))
                        }
                    }
                }
                item { SectionTitle("By customer type") }
                item {
                    GlassCard(Modifier.padding(horizontal = 16.dp), padding = 4.dp) {
                        types.forEachIndexed { i, (name, sales) ->
                            if (i > 0) RowDivider(16)
                            ReportRow(name, null, money(sales))
                        }
                    }
                }
            }
            "visas" -> {
                val rows = bookings.mapNotNull { b -> b.daysLeft(lists.docTypes)?.let { b to it } }.sortedBy { it.second }
                val upcoming = rows.filter { it.second >= 0 }
                val expired = rows.filter { it.second < 0 }.sortedByDescending { it.second }
                if (rows.isEmpty()) item {
                    GlassCard(Modifier.padding(16.dp)) {
                        Text("No visas tracked yet", color = Glass.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("Open a sale › More details › set Visa type (e.g. 30 DAYS) and Entry date.", color = Glass.Muted)
                    }
                }
                if (upcoming.isNotEmpty()) {
                    item { SectionTitle("Active") }
                    item {
                        GlassCard(Modifier.padding(horizontal = 16.dp), padding = 4.dp) {
                            upcoming.forEachIndexed { i, (b, d) ->
                                if (i > 0) RowDivider(68)
                                ActivityRow(Icons.Filled.Warning, if (d <= 7) Color(0xFFFF3B30) else if (d <= 15) Color(0xFFFF9500) else Color(0xFF34C759),
                                    b.customer, "${b.docType} · ${b.agent}", "$d days", false) { state.push(Route.EditSale(b)) }
                            }
                        }
                    }
                }
                if (expired.isNotEmpty()) {
                    item { SectionTitle("Expired") }
                    item {
                        GlassCard(Modifier.padding(horizontal = 16.dp), padding = 4.dp) {
                            expired.take(30).forEachIndexed { i, (b, d) ->
                                if (i > 0) RowDivider(68)
                                ActivityRow(Icons.Filled.DateRange, Color(0xFF8E8E93), b.customer, "${b.docType} · ${b.agent}", "${-d}d ago", false) {
                                    state.push(Route.EditSale(b))
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                val names = (pb.map { it.agent.ifBlank { "(no agent)" } } + pc.map { it.agent.ifBlank { "(no agent)" } }).distinct()
                val rows = names.map { a ->
                    a to Totals.of(pb.filter { it.agent.ifBlank { "(no agent)" } == a }, pc.filter { it.agent.ifBlank { "(no agent)" } == a })
                }.sortedByDescending { it.second.sales }
                val all = Totals.of(pb, pc)
                item {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassStat("Still to collect", money(all.remaining), Modifier.weight(1f), valueColor = if (all.remaining > 0) Color(0xFFC62828) else Glass.Ink)
                        GlassStat("Profit", money(all.profit), Modifier.weight(1f), valueColor = Color(0xFF1A7F37))
                    }
                }
                item { SectionTitle("Agents") }
                item {
                    GlassCard(Modifier.padding(horizontal = 16.dp), padding = 4.dp) {
                        if (rows.isEmpty()) Text("Nothing recorded in this period", color = Glass.Muted, modifier = Modifier.padding(12.dp))
                        rows.forEachIndexed { i, (name, t) ->
                            if (i > 0) RowDivider(68)
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    state.agentFilter = if (name == "(no agent)") "" else name; state.tab = Tab.Sales
                                }.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconDot(Icons.Filled.Person, ChartColors[i % ChartColors.size])
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(name, color = Glass.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${t.count} sales · ${money(t.sales)} · cash ${money(t.cash)}", color = Glass.Muted, fontSize = 13.sp, maxLines = 1)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(money(t.remaining), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                        color = when { t.remaining > 0 -> Color(0xFFC62828); t.remaining < 0 -> Color(0xFF1A7F37); else -> Glass.Ink })
                                    Text(if (t.remaining < 0) "paid ahead" else "to collect", color = Glass.Muted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                item {
                    Text("To collect = balance due on sales − cash paid. Tap an agent to see their sales.", color = Glass.Muted, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                }
            }
        }
        item { TabBarSpacer() }
    }
}

@Composable
private fun ReportRow(title: String, sub: String?, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Glass.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, color = Glass.Muted, fontSize = 12.sp)
        }
        Text(value, color = Glass.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------- Settings ----------

@Composable
private fun SettingRow(icon: ImageVector, color: Color, title: String, sub: String? = null, value: String? = null, titleColor: Color = Glass.Ink, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(color), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = titleColor, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!sub.isNullOrBlank()) Text(sub, color = Glass.Muted, fontSize = 12.sp, maxLines = 2)
        }
        if (value != null) Text(value, color = Glass.Muted, fontSize = 15.sp, maxLines = 1)
        if (onClick != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFFB0B4BC))
    }
}

@Composable
fun SettingsScreen(state: AppState) {
    val ctx = LocalContext.current
    val user by Repository.user.collectAsStateWithLifecycle()
    val sync by Repository.sync.collectAsStateWithLifecycle()
    val syncError by Repository.syncError.collectAsStateWithLifecycle()
    val settings by Repository.settings.collectAsStateWithLifecycle()
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val cash by Repository.cash.collectAsStateWithLifecycle()
    val lists by Repository.lists.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<String?>(null) }
    var confirmSignOut by remember { mutableStateOf(false) }
    var restoreInfo by remember { mutableStateOf<RestoreData?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching { parseBackup(ctx, uri) }
                .onSuccess { restoreInfo = it }
                .onFailure { Toast.makeText(ctx, "That file is not a tracker backup.", Toast.LENGTH_LONG).show() }
        }
    }

    LazyColumn {
        item { PageHeader("Settings") }
        item {
            GlassCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), padding = 4.dp) {
                SettingRow(Icons.Filled.AccountCircle, Glass.Tint, user?.email ?: "",
                    sub = when (sync) {
                        SyncState.Synced -> "All records synced"
                        SyncState.Syncing -> "Syncing…"
                        SyncState.Offline -> "Offline — will sync when online"
                        SyncState.Error -> "Sync problem: $syncError"
                        else -> null
                    })
                RowDivider(56)
                SettingRow(Icons.AutoMirrored.Filled.ExitToApp, Color(0xFFFF3B30), "Sign out", titleColor = Color(0xFFC62828), onClick = { confirmSignOut = true })
            }
        }
        item { SectionTitle("Business") }
        item {
            GlassCard(Modifier.padding(horizontal = 16.dp), padding = 4.dp) {
                SettingRow(Icons.Filled.Info, Color(0xFF5856D6), "Company", value = settings.company, onClick = { editing = "company" })
                RowDivider(56)
                SettingRow(Icons.Filled.Person, Color(0xFFFF9500), "Sales person", value = settings.user, onClick = { editing = "user" })
                RowDivider(56)
                SettingRow(Icons.Filled.DateRange, Color(0xFF34C759), "Currency", value = settings.currency, onClick = { editing = "currency" })
                RowDivider(56)
                SettingRow(Icons.AutoMirrored.Filled.List, Color(0xFF13294B), "Lists & visa durations", sub = "Agents, products, payment modes…", onClick = { state.push(Route.ListsEditor) })
            }
        }
        item { SectionTitle("Data") }
        item {
            GlassCard(Modifier.padding(horizontal = 16.dp), padding = 4.dp) {
                SettingRow(Icons.Filled.Share, Color(0xFF34C759), "Export to Excel (CSV)", sub = "Sales and cash paid", onClick = { exportCsv(ctx, bookings, cash, lists) })
                RowDivider(56)
                SettingRow(Icons.Filled.DateRange, Glass.Tint, "Save backup file", sub = "Works with the website & Windows app", onClick = { shareBackup(ctx, bookings, cash, lists, settings) })
                RowDivider(56)
                SettingRow(Icons.Filled.Refresh, Color(0xFF8E8E93), "Restore from backup file", onClick = { picker.launch(arrayOf("application/json", "text/plain", "*/*")) })
            }
        }
        item { SectionTitle("About") }
        item {
            GlassCard(Modifier.padding(horizontal = 16.dp), padding = 4.dp) {
                SettingRow(Icons.Filled.Info, Color(0xFF8E8E93), "Version", value = BuildConfig.VERSION_NAME)
                RowDivider(56)
                SettingRow(Icons.Filled.Share, Glass.Tint, "Open the website", onClick = {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shahadmangaden.github.io/SalesCashTracker/")))
                })
                RowDivider(56)
                SettingRow(Icons.Filled.Refresh, Color(0xFF5856D6), "Download updates", sub = "Latest Android & Windows versions", onClick = {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/shahadmangaden/SalesCashTracker/releases")))
                })
            }
        }
        item { TabBarSpacer() }
    }

    editing?.let { field ->
        var text by remember(field) { mutableStateOf(when (field) { "company" -> settings.company; "user" -> settings.user; else -> settings.currency }) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(when (field) { "company" -> "Company"; "user" -> "Sales person"; else -> "Currency" }) },
            text = { OutlinedTextField(text, { text = it }, singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)) },
            confirmButton = {
                TextButton(onClick = {
                    val v = text.trim()
                    Repository.saveSettings(when (field) { "company" -> settings.copy(company = v); "user" -> settings.copy(user = v); else -> settings.copy(currency = v) })
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("Your records stay safe in your account. Sign in again to see them.") },
            confirmButton = { TextButton(onClick = { confirmSignOut = false; Repository.signOut() }) { Text("Sign out", color = Color(0xFFC62828)) } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
        )
    }
    restoreInfo?.let { r ->
        AlertDialog(
            onDismissRequest = { restoreInfo = null },
            title = { Text("Restore this backup?") },
            text = { Text("${r.bookings.size} sales and ${r.cash.size} cash entries will be added to your account and appear on all your devices.") },
            confirmButton = {
                TextButton(onClick = {
                    Repository.restore(r.bookings, r.cash, r.lists, r.settings); restoreInfo = null
                    Toast.makeText(ctx, "Backup restored", Toast.LENGTH_SHORT).show()
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { restoreInfo = null }) { Text("Cancel") } },
        )
    }
}

// ---------- Lists editor (sheet) ----------

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ListsEditorSheet(state: AppState) {
    val lists by Repository.lists.collectAsStateWithLifecycle()
    var section by remember { mutableStateOf("agents") }
    val sections: List<Triple<String, String, Pair<List<String>, (List<String>) -> Lists>>> = listOf(
        Triple("agents", "Agents", lists.agents to { v: List<String> -> lists.copy(agents = v) }),
        Triple("categories", "Products", lists.categories to { v: List<String> -> lists.copy(categories = v) }),
        Triple("customerTypes", "Customer types", lists.customerTypes to { v: List<String> -> lists.copy(customerTypes = v) }),
        Triple("paymentModes", "Payment modes", lists.paymentModes to { v: List<String> -> lists.copy(paymentModes = v) }),
        Triple("bookingStatuses", "Booking statuses", lists.bookingStatuses to { v: List<String> -> lists.copy(bookingStatuses = v) }),
        Triple("countries", "Sectors", lists.countries to { v: List<String> -> lists.copy(countries = v) }),
        Triple("docTypes", "Visa types", emptyList<String>() to { _: List<String> -> lists }),
    )
    var adding by remember(section) { mutableStateOf("") }

    SheetFrame("Lists", onCancel = { state.pop() }, onSave = null) {
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sections.forEach { (key, label, _) -> ChoiceChip(label, section == key, { section = key }) }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(adding, { adding = it.uppercase() }, placeholder = { Text("Add new…") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
            TextButton(onClick = {
                val v = adding.trim()
                if (v.isNotEmpty()) {
                    if (section == "docTypes") { if (lists.docTypes.none { it.name == v }) Repository.saveLists(lists.copy(docTypes = lists.docTypes + DocType(v, null))) }
                    else { val (items, update) = sections.first { it.first == section }.third; if (v !in items) Repository.saveLists(update(items + v)) }
                }
                adding = ""
            }) { Text("Add", fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(10.dp))
        GlassCard(padding = 4.dp) {
            if (section == "docTypes") {
                lists.docTypes.forEachIndexed { i, d ->
                    if (i > 0) RowDivider(12)
                    Row(Modifier.padding(start = 12.dp, end = 2.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(d.name, color = Glass.Ink, modifier = Modifier.weight(1f))
                        var days by remember(d.name, d.days) { mutableStateOf(d.days?.toString() ?: "") }
                        OutlinedTextField(days, { v ->
                            days = v.filter { it.isDigit() }
                            Repository.saveLists(lists.copy(docTypes = lists.docTypes.map { x -> if (x.name == d.name) x.copy(days = days.toIntOrNull()) else x }))
                        }, placeholder = { Text("days") }, singleLine = true, shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(96.dp))
                        IconButton(onClick = { Repository.saveLists(lists.copy(docTypes = lists.docTypes.filterIndexed { j, _ -> j != i })) }) {
                            Icon(Icons.Filled.Close, "Remove", tint = Color(0xFFC62828))
                        }
                    }
                }
            } else {
                val (items, update) = sections.first { it.first == section }.third
                items.forEachIndexed { i, v ->
                    if (i > 0) RowDivider(12)
                    Row(Modifier.padding(start = 12.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(v, color = Glass.Ink, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
                        IconButton(onClick = { Repository.saveLists(update(items.filterIndexed { j, _ -> j != i })) }) {
                            Icon(Icons.Filled.Close, "Remove", tint = Color(0xFFC62828))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
