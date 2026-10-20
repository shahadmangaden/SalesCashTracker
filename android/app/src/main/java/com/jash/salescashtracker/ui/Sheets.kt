package com.jash.salescashtracker.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.R
import com.jash.salescashtracker.data.Booking
import com.jash.salescashtracker.data.CashEntry
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.data.SUB_PRODUCTS
import com.jash.salescashtracker.ui.theme.Inter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val labelDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private fun labelDate(iso: String) = runCatching { LocalDate.parse(iso.take(10)).format(labelDate) }.getOrDefault(iso)

private fun parseNum(s: String): Double? = s.replace(",", "").trim().toDoubleOrNull()

/** Most used values first, then the rest of the list. */
private fun ranked(used: List<String>, all: List<String>, take: Int): List<String> {
    val freq = used.filter { it.isNotBlank() }.groupingBy { it }.eachCount()
    return (freq.entries.sortedByDescending { it.value }.map { it.key } + all).distinct().take(take)
}

/** Label + wrapping row of choice pills. Values are stored as-is, shown nicely. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceGroup(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    display: (String) -> String = ::sentence,
    extra: (@Composable () -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { o -> Choice(display(o), o == selected, { onSelect(o) }) }
            extra?.invoke()
        }
    }
}

/** "Sale date · 16 Oct 2026" with Today / Yesterday / Pick date. Back-dating supported. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DateChoices(label: String, iso: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val today = LocalDate.now().toString()
    val yesterday = LocalDate.now().minusDays(1).toString()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel("$label · ${labelDate(iso)}")
        DateStrip(iso, onChange) { open = true }
    }
    if (open) {
        val initial = runCatching { LocalDate.parse(iso.take(10)) }.getOrNull() ?: LocalDate.now()
        val st = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) { DatePicker(state = st) }
    }
}

@Composable
private fun TextAction(text: String, color: Color = Glass.Ink, modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    Box(modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        T(text, 14.sp, color, FontWeight.SemiBold)
    }
}

@Composable
private fun ErrorText(text: String) {
    if (text.isNotEmpty()) T(text, 12.sp, Glass.Red, FontWeight.SemiBold, maxLines = 2)
}

private const val MORE = "__MORE__"

/** Friendly names for the payment-mode cards. */
private fun modeLabel(k: String, expanded: Boolean): String = when (k) {
    MORE -> if (expanded) "Less" else "More"
    "CASH" -> "Cash on Hand"
    "BANK TRANSFER" -> "Direct Transfer"
    "CDM DEPOSIT" -> "Deposit / POS"
    "CREDIT CARD", "DEBIT CARD" -> sentence(k)
    else -> sentence(k)
}

private fun modeIcon(k: String, expanded: Boolean): Int = when {
    k == MORE -> if (expanded) R.drawable.lc_chevron_down else R.drawable.lc_layout_grid
    k == "CASH" -> R.drawable.lc_banknote
    k == "BANK TRANSFER" -> R.drawable.lc_landmark
    k == "CDM DEPOSIT" -> R.drawable.lc_receipt
    "CARD" in k -> R.drawable.lc_credit_card
    "LINK" in k || "ONLINE" in k -> R.drawable.lc_smartphone
    "WALLET" in k -> R.drawable.lc_wallet
    else -> R.drawable.lc_circle_dollar_sign
}

/** Opens WhatsApp (or any share target) with a short receipt for the sale. */
private fun sendReceipt(ctx: android.content.Context, b: Booking, code: String, company: String) {
    val text = buildString {
        appendLine("*${company.ifBlank { "Receipt" }}*")
        appendLine("Receipt · ${labelDate(b.date)}")
        appendLine()
        appendLine("Customer: ${nice(b.customer)}")
        if (b.category.isNotBlank()) appendLine("Service: ${sentence(b.category)}" + if (b.detail.isNotBlank()) " – ${nice(b.detail)}" else "")
        if (b.pnr.isNotBlank()) appendLine("PNR: ${b.pnr}")
        appendLine("Amount: $code ${amt(b.sellingPrice)}")
        appendLine("Paid: $code ${amt(b.amountPaid ?: 0.0)}")
        val bal = (b.balance ?: 0.0)
        if (bal > 0.5) appendLine("Balance: $code ${amt(bal)}")
        appendLine()
        append("Thank you!")
    }
    val digits = b.contact.filter { it.isDigit() }
    val intent = if (digits.length >= 8)
        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/$digits?text=" + android.net.Uri.encode(text)))
    else android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, text); setPackage("com.whatsapp") }
    try { ctx.startActivity(intent) } catch (e: Exception) {
        ctx.startActivity(android.content.Intent.createChooser(
            android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, text) }, "Send receipt"))
    }
}

