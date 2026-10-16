package com.jash.salescashtracker.ui

import java.text.DecimalFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val moneyFmt = DecimalFormat("#,##0.00")
private val dayFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
private val shortFmt = DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.ENGLISH)
private val headerFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
private val monthFmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

fun money(v: Double?): String = if (v == null) "" else moneyFmt.format(v)
fun money0(v: Double?): String = if (v == null || v == 0.0) "–" else moneyFmt.format(v)
fun pct(v: Double?): String = if (v == null) "" else String.format(Locale.ENGLISH, "%.1f%%", v * 100)
fun num(v: Double?): String = when {
    v == null -> ""
    v == Math.floor(v) -> v.toLong().toString()
    else -> v.toString()
}

fun fdate(iso: String): String = runCatching { LocalDate.parse(iso.take(10)).format(dayFmt) }.getOrDefault(iso)
fun fshort(iso: String): String = runCatching { LocalDate.parse(iso.take(10)).format(shortFmt) }.getOrDefault(iso)
fun fshort(d: LocalDate): String = d.format(shortFmt)
fun dayHeader(iso: String): String = runCatching {
    val d = LocalDate.parse(iso.take(10))
    val today = LocalDate.now()
    when (d) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> if (d.year == today.year) d.format(headerFmt) else d.format(dayFmt)
    }
}.getOrDefault(iso)

fun monthName(key: String): String = runCatching { YearMonth.parse(key).format(monthFmt).uppercase() }.getOrDefault(key)
