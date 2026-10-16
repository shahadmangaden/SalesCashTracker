package com.jash.salescashtracker.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

// Field names match the web/desktop app exactly so the same Firestore records are shared.

data class Booking(
    val id: String = UUID.randomUUID().toString(),
    val date: String = LocalDate.now().toString(),
    val customer: String = "",
    val customerType: String = "",
    val agent: String = "",
    val category: String = "",
    val detail: String = "",
    val contact: String = "",
    val sector: String = "",
    val ref: String = "",
    val pnr: String = "",
    val netCost: Double? = null,
    val sellingPrice: Double? = null,
    val paymentStatus: String = "",
    val paymentMode: String = "",
    val amountPaid: Double? = null,
    val serviceDate: String = "",
    val bookingStatus: String = "",
    val entryDate: String = "",
    val docType: String = "",
    val pax: Double? = 1.0,
    val createdAt: String = "",
    val updatedAt: String = "",
    val ts: Long = 0L,
) {
    val profit: Double? get() = sellingPrice?.let { it - (netCost ?: 0.0) }
    val margin: Double? get() = sellingPrice?.takeIf { it != 0.0 }?.let { (profit ?: 0.0) / it }
    val balance: Double? get() = sellingPrice?.let { it - (amountPaid ?: 0.0) }

    fun expireDate(docTypes: List<DocType>): LocalDate? {
        val days = docTypes.firstOrNull { it.name == docType }?.days ?: return null
        val entry = parseDate(entryDate) ?: return null
        return entry.plusDays(days.toLong())
    }

    fun daysLeft(docTypes: List<DocType>): Long? =
        expireDate(docTypes)?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id, "date" to date, "customer" to customer, "customerType" to customerType,
        "agent" to agent, "category" to category, "detail" to detail, "contact" to contact,
        "sector" to sector, "ref" to ref, "pnr" to pnr, "netCost" to netCost,
        "sellingPrice" to sellingPrice, "paymentStatus" to paymentStatus, "paymentMode" to paymentMode,
        "amountPaid" to amountPaid, "serviceDate" to serviceDate, "bookingStatus" to bookingStatus,
        "entryDate" to entryDate, "docType" to docType, "pax" to pax,
        "createdAt" to createdAt, "updatedAt" to updatedAt,
    )

    companion object {
        fun fromMap(id: String, m: Map<String, Any?>): Booking = Booking(
            id = id,
            date = m.str("date"),
            customer = m.str("customer"),
            customerType = m.str("customerType"),
            agent = m.str("agent"),
            category = m.str("category"),
            detail = m.str("detail"),
            contact = m.str("contact"),
            sector = m.str("sector"),
            ref = m.str("ref"),
            pnr = m.str("pnr"),
            netCost = m.num("netCost"),
            sellingPrice = m.num("sellingPrice"),
            paymentStatus = m.str("paymentStatus"),
            paymentMode = m.str("paymentMode"),
            amountPaid = m.num("amountPaid"),
            serviceDate = m.str("serviceDate"),
            bookingStatus = m.str("bookingStatus"),
            entryDate = m.str("entryDate"),
            docType = m.str("docType"),
            pax = m.num("pax"),
            createdAt = m.str("createdAt"),
            updatedAt = m.str("updatedAt"),
            ts = (m["_ts"] as? Number)?.toLong() ?: 0L,
        )
    }
}

data class CashEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String = LocalDate.now().toString(),
    val agent: String = "",
    val mode: String = "CASH",
    val amount: Double = 0.0,
    val remarks: String = "",
    val createdAt: String = "",
    val ts: Long = 0L,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id, "date" to date, "agent" to agent, "mode" to mode,
        "amount" to amount, "remarks" to remarks, "createdAt" to createdAt,
    )

    companion object {
        fun fromMap(id: String, m: Map<String, Any?>): CashEntry = CashEntry(
            id = id,
            date = m.str("date"),
            agent = m.str("agent"),
            mode = m.str("mode"),
            amount = m.num("amount") ?: 0.0,
            remarks = m.str("remarks"),
            createdAt = m.str("createdAt"),
            ts = (m["_ts"] as? Number)?.toLong() ?: 0L,
        )
    }
}

data class DocType(val name: String, val days: Int?)