// ---------- Quick add chooser ----------

@Composable
fun QuickAddSheet(state: AppState) {
    SheetFrame("Add", onClose = { state.pop() }, spacing = 12.dp) {
        InsetGroup {
            IosRow(R.drawable.lc_tickets_plane, "New sale", "Ticket, visa, Umrah, package, insurance…") { state.replaceTop(Route.EditSale(null)) }
            IosRow(R.drawable.lc_wallet, "Cash paid", "Money received from an agent or customer", last = true, iconBg = RingGreen) { state.replaceTop(Route.EditCash(null)) }
        }
    }
}

@Composable
private fun QuickOption(icon: Int, bg: Color, tint: Color, title: String, sub: String, onClick: () -> Unit) {
    GlassCard(padding = PaddingValues(16.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(icon, bg, tint, 46.dp, 15.dp, 24.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                T(title, 16.sp, Glass.Ink, FontWeight.Bold)
                T(sub, 12.sp, Glass.Muted)
            }
            Lc(R.drawable.lc_chevron_right, Glass.Muted, 18.dp)
        }
    }
}

// ---------- Sale sheet ----------

@Composable
fun SaleSheet(original: Booking?, isCopy: Boolean, state: AppState) {
    val lists by Repository.lists.collectAsStateWithLifecycle()
    val all by Repository.bookings.collectAsStateWithLifecycle()
    val settings by Repository.settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val code = LocalCurrency.current
    val existing = original != null && !isCopy && all.any { it.id == original.id }
    val b0 = original ?: Booking()

    var date by remember { mutableStateOf(b0.date.ifBlank { LocalDate.now().toString() }) }
    var customer by remember { mutableStateOf(b0.customer) }
    var agent by remember { mutableStateOf(b0.agent) }
    var category by remember { mutableStateOf(b0.category) }
    var selling by remember { mutableStateOf(num(b0.sellingPrice)) }
    var cost by remember { mutableStateOf(num(b0.netCost)) }
    val initialPay = when {
        b0.paymentStatus.isNotBlank() -> b0.paymentStatus
        b0.sellingPrice != null && (b0.amountPaid ?: 0.0) >= (b0.sellingPrice ?: 0.0) -> "PAID"
        (b0.amountPaid ?: 0.0) > 0 -> "PARTIAL"
        else -> if (existing) "PENDING" else "PAID"
    }
    // Two choices: paid in full, or a balance is due (with an optional amount paid now)
    var payUi by remember { mutableStateOf(if (initialPay == "PAID") "PAID" else "DUE") }
    var partial by remember { mutableStateOf(if (initialPay == "PARTIAL") num(b0.amountPaid) else "") }
    var mode by remember { mutableStateOf(b0.paymentMode.ifBlank { "CASH" }) }

    var detail by remember { mutableStateOf(b0.detail) }
    var customerType by remember { mutableStateOf(b0.customerType) }
    var sector by remember { mutableStateOf(b0.sector) }
    var pax by remember { mutableStateOf(num(b0.pax ?: 1.0)) }
    var contact by remember { mutableStateOf(b0.contact) }
    var ref by remember { mutableStateOf(b0.ref) }
    var pnr by remember { mutableStateOf(b0.pnr) }
    var serviceDate by remember { mutableStateOf(b0.serviceDate) }
    var bookingStatus by remember { mutableStateOf(b0.bookingStatus) }
    var docType by remember { mutableStateOf(b0.docType) }
    var entryDate by remember { mutableStateOf(b0.entryDate) }
    val hasDetails = listOf(b0.contact, b0.ref, b0.pnr, b0.serviceDate, b0.docType, b0.entryDate).any { it.isNotBlank() }
    var showMore by remember { mutableStateOf(hasDetails) }
    var addAgent by remember { mutableStateOf(false) }
    var allProducts by remember { mutableStateOf(false) }
    var otherSub by remember { mutableStateOf(false) }
    var allModes by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    val sell = parseNum(selling)
    val pay = if (payUi == "PAID") "PAID" else if ((parseNum(partial) ?: 0.0) > 0.0) "PARTIAL" else "PENDING"
    val paidAmount: Double? = when (pay) { "PAID" -> sell; "PARTIAL" -> parseNum(partial); else -> 0.0 }

    fun build(): Booking = b0.copy(
        id = if (existing) b0.id else UUID.randomUUID().toString(),
        date = date, customer = customer.trim().uppercase(), agent = agent.trim().uppercase(), category = category,
        sellingPrice = sell, netCost = parseNum(cost), paymentStatus = pay, amountPaid = paidAmount,
        paymentMode = if (pay == "PENDING") "" else mode,
        detail = detail.trim().uppercase(), customerType = customerType, sector = sector.trim().uppercase(), pax = parseNum(pax),
        contact = contact.trim(), ref = ref.trim().uppercase(), pnr = pnr.trim().uppercase(), serviceDate = serviceDate,
        bookingStatus = bookingStatus, docType = docType, entryDate = entryDate,
        createdAt = if (existing) b0.createdAt else "",
    )
    val live = build()

    fun save(again: Boolean, whatsapp: Boolean = false) {
        if (customer.isBlank()) { error = "Enter the customer name"; return }
        if (sell == null) { error = "Enter the selling price"; return }
        val rec = build()
        Repository.saveBooking(rec)
        if (whatsapp) sendReceipt(ctx, rec, code, settings.company)
        Toast.makeText(ctx, if (existing) "Sale updated" else "Sale saved", Toast.LENGTH_SHORT).show()
        if (again) state.replaceTop(Route.EditSale(Booking(date = rec.date, agent = rec.agent, category = rec.category, paymentMode = rec.paymentMode, customerType = rec.customerType)))
        else state.pop()
    }

    SheetFrame(if (existing) "Edit Sale" else "New Sale", onClose = { state.pop() }, spacing = 14.dp, onConfirm = { save(false) }, footer = {
        ErrorText(error)
        SubmitButton("Save & send WhatsApp receipt", null, { save(false, whatsapp = true) })
    }) {
        ErrorText(error)
        DateChoices("Sale date", date) { date = it }

        GlassInput("Customer name", customer, { customer = it; error = "" }, placeholder = "Customer name",
            leading = R.drawable.lc_user_round, caps = KeyboardCapitalization.Words)

        // Agent: scrollable chips with a dashed "+ Add"
        val agentOptions = ranked(all.map { it.agent }, lists.agents, 8).let { if (agent.isNotBlank() && agent !in it && !addAgent) listOf(agent) + it else it }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Agent")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                agentOptions.forEach { o -> Choice(nice(o), o == agent && !addAgent, { agent = if (agent == o) "" else o; addAgent = false }, large = true) }
                DashedChip(if (addAgent) "Cancel" else "+ Add") { addAgent = !addAgent; if (addAgent) agent = "" }
            }
        }
        AnimatedVisibility(addAgent) { GlassInput(null, agent, { agent = it }, placeholder = "New agent name", caps = KeyboardCapitalization.Characters) }

        // Products: 2-column bento grid
        val productOptions = if (allProducts) lists.categories else
            ranked(all.map { it.category }, lists.categories, 3).let { if (category.isNotBlank() && category !in it) listOf(category) + it.take(2) else it }
        FieldLabel("Product")
        ProductGrid(productOptions, category, { if (category != it) { category = it; detail = ""; otherSub = false } }) { m ->
            val shape = RoundedCornerShape(20.dp)
            Column(
                m.iosCard(20.dp).clickable { allProducts = !allProducts }.padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            ) {
                Lc(if (allProducts) R.drawable.lc_chevron_down else R.drawable.lc_plus, Glass.Ink, 22.dp, if (allProducts) Modifier.rotate(180f) else Modifier)
                IT(if (allProducts) "Less" else "More", 13.sp, Glass.Ink, FontWeight.Medium)
            }
        }

        // Sub-product (stored in the sale's detail field)
        AnimatedVisibility(category.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val used = all.filter { it.category == category }.map { it.detail }
                val subs = ranked(used, SUB_PRODUCTS[category.uppercase()] ?: emptyList(), 8)
                    .let { if (detail.isNotBlank() && detail !in it && !otherSub) listOf(detail) + it else it }
                ChoiceGroup("${sentence(category)} · type", subs, detail, { detail = if (detail == it) "" else it; otherSub = false }) {
                    DashedChip(if (otherSub) "Cancel" else "+ Other") { otherSub = !otherSub; if (otherSub) detail = "" }
                }
                if (otherSub) GlassInput(null, detail, { detail = it }, placeholder = "Type the service, e.g. DXB-COK RETURN", caps = KeyboardCapitalization.Characters)
            }
        }

        // Price + live profit
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassInput("Selling price · $code", selling, { selling = it; error = "" }, Modifier.weight(1f), placeholder = "0", keyboard = KeyboardType.Decimal, bold = true)
            GlassInput("Net cost · $code", cost, { cost = it }, Modifier.weight(1f), placeholder = "0", keyboard = KeyboardType.Decimal)
        }
        AnimatedVisibility(live.profit != null) {
            val p = live.profit ?: 0.0
            val good = p >= 0
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Row(
                    Modifier.clip(RoundedCornerShape(100.dp)).background(if (good) Glass.GreenBg else Glass.RedBg).padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (good) Glass.Green else Glass.Red))
                    Spacer(Modifier.width(8.dp))
                    T("Profit: ${if (good) "+" else "−"}${amt(Math.abs(p))} $code (${pctWhole(live.margin ?: 0.0)})", 13.sp, if (good) Glass.Green else Glass.Red, FontWeight.Bold)
                }
            }
        }

        // Payment: paid in full, or balance due
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Payment")
            IosSegmented(listOf("PAID" to "Paid in full", "DUE" to "Balance due"), payUi, { payUi = it })
        }
        AnimatedVisibility(payUi == "DUE") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                GlassInput("Paid now · optional", partial, { partial = it }, Modifier.weight(1f), placeholder = "0", keyboard = KeyboardType.Decimal)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Pending balance")
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(15.dp)).background(Glass.AmberBg).padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) { T("$code ${amt((live.balance ?: 0.0).coerceAtLeast(0.0))}", 15.sp, Glass.Amber, FontWeight.Bold) }
                }
            }
        }

        // Payment mode: icon cards
        AnimatedVisibility(pay != "PENDING") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Payment mode")
                val main = listOf("CASH", "BANK TRANSFER", "CDM DEPOSIT")
                val rest = lists.paymentModes.filter { it !in main }
                val keys = main + (if (allModes) rest else listOfNotNull(mode.takeIf { it.isNotBlank() && it !in main })) + MORE
                ModeCards(keys.distinct().map { it to modeIcon(it, allModes) }, mode, { k -> if (k == MORE) allModes = !allModes else mode = k }) { modeLabel(it, allModes) }
            }
        }

        // More details accordion
        val shapeMd = RoundedCornerShape(20.dp)
        Column(
            Modifier.fillMaxWidth().softShadow(20.dp, 4.dp, 12.dp, 0.08f, 0.9f).clip(shapeMd).background(Glass.Card).border(1.dp, Glass.Edge, shapeMd)
                .clickable { showMore = !showMore }.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Glass.TileBg), contentAlignment = Alignment.Center) { Lc(R.drawable.lc_file_text, Glass.Ink, 17.dp) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    T("More details", 14.sp, Glass.Ink, FontWeight.Bold)
                    T("Passport, PNR, travel date, visa", 11.sp, Glass.Muted)
                }
                Lc(R.drawable.lc_chevron_down, Glass.Muted, 18.dp, Modifier.rotate(if (showMore) 180f else 0f))
            }
            if (!showMore) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Passport expiry", "Travel date", "PNR").forEach { StatusPill(it, Glass.Ink, Glass.TileBg, 11.sp) }
            }
        }
        AnimatedVisibility(showMore) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormField("PNR", pnr, { pnr = it }, Modifier.weight(1f))
                    FormField("Pax", pax, { pax = it }, Modifier.weight(0.5f), keyboard = KeyboardType.Number)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormField("Passport / ref", ref, { ref = it }, Modifier.weight(1f))
                    FormField("Contact", contact, { contact = it }, Modifier.weight(1f), keyboard = KeyboardType.Phone)
                }
                DateField("Travel / service date", serviceDate, { serviceDate = it }, allowClear = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DropdownField("Visa type", docType, lists.docTypes.map { it.name }, { docType = it }, Modifier.weight(1f))
                    DateField("Entry date", entryDate, { entryDate = it }, Modifier.weight(1f), allowClear = true)
                }
                live.expireDate(lists.docTypes)?.let { exp ->
                    val left = live.daysLeft(lists.docTypes) ?: 0L
                    T("Visa expires ${labelDate(exp.toString())} · " + if (left < 0) "${-left} days ago" else "$left days left",
                        12.sp, if (left <= 7) Glass.Red else Glass.Ink, FontWeight.SemiBold)
                }
            }
        }

        if (existing) {
            TextAction("Duplicate as new sale") {
                state.replaceTop(Route.EditSale(build().copy(id = UUID.randomUUID().toString(), customer = "", pnr = "", contact = "", createdAt = ""), isCopy = true))
            }
            TextAction("Delete sale", Glass.Red) { confirmDelete = true }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this sale?") },
            text = { Text("${nice(b0.customer)} · ${labelDate(b0.date)} · ${cur(b0.sellingPrice, code)}\nIt will be removed on all your devices.") },
            confirmButton = { TextButton(onClick = { Repository.deleteBooking(b0.id); confirmDelete = false; state.pop() }) { Text("Delete", color = Glass.Red) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

// ---------- Cash sheet ----------

@Composable
fun CashSheet(original: CashEntry?, state: AppState) {
    val lists by Repository.lists.collectAsStateWithLifecycle()
    val allCash by Repository.cash.collectAsStateWithLifecycle()
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val code = LocalCurrency.current
    val existing = original != null && allCash.any { it.id == original.id }
    val c0 = original ?: CashEntry()

    var date by remember { mutableStateOf(c0.date.ifBlank { LocalDate.now().toString() }) }
    var amount by remember { mutableStateOf(if (existing) num(c0.amount) else "") }
    var agent by remember { mutableStateOf(c0.agent) }
    var mode by remember { mutableStateOf(c0.mode.ifBlank { "CASH" }) }
    var remarks by remember { mutableStateOf(c0.remarks) }
    var addAgent by remember { mutableStateOf(false) }
    var allModes by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    fun save(again: Boolean) {
        val a = parseNum(amount)
        if (a == null) { error = "Enter the amount"; return }
        Repository.saveCash(c0.copy(id = if (existing) c0.id else UUID.randomUUID().toString(), date = date, amount = a,
            agent = agent.trim().uppercase(), mode = mode, remarks = remarks.trim()))
        Toast.makeText(ctx, "Payment saved", Toast.LENGTH_SHORT).show()
        if (again) state.replaceTop(Route.EditCash(CashEntry(date = date, agent = agent.trim().uppercase(), mode = mode))) else state.pop()
    }

    SheetFrame(if (existing) "Edit Payment" else "Cash Paid", onClose = { state.pop() }, spacing = 16.dp, onConfirm = { save(false) }, footer = {
        SubmitButton(if (existing) "Save changes" else "Save payment", R.drawable.lc_check, { save(false) })
    }) {
        Column(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            T("AMOUNT · $code", 12.sp, Glass.Muted)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                if (amount.isEmpty()) T("0", 44.sp, Glass.Muted.copy(alpha = 0.45f), FontWeight.Bold)
                BasicTextField(
                    amount, { amount = it; error = "" }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = Inter, fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Glass.Ink, textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(Glass.Blue),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            ErrorText(error)
        }

        val agentOptions = ranked(allCash.map { it.agent } + bookings.map { it.agent }, lists.agents, 5)
            .let { if (agent.isNotBlank() && agent !in it) listOf(agent) + it else it }
        ChoiceGroup("Paid to agent", agentOptions, agent, { agent = it; addAgent = false }, display = ::nice) {
            DashedChip(if (addAgent) "Cancel" else "+ Add") { addAgent = !addAgent; if (addAgent) agent = "" }
        }
        if (addAgent) GlassInput(null, agent, { agent = it }, placeholder = "Agent name", caps = KeyboardCapitalization.Characters)

        DateChoices("Payment date", date) { date = it }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Payment mode")
            val main = listOf("CASH", "BANK TRANSFER", "CDM DEPOSIT")
            val rest = lists.paymentModes.filter { it !in main }
            val keys = main + (if (allModes) rest else listOfNotNull(mode.takeIf { it.isNotBlank() && it !in main })) + MORE
            ModeCards(keys.distinct().map { it to modeIcon(it, allModes) }, mode, { k -> if (k == MORE) allModes = !allModes else mode = k }) { modeLabel(it, allModes) }
        }

        GlassInput("Remarks · optional", remarks, { remarks = it }, placeholder = "Receipt no., note…", leading = R.drawable.lc_pencil_line,
            caps = KeyboardCapitalization.Sentences)

        if (existing) TextAction("Delete payment", Glass.Red) { confirmDelete = true }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this payment?") },
            text = { Text("${nice(c0.agent)} · ${labelDate(c0.date)} · ${cur(c0.amount, code)}") },
            confirmButton = { TextButton(onClick = { Repository.deleteCash(c0.id); confirmDelete = false; state.pop() }) { Text("Delete", color = Glass.Red) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
