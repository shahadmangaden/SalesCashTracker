package com.jash.salescashtracker.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.jash.salescashtracker.data.Booking
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.ui.theme.AppColors
import java.time.LocalDate
import java.util.UUID

private fun parseNum(s: String): Double? = s.replace(",", "").trim().toDoubleOrNull()
private fun numText(v: Double?): String = num(v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingForm(
    original: Booking?,
    isCopy: Boolean,
    onClose: () -> Unit,
    onSaveAndNew: (Booking) -> Unit,
) {
    val lists by Repository.lists.collectAsStateWithLifecycle()
    val allBookings by Repository.bookings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val existing = original != null && !isCopy && allBookings.any { it.id == original.id }
    val b0 = original ?: Booking()

    var date by remember { mutableStateOf(b0.date.ifBlank { LocalDate.now().toString() }) }
    var customer by remember { mutableStateOf(b0.customer) }
    var customerType by remember { mutableStateOf(b0.customerType) }
    var agent by remember { mutableStateOf(b0.agent) }
    var category by remember { mutableStateOf(b0.category) }
    var detail by remember { mutableStateOf(b0.detail) }
    var contact by remember { mutableStateOf(b0.contact) }
    var sector by remember { mutableStateOf(b0.sector) }
    var pax by remember { mutableStateOf(numText(b0.pax ?: 1.0)) }
    var ref by remember { mutableStateOf(b0.ref) }
    var pnr by remember { mutableStateOf(b0.pnr) }
    var serviceDate by remember { mutableStateOf(b0.serviceDate) }
    var bookingStatus by remember { mutableStateOf(b0.bookingStatus) }
    var netCost by remember { mutableStateOf(numText(b0.netCost)) }
    var selling by remember { mutableStateOf(numText(b0.sellingPrice)) }
    var paid by remember { mutableStateOf(numText(b0.amountPaid)) }
    var payStatus by remember { mutableStateOf(b0.paymentStatus) }
    var payMode by remember { mutableStateOf(b0.paymentMode) }
    var statusTouched by remember { mutableStateOf(existing && b0.paymentStatus.isNotBlank()) }
    var docType by remember { mutableStateOf(b0.docType) }
    var entryDate by remember { mutableStateOf(b0.entryDate) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    fun current(): Booking = b0.copy(
        id = if (existing) b0.id else (if (isCopy || original == null) UUID.randomUUID().toString() else b0.id),
        date = date, customer = customer.trim().uppercase(), customerType = customerType, agent = agent.trim().uppercase(),
        category = category, detail = detail.trim().uppercase(), contact = contact.trim(), sector = sector.trim().uppercase(),
        ref = ref.trim().uppercase(), pnr = pnr.trim().uppercase(), netCost = parseNum(netCost), sellingPrice = parseNum(selling),
        paymentStatus = payStatus, paymentMode = payMode, amountPaid = parseNum(paid), serviceDate = serviceDate,
        bookingStatus = bookingStatus, entryDate = entryDate, docType = docType, pax = parseNum(pax),
        createdAt = if (existing) b0.createdAt else "", updatedAt = b0.updatedAt,
    )

    // Payment status follows the amount paid until the user picks one
    fun autoStatus() {
        if (statusTouched) return
        val s = parseNum(selling) ?: return
        val p = parseNum(paid) ?: 0.0
        payStatus = when { p <= 0 -> "PENDING"; p >= s -> "PAID"; else -> "PARTIAL" }
    }

    val live = current()

    fun save(again: Boolean) {
        val rec = current()
        if (rec.customer.isBlank()) { error = "Customer name is required."; return }
        Repository.saveBooking(rec)
        Toast.makeText(ctx, if (existing) "Booking updated" else "Sale saved", Toast.LENGTH_SHORT).show()
        if (again) onSaveAndNew(Booking(date = rec.date, agent = rec.agent, customerType = rec.customerType, category = rec.category, paymentMode = rec.paymentMode, pax = 1.0))
        else onClose()
    }

    Scaffold(
        containerColor = AppColors.GroupedBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (existing) "Edit sale" else if (isCopy) "New sale (copy)" else "New sale", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onClose) { Text("Cancel") } },
                actions = { TextButton(onClick = { save(false) }) { Text("Save", fontWeight = FontWeight.Bold) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFF9F9F9)),
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
        ) {
            if (error.isNotEmpty()) {
                Text(error, color = AppColors.RedText, modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp))
            }
            GroupedSection(title = "Customer & product") {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateField("T. Date", date, { date = it })
                    FormField("Customer name", customer, { customer = it; error = "" }, required = true)
                    DropdownField("Customer type", customerType, lists.customerTypes, { customerType = it })
                    DropdownField("Agency / sub-agent", agent, lists.agents, { agent = it }, editable = true)
                    DropdownField("Product category", category, lists.categories, { category = it })
                    DropdownField(
                        "Product / service detail", detail,
                        allBookings.map { it.detail }.filter { it.isNotBlank() }.distinct().sorted(),
                        { detail = it }, editable = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DropdownField("Sector", sector, lists.countries, { sector = it }, Modifier.weight(1f), editable = true)
                        FormField("Pax", pax, { pax = it }, Modifier.weight(0.6f), keyboard = KeyboardType.Number)
                    }
                    FormField("Contact", contact, { contact = it }, keyboard = KeyboardType.Phone)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormField("Flights / ref", ref, { ref = it }, Modifier.weight(1f))
                        FormField("PNR", pnr, { pnr = it }, Modifier.weight(1f))
                    }
                    DateField("Service / travel date", serviceDate, { serviceDate = it }, allowClear = true)
                    DropdownField("Booking status", bookingStatus, lists.bookingStatuses, { bookingStatus = it })
                }
            }

            GroupedSection(title = "Price & payment") {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormField("Net cost", netCost, { netCost = it }, Modifier.weight(1f), keyboard = KeyboardType.Decimal)
                        FormField("Selling price", selling, { selling = it; autoStatus() }, Modifier.weight(1f), keyboard = KeyboardType.Decimal)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CalcField("Profit", money(live.profit), Modifier.weight(1f),
                            if ((live.profit ?: 0.0) < 0) AppColors.RedText else AppColors.GreenText)
                        CalcField("Profit %", pct(live.margin), Modifier.weight(1f))
                    }
                    FormField("Amount paid", paid, { paid = it; autoStatus() }, keyboard = KeyboardType.Decimal)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DropdownField("Payment status", payStatus, lists.paymentStatuses, { payStatus = it; statusTouched = true }, Modifier.weight(1f))
                        DropdownField("Payment mode", payMode, lists.paymentModes, { payMode = it }, Modifier.weight(1f))
                    }
                    CalcField("Balance due", money(live.balance), Modifier.fillMaxWidth(),
                        if ((live.balance ?: 0.0) > 0) AppColors.RedText else AppColors.Label)
                    OutlinedButton(
                        onClick = { paid = selling; payStatus = "PAID"; statusTouched = true },
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                    ) { Text("Mark fully paid") }
                }
            }

            GroupedSection(title = "Visa / document", footer = "Expiry = entry date + visa duration (set durations in More › Lists).") {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DropdownField("Visa / doc type", docType, lists.docTypes.map { it.name }, { docType = it })
                    DateField("Entry date", entryDate, { entryDate = it }, allowClear = true)
                    val exp = live.expireDate(lists.docTypes)
                    val left = live.daysLeft(lists.docTypes)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CalcField("Expire date", exp?.let { fshort(it) } ?: "", Modifier.weight(1f))
                        CalcField("Days to expiry", left?.let { "$it days" } ?: "", Modifier.weight(1f),
                            if (left != null && left <= 7) AppColors.RedText else AppColors.Label)
                    }
                }
            }

            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!existing) {
                    OutlinedButton(onClick = { save(true) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("Save & add another")
                    }
                } else {
                    OutlinedButton(
                        onClick = { onSaveAndNew(current().copy(id = UUID.randomUUID().toString(), customer = "", pnr = "", contact = "", createdAt = "")) },
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                    ) { Text("Duplicate as new sale") }
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Delete sale", color = AppColors.RedText)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this sale?") },
            text = { Text("${b0.customer} · ${fdate(b0.date)} · ${money(b0.sellingPrice)}. It will be removed on all your devices.") },
            confirmButton = {
                TextButton(onClick = {
                    Repository.deleteBooking(b0.id); confirmDelete = false
                    Toast.makeText(ctx, "Sale deleted", Toast.LENGTH_SHORT).show(); onClose()
                }) { Text("Delete", color = AppColors.RedText) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
