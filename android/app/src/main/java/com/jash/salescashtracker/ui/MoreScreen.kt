package com.jash.salescashtracker.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.BuildConfig
import com.jash.salescashtracker.data.Booking
import com.jash.salescashtracker.data.BusinessSettings
import com.jash.salescashtracker.data.CashEntry
import com.jash.salescashtracker.data.DocType
import com.jash.salescashtracker.data.Lists
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.data.SyncState
import com.jash.salescashtracker.ui.theme.AppColors
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

@Composable
fun MoreScreen(state: AppState) {
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

    LargeTitlePage(title = "More") { pad ->
        LazyColumn(contentPadding = pad) {
            item {
                GroupedSection(title = "Account") {
                    ListRow(
                        user?.email ?: "",
                        subtitle = when (sync) {
                            SyncState.Synced -> "All records synced"
                            SyncState.Syncing -> "Syncing…"
                            SyncState.Offline -> "Offline — changes will sync when you're back online"
                            SyncState.Error -> "Sync problem: $syncError"
                            else -> ""
                        },
                        leading = { IconBadge(AppColors.Blue) { Icon(Icons.Filled.AccountCircle, null, tint = Color.White) } },
                    )
                    RowDivider(58)
                    ListRow(
                        "Sign out", titleColor = AppColors.RedText,
                        leading = { IconBadge(AppColors.Red) { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color.White) } },
                        onClick = { confirmSignOut = true },
                    )
                }
            }
            item {
                GroupedSection(title = "Business") {
                    ListRow("Company", value = settings.company, chevron = true, onClick = { editing = "company" })
                    RowDivider()
                    ListRow("Sales person", value = settings.user, chevron = true, onClick = { editing = "user" })
                    RowDivider()
                    ListRow("Currency", value = settings.currency, chevron = true, onClick = { editing = "currency" })
                }
            }
            item {
                GroupedSection(title = "Tools") {
                    ListRow(
                        "Visa expiry tracker",
                        leading = { IconBadge(AppColors.Orange) { Icon(Icons.Filled.Warning, null, tint = Color.White) } },
                        chevron = true, onClick = { state.push(Route.Expiry) },
                    )
                    RowDivider(58)
                    ListRow(
                        "Lists & visa durations", subtitle = "Agents, products, payment modes, statuses…",
                        leading = { IconBadge(AppColors.Navy) { Icon(Icons.AutoMirrored.Filled.List, null, tint = Color.White) } },
                        chevron = true, onClick = { state.push(Route.ListsEditor) },
                    )
                }
            }
            item {
                GroupedSection(title = "Data", footer = "Excel import/export with full formatting is also available in the Windows app and the website.") {
                    ListRow(
                        "Export to Excel (CSV)", subtitle = "Sales and cash paid as two spreadsheets",
                        leading = { IconBadge(AppColors.Green) { Icon(Icons.Filled.Share, null, tint = Color.White) } },
                        onClick = { exportCsv(ctx, bookings, cash, lists) },
                    )
                    RowDivider(58)
                    ListRow(
                        "Save backup file", subtitle = "Same format as the website & Windows app",
                        leading = { IconBadge(AppColors.Blue) { Icon(Icons.Filled.DateRange, null, tint = Color.White) } },
                        onClick = { shareBackup(ctx, bookings, cash, lists, settings) },
                    )
                    RowDivider(58)
                    ListRow(
                        "Restore from backup file",
                        leading = { IconBadge(AppColors.Secondary) { Icon(Icons.Filled.Refresh, null, tint = Color.White) } },
                        onClick = { picker.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    )
                }
            }
            item {
                GroupedSection(title = "About") {
                    ListRow(
                        "Sales Cash Tracker", value = "v${BuildConfig.VERSION_NAME}",
                        leading = { IconBadge(AppColors.Secondary) { Icon(Icons.Filled.Info, null, tint = Color.White) } },
                    )
                    RowDivider(58)
                    ListRow(
                        "Open the website", chevron = true,
                        onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shahadmangaden.github.io/SalesCashTracker/"))) },
                    )
                    RowDivider()
                    ListRow(
                        "Download updates", subtitle = "Windows app & latest Android APK", chevron = true,
                        onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/shahadmangaden/SalesCashTracker/releases"))) },
                    )
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    editing?.let { field ->
        var text by remember(field) { mutableStateOf(when (field) { "company" -> settings.company; "user" -> settings.user; else -> settings.currency }) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(when (field) { "company" -> "Company"; "user" -> "Sales person"; else -> "Currency" }) },
            text = {
                OutlinedTextField(text, { text = it }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = text.trim()
                    Repository.saveSettings(
                        when (field) {
                            "company" -> settings.copy(company = v)
                            "user" -> settings.copy(user = v)
                            else -> settings.copy(currency = v)
                        },
                    )
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
            confirmButton = { TextButton(onClick = { confirmSignOut = false; Repository.signOut() }) { Text("Sign out", color = AppColors.RedText) } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
        )
    }

    restoreInfo?.let { r ->
        AlertDialog(
            onDismissRequest = { restoreInfo = null },
            title = { Text("Restore this backup?") },
            text = { Text("${r.bookings.size} sales and ${r.cash.size} cash entries will be added to your account (records with the same ID are replaced). They will appear on all your devices.") },
            confirmButton = {
                TextButton(onClick = {
                    Repository.restore(r.bookings, r.cash, r.lists, r.settings)
                    restoreInfo = null
                    Toast.makeText(ctx, "Backup restored", Toast.LENGTH_SHORT).show()
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { restoreInfo = null }) { Text("Cancel") } },
        )
    }
}

// ---------- Lists editor ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsEditorScreen(onBack: () -> Unit) {
    val lists by Repository.lists.collectAsStateWithLifecycle()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val sections: List<Triple<String, List<String>, (List<String>) -> Lists>> = listOf(
        Triple("Agents / sub-agents", lists.agents, { v: List<String> -> lists.copy(agents = v) }),
        Triple("Product categories", lists.categories, { v: List<String> -> lists.copy(categories = v) }),
        Triple("Customer types", lists.customerTypes, { v: List<String> -> lists.copy(customerTypes = v) }),
        Triple("Payment modes", lists.paymentModes, { v: List<String> -> lists.copy(paymentModes = v) }),
        Triple("Payment statuses", lists.paymentStatuses, { v: List<String> -> lists.copy(paymentStatuses = v) }),
        Triple("Booking statuses", lists.bookingStatuses, { v: List<String> -> lists.copy(bookingStatuses = v) }),
        Triple("Sectors / destinations", lists.countries, { v: List<String> -> lists.copy(countries = v) }),
    )
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = AppColors.GroupedBg,
        topBar = {
            LargeTopAppBar(
                title = { Text("Lists") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = AppColors.GroupedBg, scrolledContainerColor = Color(0xFFF9F9F9)),
            )
        },
    ) { pad ->
        LazyColumn(contentPadding = pad) {
            sections.forEach { (title, items, update) ->
                item(key = title) {
                    var adding by remember { mutableStateOf("") }
                    GroupedSection(title = "$title (${items.size})") {
                        items.forEachIndexed { i, v ->
                            if (i > 0) RowDivider()
                            ListRow(v, trailing = {
                                IconButton(onClick = { Repository.saveLists(update(items.filterIndexed { j, _ -> j != i })) }) {
                                    Icon(Icons.Filled.Close, "Remove", tint = AppColors.RedText)
                                }
                            })
                        }
                        RowDivider()
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                adding, { adding = it.uppercase() }, placeholder = { Text("Add new…") }, singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            )
                            TextButton(onClick = {
                                val v = adding.trim()
                                if (v.isNotEmpty() && v !in items) Repository.saveLists(update(items + v))
                                adding = ""
                            }) { Text("Add") }
                        }
                    }
                }
            }
            item(key = "docTypes") {
                var adding by remember { mutableStateOf("") }
                GroupedSection(title = "Visa / doc types", footer = "Days are used to work out the expiry date.") {
                    lists.docTypes.forEachIndexed { i, d ->
                        if (i > 0) RowDivider()
                        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(d.name, modifier = Modifier.weight(1f))
                            var days by remember(d.name, d.days) { mutableStateOf(d.days?.toString() ?: "") }
                            OutlinedTextField(
                                days,
                                { v ->
                                    days = v.filter { it.isDigit() }
                                    Repository.saveLists(lists.copy(docTypes = lists.docTypes.map { x -> if (x.name == d.name) x.copy(days = days.toIntOrNull()) else x }))
                                },
                                placeholder = { Text("days") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth(0.35f),
                            )
                            IconButton(onClick = { Repository.saveLists(lists.copy(docTypes = lists.docTypes.filterIndexed { j, _ -> j != i })) }) {
                                Icon(Icons.Filled.Close, "Remove", tint = AppColors.RedText)
                            }
                        }
                    }
                    RowDivider()
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            adding, { adding = it.uppercase() }, placeholder = { Text("Add new type…") }, singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        )
                        TextButton(onClick = {
                            val v = adding.trim()
                            if (v.isNotEmpty() && lists.docTypes.none { it.name == v }) Repository.saveLists(lists.copy(docTypes = lists.docTypes + DocType(v, null)))
                            adding = ""
                        }) { Text("Add") }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ---------- Export / backup ----------

private fun csvCell(v: Any?): String {
    val s = when (v) { null -> ""; is Double -> num(v); else -> v.toString() }
    return if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
}

private fun shareFiles(ctx: Context, files: List<File>, mime: String, title: String) {
    val auth = ctx.packageName + ".files"
    val uris = ArrayList(files.map { FileProvider.getUriForFile(ctx, auth, it) })
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply { type = mime; putExtra(Intent.EXTRA_STREAM, uris[0]) }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply { type = mime; putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris) }
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(intent, title))
}

private fun exportDir(ctx: Context): File = File(ctx.cacheDir, "exports").apply { mkdirs() }

fun exportCsv(ctx: Context, bookings: List<Booking>, cash: List<CashEntry>, lists: Lists) {
    val stamp = LocalDate.now().toString()
    val bHeader = listOf(
        "S.NO", "T. DATE", "CUSTOMER NAME", "CUSTOMER TYPE", "AGENCY / SUB-AGENT NAME", "PRODUCT CATEGORY",
        "PRODUCT / SERVICE DETAIL", "CONTACT", "SECTOR / DESTINATION", "FLIGHTS / REF", "PNR", "NET COST",
        "SELLING PRICE", "PROFIT", "PROFIT %", "PAYMENT STATUS", "PAYMENT MODE", "AMOUNT PAID", "BALANCE DUE",
        "SERVICE / TRVL DATE", "BOOKING STATUS", "ENTRY DATE", "EXPIRE DATE", "DAYS TO EXPIRY", "VISA / DOC TYPE", "NO. OF PAX",
    )
    val bf = File(exportDir(ctx), "Sales-$stamp.csv")
    bf.bufferedWriter().use { w ->
        w.write("﻿" + bHeader.joinToString(",") + "\n")
        bookings.sortedBy { it.date }.forEachIndexed { i, b ->
            val row = listOf(
                i + 1, b.date, b.customer, b.customerType, b.agent, b.category, b.detail, b.contact, b.sector, b.ref, b.pnr,
                b.netCost, b.sellingPrice, b.profit, b.margin?.let { String.format(java.util.Locale.ENGLISH, "%.1f%%", it * 100) },
                b.paymentStatus, b.paymentMode, b.amountPaid, b.balance, b.serviceDate, b.bookingStatus, b.entryDate,
                b.expireDate(lists.docTypes)?.toString(), b.daysLeft(lists.docTypes), b.docType, b.pax,
            )
            w.write(row.joinToString(",") { csvCell(it) } + "\n")
        }
    }
    val cf = File(exportDir(ctx), "CashPaid-$stamp.csv")
    cf.bufferedWriter().use { w ->
        w.write("﻿S.NO,CASH PAID DATE,AGENT / CUSTOMER,PAYMENT MODE,CASH PAID AMOUNT,REMARKS / RECEIPT REF\n")
        cash.sortedBy { it.date }.forEachIndexed { i, c ->
            w.write(listOf(i + 1, c.date, c.agent, c.mode, c.amount, c.remarks).joinToString(",") { csvCell(it) } + "\n")
        }
    }
    shareFiles(ctx, listOf(bf, cf), "text/csv", "Save or send the spreadsheets")
}

fun shareBackup(ctx: Context, bookings: List<Booking>, cash: List<CashEntry>, lists: Lists, settings: BusinessSettings) {
    val root = JSONObject()
    root.put("version", 1)
    root.put("settings", JSONObject(settings.toMap()))
    val l = JSONObject()
    lists.toMap().forEach { (k, v) ->
        if (k == "docTypes") l.put(k, JSONArray(lists.docTypes.map { d -> JSONObject().put("name", d.name).put("days", d.days ?: JSONObject.NULL) }))
        else l.put(k, JSONArray(v as List<*>))
    }
    root.put("lists", l)
    root.put("bookings", JSONArray(bookings.map { b -> JSONObject(b.toMap().mapValues { it.value ?: JSONObject.NULL }) }))
    root.put("cash", JSONArray(cash.map { c -> JSONObject(c.toMap().mapValues { it.value ?: JSONObject.NULL }) }))
    val f = File(exportDir(ctx), "tracker-backup-${LocalDate.now()}.json")
    f.writeText(root.toString(1))
    shareFiles(ctx, listOf(f), "application/json", "Save or send the backup")
}

data class RestoreData(val bookings: List<Booking>, val cash: List<CashEntry>, val lists: Lists?, val settings: BusinessSettings?)

private fun JSONObject.toMap(): Map<String, Any?> = keys().asSequence().associateWith { k ->
    when (val v = opt(k)) {
        JSONObject.NULL -> null
        is JSONObject -> v.toMap()
        is JSONArray -> (0 until v.length()).map { i -> v.opt(i).let { x -> if (x is JSONObject) x.toMap() else if (x == JSONObject.NULL) null else x } }
        else -> v
    }
}

fun parseBackup(ctx: Context, uri: Uri): RestoreData {
    val text = ctx.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
    val root = JSONObject(text)
    val b = root.getJSONArray("bookings")
    val bookings = (0 until b.length()).map { i -> b.getJSONObject(i).toMap() }
        .filter { it["deleted"] != true }
        .map { m -> Booking.fromMap((m["id"] as? String) ?: java.util.UUID.randomUUID().toString(), m) }
    val c = root.optJSONArray("cash") ?: JSONArray()
    val cash = (0 until c.length()).map { i -> c.getJSONObject(i).toMap() }
        .filter { it["deleted"] != true }
        .map { m -> CashEntry.fromMap((m["id"] as? String) ?: java.util.UUID.randomUUID().toString(), m) }
    @Suppress("UNCHECKED_CAST")
    val lists = root.optJSONObject("lists")?.let { Lists.fromMap(it.toMap()) }
    val settings = root.optJSONObject("settings")?.let { BusinessSettings.fromMap(it.toMap()) }
    return RestoreData(bookings, cash, lists, settings)
}

