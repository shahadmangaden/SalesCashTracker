package com.jash.salescashtracker.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.BuildConfig
import com.jash.salescashtracker.R
import com.jash.salescashtracker.data.Booking
import com.jash.salescashtracker.data.DocType
import com.jash.salescashtracker.data.Lists
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.data.SyncState
import com.jash.salescashtracker.data.Totals
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------- Reports ----------

@Composable
fun ReportsScreen(state: AppState) {
    val ctx = LocalContext.current
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val cash by Repository.cash.collectAsStateWithLifecycle()
    val lists by Repository.lists.collectAsStateWithLifecycle()
    val settings by Repository.settings.collectAsStateWithLifecycle()
    val code = LocalCurrency.current
    val pb = bookings.filter { state.inPeriod(it.date) }
    val pc = cash.filter { state.inPeriod(it.date) }
    var monthMenu by remember { mutableStateOf(false) }

    val range = if (state.month == "all") "All time" else runCatching {
        val ym = YearMonth.parse(state.month)
        val last = if (ym == YearMonth.now()) LocalDate.now().dayOfMonth else ym.lengthOfMonth()
        "1 – $last ${ym.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH))}"
    }.getOrDefault("")

    IosPage(Scene.Reports) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                LargeTitle("Reports", range, modifier = Modifier.bleed(16.dp)) {
                    Box { GlassCircle(R.drawable.lc_calendar, "Choose a month") { monthMenu = true }; MonthMenu(state, monthMenu) { monthMenu = false } }
                    GlassCircle(R.drawable.lc_download, "Export to Excel") { exportCsv(ctx, bookings, cash, lists) }
                    AvatarButton(settings.user) { state.push(Route.Settings) }
                }
            }
            item { IosSegmented(listOf("agents" to "Agents", "products" to "Products", "visas" to "Visas"), state.reportsPage, { state.reportsPage = it }) }
            when (state.reportsPage) {
                "products" -> productsReport(pb, code, state)
                "visas" -> visasReport(bookings, lists.docTypes, ctx, state)
                else -> agentsReport(pb, pc, bookings, code, state)
            }
            item { TabBarSpacer() }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.agentsReport(
    pb: List<Booking>, pc: List<com.jash.salescashtracker.data.CashEntry>, allBookings: List<Booking>, code: String, state: AppState,
) {
    val key = { a: String -> a.ifBlank { "(no agent)" } }
    val names = (pb.map { key(it.agent) } + pc.map { key(it.agent) }).distinct()
    val rows = names.map { a -> a to Totals.of(pb.filter { key(it.agent) == a }, pc.filter { key(it.agent) == a }) }
        .sortedWith(compareByDescending<Pair<String, Totals>> { it.second.remaining }.thenByDescending { it.second.sales })
    val owing = rows.filter { it.second.remaining > 0.5 }
    val totalDue = owing.sumOf { it.second.remaining }
    // Six months of sales ending at the selected month
    val end = if (state.month == "all") YearMonth.now() else runCatching { YearMonth.parse(state.month) }.getOrDefault(YearMonth.now())
    val months = (5 downTo 0).map { end.minusMonths(it.toLong()) }
    val sales = months.map { ym -> ym.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)) to allBookings.filter { it.date.startsWith(ym.toString()) }.sumOf { it.sellingPrice ?: 0.0 } }

    item {
        Column(Modifier.fillMaxWidth().enter(0).iosCard().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    IT("To collect from agents", 13.sp, Glass.Muted, FontWeight.SemiBold)
                    IT(cur(totalDue, code), 28.sp, Glass.Ink, FontWeight.Bold)
                }
                IT("${owing.size} ${if (owing.size == 1) "agent" else "agents"}", 13.sp, Glass.Muted, FontWeight.SemiBold)
            }
            MonthBars(sales)
        }
    }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IosSection("Agent balances", "Highest first")
            if (rows.isEmpty()) Box(Modifier.fillMaxWidth().iosCard().padding(20.dp), contentAlignment = Alignment.Center) { IT("Nothing recorded in this period", 15.sp, Glass.Muted) }
            else InsetGroup(Modifier.enter(1)) {
                rows.forEachIndexed { i, (name, t) ->
                    val pct = if (t.sales > 0) ((t.sales - t.remaining) / t.sales).coerceIn(0.0, 1.0) else 1.0
                    IosRow(R.drawable.lc_building_2, nice(name), "${Math.round(pct * 100)}% collected · ${t.count} ${if (t.count == 1) "sale" else "sales"}",
                        if (t.remaining > 0.5) cur(t.remaining, code) else "Settled", if (t.remaining > 0.5) Glass.Due else Glass.Green,
                        if (t.remaining > 0.5) "due" else "", Glass.Muted, last = i == rows.lastIndex) {
                        state.agentFilter = if (name == "(no agent)") "" else name; state.tab = Tab.Sales
                    }
                }
            }
        }
    }
}

