package com.jash.salescashtracker.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.jash.salescashtracker.data.Booking
import com.jash.salescashtracker.data.BusinessSettings
import com.jash.salescashtracker.data.CashEntry
import com.jash.salescashtracker.data.Lists
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

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

