package com.jash.salescashtracker.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.data.Booking
import com.jash.salescashtracker.data.CashEntry
import com.jash.salescashtracker.data.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

// ---------- Sheet frame (iOS-style card sliding up) ----------

@Composable
fun SheetFrame(
    title: String,
    onCancel: () -> Unit,
    onSave: (() -> Unit)?,
    saveLabel: String = "Save",
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.94f)
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFF7F9FD), Color(0xFFEEF2F9))))
            .clickable(enabled = false) {},
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.width(38.dp).height(5.dp).clip(RoundedCornerShape(50)).background(Color(0x33000000)))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) { Text("Cancel", fontSize = 17.sp) }
            Text(title, modifier = Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Glass.Ink,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
            if (onSave != null) TextButton(onClick = onSave) { Text(saveLabel, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            else Spacer(Modifier.width(80.dp))
        }
        Column(
            Modifier.fillMaxWidth().weight(1f).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
            content = content,
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), color = Glass.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(options: List<String>, selected: String, onSelect: (String) -> Unit, color: Color = Glass.Tint) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o -> ChoiceChip(o, o == selected, { onSelect(if (o == selected) "" else o) }, color = color) }
    }
}

/** Today / Yesterday / pick a date (back-dating supported). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DateChips(iso: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val today = LocalDate.now().toString()
    val yesterday = LocalDate.now().minusDays(1).toString()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceChip("Today", iso == today, { onChange(today) })
        ChoiceChip("Yesterday", iso == yesterday, { onChange(yesterday) })
        ChoiceChip(if (iso != today && iso != yesterday && iso.isNotBlank()) "📅  ${fdate(iso)}" else "📅  Pick date", iso != today && iso != yesterday, { open = true })
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
private fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Glass.Tint),
        modifier = modifier.fillMaxWidth().height(54.dp),
    ) { Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
}

private fun parseNum(s: String): Double? = s.replace(",", "").trim().toDoubleOrNull()

/** Most used values first, then the rest of the list. */
private fun ranked(used: List<String>, all: List<String>, take: Int): List<String> {
    val freq = used.filter { it.isNotBlank() }.groupingBy { it }.eachCount()
    return (freq.entries.sortedByDescending { it.value }.map { it.key } + all).distinct().take(take)
}

// ---------- Quick add chooser ----------

