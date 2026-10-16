package com.jash.salescashtracker.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.R
import com.jash.salescashtracker.data.Booking
import com.jash.salescashtracker.data.CashEntry
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.data.SyncState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Tab(val label: String, @DrawableRes val icon: Int) {
    Home("Home", R.drawable.lc_house),
    Sales("Sales", R.drawable.lc_tickets_plane),
    Cash("Cash", R.drawable.lc_wallet),
    Reports("Reports", R.drawable.lc_chart_no_axes_combined),
    Settings("Settings", R.drawable.lc_settings_2),
}

sealed class Route {
    data class EditSale(val booking: Booking?, val isCopy: Boolean = false, val nonce: Long = System.nanoTime()) : Route()
    data class EditCash(val entry: CashEntry?, val nonce: Long = System.nanoTime()) : Route()
    data object QuickAdd : Route()
}

/** UI-wide state shared by the tabs. */
class AppState {
    var tab by mutableStateOf(Tab.Home)
    var month by mutableStateOf(LocalDate.now().toString().take(7))   // "all" or "yyyy-MM"
    var agentFilter by mutableStateOf("")
    var reportsPage by mutableStateOf("agents")
    val routes = mutableStateListOf<Route>()
    fun push(r: Route) { routes.add(r) }
    fun pop() { if (routes.isNotEmpty()) routes.removeAt(routes.lastIndex) }
    fun replaceTop(r: Route) { pop(); push(r) }
    fun inPeriod(iso: String) = month == "all" || iso.startsWith(month)

    /** "October 2026" / "All time" */
    val periodLong: String get() = if (month == "all") "All time" else runCatching {
        YearMonth.parse(month).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
    }.getOrDefault(month)

    /** "Oct 2026" / "All time" */
    val periodShort: String get() = if (month == "all") "All time" else runCatching {
        YearMonth.parse(month).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH))
    }.getOrDefault(month)
}

@Composable
fun AppRoot() {
    val user by Repository.user.collectAsStateWithLifecycle()
    val sync by Repository.sync.collectAsStateWithLifecycle()
    when {
        sync == SyncState.Checking -> GlassBackground {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Glass.Blue)
        }
        user == null -> AuthScreen()
        else -> MainShell()
    }
}

@Composable
fun MainShell() {
    val state = remember { AppState() }
    val settings by Repository.settings.collectAsStateWithLifecycle()
    val top = state.routes.lastOrNull()

    BackHandler(enabled = top != null || state.tab != Tab.Home) {
        if (top != null) state.pop() else state.tab = Tab.Home
    }

    CompositionLocalProvider(LocalCurrency provides settings.currency.ifBlank { "AED" }) {
        GlassBackground {
            Box(Modifier.fillMaxSize().statusBarsPadding()) {
                when (state.tab) {
                    Tab.Home -> HomeScreen(state)
                    Tab.Sales -> SalesScreen(state)
                    Tab.Cash -> CashScreen(state)
                    Tab.Reports -> ReportsScreen(state)
                    Tab.Settings -> SettingsScreen(state)
                }
            }

            FloatingNav(state, Modifier.align(Alignment.BottomCenter))

            // Sheets
            AnimatedVisibility(visible = top != null, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier.fillMaxSize().background(Color(0x30253D62))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { state.pop() },
                )
            }
            AnimatedVisibility(
                visible = top != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                val r = top
                if (r != null) key(r) {
                    when (r) {
                        is Route.EditSale -> SaleSheet(r.booking, r.isCopy, state)
                        is Route.EditCash -> CashSheet(r.entry, state)
                        Route.QuickAdd -> QuickAddSheet(state)
                    }
                }
            }
        }
    }
}

/** Floating glass tab bar (302dp in the design) plus the 68dp round + button. */
@Composable
private fun FloatingNav(state: AppState, modifier: Modifier) {
    val pill = RoundedCornerShape(100.dp)
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp).height(68.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.weight(1f).fillMaxHeight()
                .shadow(14.dp, pill, ambientColor = Color(0xFF3B609D).copy(alpha = 0.14f), spotColor = Color(0xFF3B609D).copy(alpha = 0.22f))
                .clip(pill).background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.80f))))
                .border(1.dp, Brush.verticalGradient(listOf(Glass.Edge, Color.White.copy(alpha = 0.25f))), pill).padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Tab.entries.forEach { t ->
                val on = state.tab == t
                Column(
                    Modifier.weight(1f).fillMaxHeight().clip(pill)
                        .background(if (on) Glass.Yellow else Color.Transparent)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { state.tab = t },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Lc(t.icon, if (on) Glass.Ink else Glass.Muted, 21.dp, description = t.label)
                    T(t.label, 9.sp, if (on) Glass.Ink else Glass.Muted, if (on) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
        Box(
            Modifier.size(68.dp)
                .shadow(14.dp, CircleShape, ambientColor = Color(0xFF3B609D).copy(alpha = 0.14f), spotColor = Color(0xFF1F1F22).copy(alpha = 0.35f))
                .clip(CircleShape).background(Color.White.copy(alpha = 0.85f)).padding(6.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFF8D25A), Color(0xFFF2BE2E))))
                .clickable { state.push(Route.QuickAdd) },
            contentAlignment = Alignment.Center,
        ) { Lc(R.drawable.lc_plus, Glass.Ink, 28.dp, description = "Add") }
    }
}

/** Month chip with a picker menu ("Oct 2026 ⌄"). */
@Composable
fun PeriodChip(state: AppState, long: Boolean = false) {
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val cash by Repository.cash.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    val months = remember(bookings, cash) {
        (bookings.map { it.date.take(7) } + cash.map { it.date.take(7) } + LocalDate.now().toString().take(7))
            .filter { it.length == 7 }.distinct().sortedDescending()
    }
    Box {
        Choice((if (long) state.periodLong else state.periodShort) + " ⌄", false, { open = true }, large = long)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("All time") }, onClick = { state.month = "all"; open = false })
            months.forEach { m ->
                val label = runCatching { YearMonth.parse(m).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)) }.getOrDefault(m)
                DropdownMenuItem(text = { Text(label) }, onClick = { state.month = m; open = false })
            }
        }
    }
}

/** Bottom space so lists clear the floating tab bar. */
@Composable
fun TabBarSpacer() = Spacer(Modifier.height(110.dp).navigationBarsPadding())

/** Small sync status line (Settings). */
@Composable
fun syncText(): Pair<String, Color> {
    val sync by Repository.sync.collectAsStateWithLifecycle()
    return when (sync) {
        SyncState.Synced -> "Everything is safely backed up" to Glass.Green
        SyncState.Syncing -> "Syncing your changes…" to Glass.Blue
        SyncState.Offline -> "Offline — changes will sync when you're back online" to Glass.Amber
        SyncState.Error -> "Sync problem — tap your account for details" to Glass.Red
        else -> "" to Glass.Muted
    }
}