/** Six-month bar chart; the selected month is tinted and bars grow in. */
@Composable
private fun MonthBars(values: List<Pair<String, Double>>) {
    val max = (values.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    val grow = remember { androidx.compose.animation.core.Animatable(if (ThemeState.reduceMotion) 1f else 0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) { if (!ThemeState.reduceMotion) grow.animateTo(1f, androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
    Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, (label, v) ->
            val on = i == values.lastIndex
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val h = (92f * (v / max).toFloat() * grow.value).coerceAtLeast(4f)
                Box(Modifier.fillMaxWidth().height(h.dp).clip(RoundedCornerShape(8.dp)).background(if (on) Glass.Yellow else Glass.TileBg))
                IT(label, 12.sp, if (on) Glass.Ink else Glass.Muted, if (on) FontWeight.SemiBold else FontWeight.Medium)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.productsReport(pb: List<Booking>, code: String, state: AppState) {
    val groups = pb.groupBy { it.category.ifBlank { "Other" } }
        .map { (k, v) -> Triple(k, v.sumOf { it.sellingPrice ?: 0.0 }, v.size) }.sortedByDescending { it.second }
    val total = groups.sumOf { it.second }
    item {
        GlassCard(spacing = 12.dp) {
            T("Sales by product", 15.sp, Glass.Ink, FontWeight.Bold)
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Donut(groups.mapIndexed { i, g -> DonutColors[i % DonutColors.size] to g.second }, Modifier.size(200.dp))
                Column(Modifier.width(154.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    T("TOTAL SALES · $code", 11.sp, Glass.Muted)
                    T(amt(total), 29.sp, Glass.Ink, FontWeight.Bold)
                    T("${pb.size} ${if (pb.size == 1) "sale" else "sales"} ${if (state.month == "all") "in total" else "this month"}", 11.sp, Glass.Muted)
                }
            }
        }
    }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeading("Product breakdown", "Sales · $code")
            GlassCard(padding = PaddingValues(15.dp)) {
                if (groups.isEmpty()) T("No sales in this period", 13.sp, Glass.Muted)
                groups.forEachIndexed { i, (name, sales, _) ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 32.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(DonutColors[i % DonutColors.size]))
                        T(sentence(name), 12.sp, Glass.Ink, FontWeight.Medium, Modifier.weight(1f))
                        T(amt(sales), 12.sp, Glass.Ink, FontWeight.Bold)
                        T(if (total > 0) pctWhole(sales / total) else "–", 11.sp, Glass.Muted, modifier = Modifier.width(36.dp), align = TextAlign.End)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.visasReport(
    bookings: List<Booking>, docTypes: List<DocType>, ctx: android.content.Context, state: AppState,
) {
    val rows = bookings.filter { it.bookingStatus !in listOf("CANCELLED", "REFUND") }
        .mapNotNull { b -> b.daysLeft(docTypes)?.let { b to it } }.sortedBy { it.second }
    val upcoming = rows.filter { it.second >= 0 }
    val expired = rows.filter { it.second < 0 }.sortedByDescending { it.second }
    val week = upcoming.count { it.second <= 7 }

    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VisaStat("Expiring in 7 days", week, Glass.Amber, Modifier.weight(1f))
            VisaStat("Already expired", expired.size, Glass.Red, Modifier.weight(1f))
        }
    }
    if (rows.isEmpty()) item {
        GlassCard(spacing = 4.dp) {
            T("No visas tracked yet", 15.sp, Glass.Ink, FontWeight.Bold)
            T("Open a sale › More details › set Visa type (e.g. 30 DAYS) and Entry date.", 12.sp, Glass.Muted, maxLines = 3)
        }
    }
    if (upcoming.isNotEmpty()) item {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SectionHeading("Expiring soon", "Nearest first ↓")
            upcoming.take(30).forEach { (b, d) -> VisaCard(b, d, docTypes, ctx) { state.push(Route.EditSale(b)) } }
        }
    }
    if (expired.isNotEmpty()) item {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SectionHeading("Action overdue")
            expired.take(30).forEach { (b, d) -> VisaCard(b, d, docTypes, ctx) { state.push(Route.EditSale(b)) } }
        }
    }
}

@Composable
private fun VisaStat(label: String, count: Int, color: Color, modifier: Modifier) {
    Column(modifier.glass().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        T(label, 11.sp, Glass.Muted)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            T(count.toString(), 27.sp, color, FontWeight.Bold)
            T(if (count == 1) "passenger" else "passengers", 12.sp, color, modifier = Modifier.padding(bottom = 5.dp))
        }
    }
}

private val shortDay = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

@Composable
private fun VisaCard(b: Booking, d: Long, docTypes: List<DocType>, ctx: android.content.Context, onClick: () -> Unit) {
    val overdue = d < 0
    val fg = if (overdue) Glass.Red else Glass.Amber
    val bg = if (overdue) Glass.RedBg else Glass.AmberBg
    val exp = b.expireDate(docTypes)
    val days = docTypes.firstOrNull { it.name == b.docType }?.days
    val tail = listOf(b.ref, b.pnr).firstOrNull { it.length >= 4 }?.takeLast(4)
    GlassCard(padding = PaddingValues(14.dp), spacing = 10.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconTile(R.drawable.lc_contact_round, bg, fg, 35.dp, 12.dp, 18.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                T(nice(b.customer).ifBlank { "—" }, 14.sp, Glass.Ink, FontWeight.Bold)
                T(listOfNotNull(nice(b.agent).ifBlank { null }, tail?.let { "•••• $it" }).joinToString(" · "), 11.sp, Glass.Muted)
            }
            StatusPill(
                when { overdue -> "${-d} ${if (d == -1L) "day" else "days"} overdue"; d == 0L -> "Today"; d == 1L -> "Tomorrow"; else -> "$d days" },
                fg, bg,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(listOfNotNull(days?.let { "$it-day visa" } ?: sentence(b.docType), exp?.let { "Expires ${it.format(shortDay)}" }).joinToString(" · "),
                11.sp, Glass.Muted, modifier = Modifier.weight(1f))
            if (b.contact.isNotBlank()) {
                Row(
                    Modifier.clip(RoundedCornerShape(6.dp)).clickable {
                        val digits = b.contact.filter { it.isDigit() || it == '+' }
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))) }
                    }.padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Lc(R.drawable.lc_message_circle, Glass.Blue, 12.dp)
                    T("Contact", 11.sp, Glass.Blue, FontWeight.SemiBold)
                }
            }
        }
    }
}