@Composable
fun QuickAddSheet(state: AppState) {
    SheetFrame("Add", onCancel = { state.pop() }, onSave = null) {
        Spacer(Modifier.height(12.dp))
        GlassCard(onClick = { state.replaceTop(Route.EditSale(null)) }, padding = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconDot(Icons.Filled.ShoppingCart, Color(0xFF0A84FF), 48.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("New sale", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Glass.Ink)
                    Text("Ticket, visa, package, hotel, insurance…", color = Glass.Muted, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassCard(onClick = { state.replaceTop(Route.EditCash(null)) }, padding = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconDot(Icons.Filled.Check, Color(0xFF34C759), 48.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Cash paid", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Glass.Ink)
                    Text("Money received from an agent or customer", color = Glass.Muted, fontSize = 14.sp)
                }
            }
        }
    }
}

// ---------- Sale sheet ----------

@Composable
fun SaleSheet(original: Booking?, isCopy: Boolean, state: AppState) {
    val lists by Repository.lists.collectAsStateWithLifecycle()
    val all by Repository.bookings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
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
    var pay by remember { mutableStateOf(initialPay) }
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
    val hasDetails = listOf(b0.detail, b0.customerType, b0.sector, b0.contact, b0.ref, b0.pnr, b0.serviceDate, b0.bookingStatus, b0.docType, b0.entryDate).any { it.isNotBlank() }
    var showMore by remember { mutableStateOf(hasDetails) }
    var otherAgent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    val sell = parseNum(selling)
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

    fun save(again: Boolean) {
        if (customer.isBlank()) { error = "Enter the customer name"; return }
        if (sell == null) { error = "Enter the selling price"; return }
        val rec = build()
        Repository.saveBooking(rec)
        Toast.makeText(ctx, if (existing) "Sale updated" else "Sale saved", Toast.LENGTH_SHORT).show()
        if (again) state.replaceTop(Route.EditSale(Booking(date = rec.date, agent = rec.agent, category = rec.category, paymentMode = rec.paymentMode, customerType = rec.customerType)))
        else state.pop()
    }

    SheetFrame(if (existing) "Edit sale" else "New sale", onCancel = { state.pop() }, onSave = { save(false) }) {
        if (error.isNotEmpty()) Text(error, color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, start = 4.dp))

        Label("Date")
        DateChips(date) { date = it }

        Label("Customer")
        OutlinedTextField(
            customer, { customer = it; error = "" }, placeholder = { Text("Customer name") }, singleLine = true,
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        )

        Label("Agent")
        val agentOptions = ranked(all.map { it.agent }, lists.agents, 7)
        Chips(agentOptions + "Other…", if (otherAgent) "Other…" else agent, { v ->
            if (v == "Other…") { otherAgent = true; agent = "" } else { otherAgent = false; agent = v }
        }, color = Color(0xFF5856D6))
        if (otherAgent || (agent.isNotBlank() && agent !in agentOptions)) {
            Spacer(Modifier.height(8.dp))
            DropdownField("Agent name", agent, lists.agents, { agent = it }, editable = true)
        }

        Label("Product")
        val catOptions = ranked(all.map { it.category }, lists.categories, 8)
        Chips(catOptions, category, { category = it }, color = Color(0xFFFF9500))
        if (category.isNotBlank() && category !in catOptions) {
            Spacer(Modifier.height(8.dp)); DropdownField("Product", category, lists.categories, { category = it })
        }

        Label("Price")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(selling, { selling = it; error = "" }, label = { Text("Selling price") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold))
            OutlinedTextField(cost, { cost = it }, label = { Text("Net cost") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(fontSize = 20.sp))
        }
        if (live.profit != null) {
            Spacer(Modifier.height(8.dp))
            val p = live.profit ?: 0.0
            Text("Profit ${money(p)}   ·   ${pct(live.margin)}", color = if (p >= 0) Color(0xFF1A7F37) else Color(0xFFC62828),
                fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(start = 4.dp))
        }

        Label("Payment")
        Chips(listOf("Fully paid", "Partly paid", "Not paid"),
            when (pay) { "PAID" -> "Fully paid"; "PARTIAL" -> "Partly paid"; "PENDING" -> "Not paid"; else -> "" },
            { v -> pay = when (v) { "Fully paid" -> "PAID"; "Partly paid" -> "PARTIAL"; "Not paid" -> "PENDING"; else -> pay } },
            color = Color(0xFF34C759))
        if (pay == "PARTIAL") {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(partial, { partial = it }, label = { Text("Amount paid") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            val bal = live.balance
            if (bal != null) Text("Balance due ${money(bal)}", color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
        }
        if (pay != "PENDING") {
            Spacer(Modifier.height(10.dp))
            Chips(ranked(all.map { it.paymentMode }, lists.paymentModes, 5), mode, { mode = it.ifBlank { mode } }, color = Color(0xFF34C759))
        }

        Spacer(Modifier.height(18.dp))
        Text(
            if (showMore) "Hide details ▲" else "More details (PNR, sector, visa, contact…) ▼",
            color = Glass.Tint, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
            modifier = Modifier.clickable { showMore = !showMore }.padding(4.dp),
        )
        AnimatedVisibility(showMore) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DropdownField("Service detail", detail, all.map { it.detail }.filter { it.isNotBlank() }.distinct().sorted(), { detail = it }, editable = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DropdownField("Customer type", customerType, lists.customerTypes, { customerType = it }, Modifier.weight(1f))
                    FormField("Pax", pax, { pax = it }, Modifier.weight(0.5f), keyboard = KeyboardType.Number)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DropdownField("Sector", sector, lists.countries, { sector = it }, Modifier.weight(1f), editable = true)
                    FormField("PNR", pnr, { pnr = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormField("Flights / ref", ref, { ref = it }, Modifier.weight(1f))
                    FormField("Contact", contact, { contact = it }, Modifier.weight(1f), keyboard = KeyboardType.Phone)
                }
                DateField("Travel / service date", serviceDate, { serviceDate = it }, allowClear = true)
                DropdownField("Booking status", bookingStatus, lists.bookingStatuses, { bookingStatus = it })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DropdownField("Visa type", docType, lists.docTypes.map { it.name }, { docType = it }, Modifier.weight(1f))
                    DateField("Entry date", entryDate, { entryDate = it }, Modifier.weight(1f), allowClear = true)
                }
                live.expireDate(lists.docTypes)?.let { exp ->
                    val left = live.daysLeft(lists.docTypes) ?: 0L
                    Text("Visa expires ${fdate(exp.toString())} · $left days left",
                        color = if (left <= 7) Color(0xFFC62828) else Glass.Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        BigButton(if (existing) "Save changes" else "Save sale", { save(false) })
        if (!existing) {
            TextButton(onClick = { save(true) }, modifier = Modifier.fillMaxWidth()) { Text("Save & add another", fontSize = 16.sp) }
        } else {
            TextButton(onClick = {
                state.replaceTop(Route.EditSale(build().copy(id = UUID.randomUUID().toString(), customer = "", pnr = "", contact = "", createdAt = ""), isCopy = true))
            }, modifier = Modifier.fillMaxWidth()) { Text("Duplicate as new sale", fontSize = 16.sp) }
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete sale", color = Color(0xFFC62828), fontSize = 16.sp) }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this sale?") },
            text = { Text("${b0.customer} · ${fdate(b0.date)} · ${money(b0.sellingPrice)}\nIt will be removed on all your devices.") },
            confirmButton = { TextButton(onClick = { Repository.deleteBooking(b0.id); confirmDelete = false; state.pop() }) { Text("Delete", color = Color(0xFFC62828)) } },
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
    val existing = original != null && allCash.any { it.id == original.id }
    val c0 = original ?: CashEntry()

    var date by remember { mutableStateOf(c0.date.ifBlank { LocalDate.now().toString() }) }
    var amount by remember { mutableStateOf(if (existing) num(c0.amount) else "") }
    var agent by remember { mutableStateOf(c0.agent) }
    var mode by remember { mutableStateOf(c0.mode.ifBlank { "CASH" }) }
    var remarks by remember { mutableStateOf(c0.remarks) }
    var otherAgent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    fun save(again: Boolean) {
        val a = parseNum(amount)
        if (a == null) { error = "Enter the amount"; return }
        Repository.saveCash(c0.copy(id = if (existing) c0.id else UUID.randomUUID().toString(), date = date, amount = a,
            agent = agent.trim().uppercase(), mode = mode, remarks = remarks.trim()))
        Toast.makeText(ctx, "Cash paid saved", Toast.LENGTH_SHORT).show()
        if (again) state.replaceTop(Route.EditCash(CashEntry(date = date, agent = agent.trim().uppercase(), mode = mode))) else state.pop()
    }

    SheetFrame(if (existing) "Edit cash paid" else "Cash paid", onCancel = { state.pop() }, onSave = { save(false) }) {
        if (error.isNotEmpty()) Text(error, color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
        Label("Amount")
        OutlinedTextField(
            amount, { amount = it; error = "" }, placeholder = { Text("0.00", fontSize = 34.sp) }, singleLine = true,
            shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A7F37)),
        )
        Label("From agent / customer")
        val agentOptions = ranked(allCash.map { it.agent } + bookings.map { it.agent }, lists.agents, 7)
        Chips(agentOptions + "Other…", if (otherAgent) "Other…" else agent, { v ->
            if (v == "Other…") { otherAgent = true; agent = "" } else { otherAgent = false; agent = v }
        }, color = Color(0xFF5856D6))
        if (otherAgent || (agent.isNotBlank() && agent !in agentOptions)) {
            Spacer(Modifier.height(8.dp)); DropdownField("Agent name", agent, lists.agents, { agent = it }, editable = true)
        }
        Label("Date")
        DateChips(date) { date = it }
        Label("Paid by")
        Chips(ranked(allCash.map { it.mode }, lists.paymentModes, 6), mode, { mode = it.ifBlank { mode } }, color = Color(0xFF34C759))
        Label("Remarks (optional)")
        OutlinedTextField(remarks, { remarks = it }, placeholder = { Text("Receipt no., note…") },
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        BigButton(if (existing) "Save changes" else "Save cash paid", { save(false) })
        if (!existing) TextButton(onClick = { save(true) }, modifier = Modifier.fillMaxWidth()) { Text("Save & add another", fontSize = 16.sp) }
        else TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete entry", color = Color(0xFFC62828), fontSize = 16.sp) }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this cash entry?") },
            text = { Text("${c0.agent} · ${fdate(c0.date)} · ${money(c0.amount)}") },
            confirmButton = { TextButton(onClick = { Repository.deleteCash(c0.id); confirmDelete = false; state.pop() }) { Text("Delete", color = Color(0xFFC62828)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