data class Lists(
    val agents: List<String> = listOf("DIRECT / WALK-IN"),
    val customerTypes: List<String> = listOf("AGENT", "SUB-AGENT", "DIRECT CUSTOMER", "BUSINESS", "CORPORATE"),
    val categories: List<String> = listOf(
        "FLIGHT TICKET", "VISA SERVICE", "VISA CHANGE BY BUS", "KSA MULTIPPLE ENTRY",
        "TYPING CENTER / DOCUMENT SERVICE", "HOTEL", "TOUR PACKAGE", "GLOBAL VISA", "UMRAH PACKAGE",
        "HAJJ PACKAGE", "CAR RENTAL", "INSURANCE", "OTHER",
    ),
    val paymentStatuses: List<String> = listOf("PAID", "PARTIAL", "PENDING", "REFUNDED"),
    val paymentModes: List<String> = listOf(
        "CASH", "BANK TRANSFER", "CDM DEPOSIT", "CREDIT CARD", "DEBIT CARD",
        "ONLINE PAYMENT LINK", "WALLET PAYMENT", "CHEQUE",
    ),
    val bookingStatuses: List<String> = listOf(
        "SUBMITTED", "ENQUIRY", "HOLD", "OPEN", "ACTIVE", "USED", "COMPLETED", "CANCELLED", "REFUND",
    ),
    val docTypes: List<DocType> = listOf(
        DocType("14 DAYS", 14), DocType("30 DAYS", 30), DocType("60 DAYS", 60), DocType("90 DAYS", 90),
        DocType("120 DAYS", 120), DocType("180 DAYS", 180), DocType("1 YEAR / 365 DAYS", 365),
        DocType("2 YEARS / 730 DAYS", 730), DocType("TRANSIT 48 HOURS", 2), DocType("TRANSIT 96 HOURS", 4),
        DocType("SINGLE ENTRY", null), DocType("MULTIPLE ENTRY", null), DocType("TOURIST VISA", null),
        DocType("VISIT VISA", null), DocType("BUSINESS VISA", null), DocType("EMPLOYMENT VISA", null),
        DocType("RESIDENCE VISA", null), DocType("E-VISA", null), DocType("UMRAH VISA", null),
        DocType("DOCUMENT SERVICE", null), DocType("EMIRATES ID", null), DocType("N/A", null),
    ),
    val countries: List<String> = listOf(
        "UAE", "IND", "PAK", "BGD", "NPL", "LKA", "PHL", "KSA", "OMN", "QAT", "BHR", "KWT",
        "EGY", "JOR", "TUR", "GEO", "ARM", "AZE", "THA", "MYS", "IDN", "SGP", "GBR", "USA",
    ),
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "agents" to agents, "customerTypes" to customerTypes, "categories" to categories,
        "paymentStatuses" to paymentStatuses, "paymentModes" to paymentModes,
        "bookingStatuses" to bookingStatuses,
        "docTypes" to docTypes.map { mapOf("name" to it.name, "days" to it.days) },
        "countries" to countries,
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(m: Map<String, Any?>?): Lists {
            val d = Lists()
            if (m == null) return d
            fun strList(key: String, def: List<String>): List<String> =
                (m[key] as? List<*>)?.mapNotNull { it as? String } ?: def
            val docs = (m["docTypes"] as? List<*>)?.mapNotNull { item ->
                val x = item as? Map<String, Any?> ?: return@mapNotNull null
                val name = x["name"] as? String ?: return@mapNotNull null
                DocType(name, (x["days"] as? Number)?.toInt())
            } ?: d.docTypes
            return Lists(
                agents = strList("agents", d.agents),
                customerTypes = strList("customerTypes", d.customerTypes),
                categories = strList("categories", d.categories),
                paymentStatuses = strList("paymentStatuses", d.paymentStatuses),
                paymentModes = strList("paymentModes", d.paymentModes),
                bookingStatuses = strList("bookingStatuses", d.bookingStatuses),
                docTypes = docs,
                countries = strList("countries", d.countries),
            )
        }
    }
}

data class BusinessSettings(
    val company: String = "BAKHTAR TRAVELS",
    val user: String = "SAHAD M",
    val currency: String = "AED",
) {
    fun toMap(): Map<String, Any?> = mapOf("company" to company, "user" to user, "currency" to currency)

    companion object {
        fun fromMap(m: Map<String, Any?>?): BusinessSettings {
            val d = BusinessSettings()
            if (m == null) return d
            return BusinessSettings(
                company = (m["company"] as? String) ?: d.company,
                user = (m["user"] as? String) ?: d.user,
                currency = (m["currency"] as? String) ?: d.currency,
            )
        }
    }
}

data class Totals(
    val count: Int = 0,
    val sales: Double = 0.0,
    val cost: Double = 0.0,
    val profit: Double = 0.0,
    val paid: Double = 0.0,
    val balance: Double = 0.0,
    val pax: Double = 0.0,
    val cash: Double = 0.0,
    val cashCount: Int = 0,
) {
    val margin: Double get() = if (sales != 0.0) profit / sales else 0.0
    val remaining: Double get() = balance - cash

    companion object {
        fun of(bookings: List<Booking>, cash: List<CashEntry>): Totals {
            var t = Totals(count = bookings.size, cashCount = cash.size)
            for (b in bookings) {
                t = t.copy(
                    sales = t.sales + (b.sellingPrice ?: 0.0),
                    cost = t.cost + (b.netCost ?: 0.0),
                    profit = t.profit + (b.profit ?: 0.0),
                    paid = t.paid + (b.amountPaid ?: 0.0),
                    balance = t.balance + (b.balance ?: 0.0),
                    pax = t.pax + (b.pax ?: 0.0),
                )
            }
            return t.copy(cash = cash.sumOf { it.amount })
        }
    }
}

fun parseDate(s: String?): LocalDate? =
    if (s.isNullOrBlank()) null else runCatching { LocalDate.parse(s.take(10)) }.getOrNull()

private fun Map<String, Any?>.str(key: String): String = (this[key] as? String) ?: ""
private fun Map<String, Any?>.num(key: String): Double? = (this[key] as? Number)?.toDouble()