// ---------- Settings ----------

@Composable
private fun SettingRow(@DrawableRes icon: Int, title: String, sub: String, divider: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 61.dp).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(icon, Glass.BlueBg, Glass.Blue, 34.dp, 11.dp, 18.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                T(title, 14.sp, Glass.Ink, FontWeight.SemiBold)
                T(sub, 11.sp, Glass.Muted)
            }
            Lc(R.drawable.lc_chevron_right, Glass.Muted, 16.dp)
        }
        if (divider) Box(Modifier.fillMaxWidth().height(0.6.dp).background(Glass.Divider))
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
    var dialog by remember { mutableStateOf<String?>(null) }
    var restoreInfo by remember { mutableStateOf<RestoreData?>(null) }
    val (syncLine, syncColor) = syncText()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching { parseBackup(ctx, uri) }
                .onSuccess { restoreInfo = it }
                .onFailure { Toast.makeText(ctx, "That file is not a tracker backup.", Toast.LENGTH_LONG).show() }
        }
    }
    fun open(url: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    IosPage(Scene.Settings) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Column(Modifier.statusBarsPadding().padding(top = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassCircle(R.drawable.lc_chevron_left, "Back") { state.pop() }
                        Spacer(Modifier.weight(1f))
                        GlassCircle(R.drawable.lc_circle_help, "Help") { dialog = "help" }
                    }
                    IT("Settings", 34.sp, Glass.Ink, FontWeight.Bold, Modifier.padding(top = 12.dp, start = 4.dp))
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().enter(0).iosCard().clickable { dialog = "account" }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(Glass.Yellow), contentAlignment = Alignment.Center) {
                        IT(initials(settings.user.ifBlank { user?.email ?: "" }), 20.sp, Glass.OnAccent, FontWeight.SemiBold)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        IT(nice(settings.user).ifBlank { "Your account" }, 20.sp, Glass.Ink, FontWeight.SemiBold)
                        IT(listOf(nice(settings.company), settings.currency.ifBlank { "AED" }).filter { it.isNotBlank() }.joinToString(" · "), 14.sp, Glass.Muted)
                        if (syncLine.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Lc(if (sync == SyncState.Synced) R.drawable.lc_cloud_check else if (sync == SyncState.Offline) R.drawable.lc_cloud_off else R.drawable.lc_refresh_cw, syncColor, 13.dp)
                            IT(syncLine, 12.sp, syncColor, FontWeight.SemiBold)
                        }
                    }
                    Lc(R.drawable.lc_chevron_right, Glass.Muted, 14.dp)
                }
            }
            item { Eyebrow("Appearance", modifier = Modifier.padding(start = 16.dp)) }
            item {
                Row(Modifier.enter(1), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Palettes.forEach { pal ->
                        val on = ThemeState.palette.id == pal.id
                        Column(
                            Modifier.weight(1f).iosCard(22.dp).clickable { ThemeState.set(ctx, pal) }.padding(vertical = 14.dp)
                                .semantics { selected = on },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // little phone preview: page colour, header tint, white rows
                            Column(
                                Modifier.size(width = 44.dp, height = 70.dp).clip(RoundedCornerShape(10.dp)).background(pal.bg.first())
                                    .border(1.dp, pal.divider, RoundedCornerShape(10.dp)),
                            ) {
                                Box(Modifier.fillMaxWidth().height(22.dp).background(pal.accent))
                                repeat(3) { Box(Modifier.padding(start = 6.dp, top = 5.dp).size(width = 32.dp, height = 7.dp).clip(RoundedCornerShape(3.dp)).background(Color.White)) }
                            }
                            IT(pal.name, 13.sp, Glass.Ink, if (on) FontWeight.SemiBold else FontWeight.Medium)
                            Box(
                                Modifier.size(20.dp).clip(CircleShape).background(if (on) pal.accent else Color.Transparent)
                                    .then(if (on) Modifier else Modifier.border(1.5.dp, Glass.Divider, CircleShape)),
                                contentAlignment = Alignment.Center,
                            ) { if (on) Lc(R.drawable.lc_check, pal.onAccent, 12.dp) }
                        }
                    }
                }
            }
            item { Eyebrow("Your data", modifier = Modifier.padding(start = 16.dp)) }
            item {
                InsetGroup(Modifier.enter(2)) {
                    IosRow(R.drawable.lc_building_2, "Business details",
                        listOf(nice(settings.company), nice(settings.user), settings.currency.ifBlank { "AED" }).filter { it.isNotBlank() }.joinToString(" · ")) { dialog = "business" }
                    IosRow(R.drawable.lc_cloud_check, "Backup file", "${bookings.size} sales · ${cash.size} payments", last = true) { dialog = "backup" }
                }
            }
            item {
                Box(Modifier.fillMaxWidth().iosCard().clickable { dialog = "signout" }.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
                    IT("Sign Out", 17.sp, Glass.Due)
                }
            }
            item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { IT("Version ${BuildConfig.VERSION_NAME}", 13.sp, Glass.Muted) } }
        }
    }

    when (dialog) {
        "account" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Your account") },
            text = {
                Text(
                    (user?.email ?: "") + "\n\n" + when (sync) {
                        SyncState.Synced -> "All records are synced with your phone, the website and the Windows app."
                        SyncState.Syncing -> "Syncing…"
                        SyncState.Offline -> "Offline — changes are kept and will sync when you're online."
                        SyncState.Error -> "Sync problem: $syncError"
                        else -> ""
                    },
                )
            },
            confirmButton = { TextButton(onClick = { dialog = "signout" }) { Text("Sign out", color = Glass.Red) } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Close") } },
        )
        "signout" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Sign out?") },
            text = { Text("Your records stay safe in your account. Sign in again to see them.") },
            confirmButton = { TextButton(onClick = { dialog = null; Repository.signOut() }) { Text("Sign out", color = Glass.Red) } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } },
        )
        "business" -> {
            var company by remember { mutableStateOf(settings.company) }
            var person by remember { mutableStateOf(settings.user) }
            var currency by remember { mutableStateOf(settings.currency) }
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text("Business details") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(company, { company = it }, label = { Text("Company") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
                        OutlinedTextField(person, { person = it }, label = { Text("Your name") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
                        OutlinedTextField(currency, { currency = it.uppercase().take(4) }, label = { Text("Currency") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { Repository.saveSettings(settings.copy(company = company.trim(), user = person.trim(), currency = currency.trim().ifBlank { "AED" })); dialog = null }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } },
            )
        }
        "backup" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Backup & restore") },
            text = { Text("Your records are already saved to your account online. You can also keep a backup file — it works with the website and the Windows app too.") },
            confirmButton = {
                TextButton(onClick = { dialog = null; shareBackup(ctx, bookings, cash, lists, settings) }) { Text("Save backup file") }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null; picker.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("Restore from file") }
            },
        )
        "theme" -> AlertDialog(
            onDismissRequest = { dialog = null },
            containerColor = Glass.Card,
            title = { Text("Theme") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Palettes.forEach { pal ->
                        val on = ThemeState.palette.id == pal.id
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .border(if (on) 2.dp else 1.dp, if (on) pal.primary else Glass.Divider, RoundedCornerShape(16.dp))
                                .clickable { ThemeState.set(ctx, pal); dialog = null }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // little preview: backdrop, card and accent
                            Box(Modifier.size(width = 54.dp, height = 40.dp).clip(RoundedCornerShape(10.dp))
                                .background(androidx.compose.ui.graphics.Brush.verticalGradient(pal.bg)), contentAlignment = Alignment.Center) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(Modifier.size(width = 18.dp, height = 22.dp).clip(RoundedCornerShape(5.dp)).background(pal.card))
                                    Box(Modifier.size(14.dp).clip(CircleShape).background(pal.accent))
                                }
                            }
                            Text(pal.name, modifier = Modifier.weight(1f), fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                            if (on) Lc(R.drawable.lc_check, pal.primary, 18.dp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Done") } },
        )
        "help" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Help & updates") },
            text = { Text("Use the same account on the website and the Windows app — everything stays in sync. Download the latest versions from the releases page.") },
            confirmButton = { TextButton(onClick = { dialog = null; open("https://github.com/shahadmangaden/SalesCashTracker/releases") }) { Text("Download updates") } },
            dismissButton = { TextButton(onClick = { dialog = null; open("https://shahadmangaden.github.io/SalesCashTracker/") }) { Text("Open website") } },
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
