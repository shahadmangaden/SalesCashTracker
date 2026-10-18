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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
        user == null -> { SystemBarIcons(darkContent = !ThemeState.palette.darkBg, navDark = !ThemeState.palette.darkBg); AuthScreen() }
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

    val pal = ThemeState.palette
    // Status bar icons: every tab starts with the accent header
    val overLight = if (top == null) pal.accent.luminance() > 0.5f else !pal.darkBg
    SystemBarIcons(darkContent = overLight, navDark = !pal.darkBg)

    CompositionLocalProvider(LocalCurrency provides settings.currency.ifBlank { "AED" }) {
        GlassBackground {
            androidx.compose.animation.Crossfade(
                targetState = state.tab,
                animationSpec = androidx.compose.animation.core.tween(if (ThemeState.reduceMotion) 0 else 220),
                label = "tab",
            ) { tab ->
                Box(Modifier.fillMaxSize()) {
                    when (tab) {
                        Tab.Home -> HomeScreen(state)
                        Tab.Sales -> SalesScreen(state)
                        Tab.Cash -> CashScreen(state)
                        Tab.Reports -> ReportsScreen(state)
                        Tab.Settings -> SettingsScreen(state)
                    }
                }
            }

            FloatingNav(state, Modifier.align(Alignment.BottomCenter))

            // Sheets
            AnimatedVisibility(visible = top != null, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (pal.darkCard) 0.5f else 0.28f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { state.pop() },
                )
            }
            AnimatedVisibility(
                visible = top != null,
                // Standard navigation timing (~300 ms), gentle spring, no bounce; fade only with Reduce Motion
                enter = if (ThemeState.reduceMotion) fadeIn() else slideInVertically(androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 500f)) { it },
                exit = if (ThemeState.reduceMotion) fadeOut() else slideOutVertically(androidx.compose.animation.core.tween(240)) { it },
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

/** Floating glass tab bar from the mockups: 64dp pill, active item in a 52dp capsule, round accent + button. */
@Composable
private fun FloatingNav(state: AppState, modifier: Modifier) {
    val pal = ThemeState.palette
    val pill = RoundedCornerShape(32.dp)
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp).height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.weight(1f).fillMaxHeight()
                .shadow(if (pal.darkCard) 18.dp else 14.dp, pill, ambientColor = pal.shadow.copy(alpha = 0.16f), spotColor = pal.shadow.copy(alpha = if (pal.darkCard) 0.5f else 0.2f))
                .clip(pill).background(pal.navBg)
                .border(1.dp, pal.navEdge, pill).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { t ->
                val on = state.tab == t
                val bg by androidx.compose.animation.animateColorAsState(
                    if (on) pal.navActiveBg else Color.Transparent, androidx.compose.animation.core.tween(200), label = "navbg",
                )
                val fg by androidx.compose.animation.animateColorAsState(
                    if (on) pal.navActive else pal.navIdle, androidx.compose.animation.core.tween(200), label = "navfg",
                )
                Column(
                    Modifier.weight(1f).height(52.dp).pressable(scaleTo = 0.92f) { state.tab = t }
                        .semantics { selected = on }
                        .clip(RoundedCornerShape(26.dp)).background(bg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                ) {
                    Lc(t.icon, fg, 22.dp, description = null)
                    T(t.label, 11.sp, fg, if (on) FontWeight.Bold else FontWeight.SemiBold)
                }
            }
        }
        Box(
            Modifier.size(64.dp)
                .pressable(haptic = true, scaleTo = 0.9f) { state.push(Route.QuickAdd) }
                .shadow(16.dp, CircleShape, ambientColor = pal.accentShadow.copy(alpha = 0.3f), spotColor = pal.accentShadow.copy(alpha = 0.45f))
                .clip(CircleShape).background(pal.accent),
            contentAlignment = Alignment.Center,
        ) { Lc(R.drawable.lc_plus, pal.onAccent, 26.dp, description = "Add sale or payment") }
    }
}

/** Dark or light status / navigation bar icons to suit what is behind them. */
@Composable
fun SystemBarIcons(darkContent: Boolean, navDark: Boolean) {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        val c = androidx.core.view.WindowCompat.getInsetsController(window, view)
        c.isAppearanceLightStatusBars = darkContent
        c.isAppearanceLightNavigationBars = navDark
    }
}

/** Month chip with a picker menu ("Oct 2026 ⌄"). */
@Composable
fun PeriodChip(state: AppState, long: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    Box {
        Choice((if (long) state.periodLong else state.periodShort) + " ⌄", false, { open = true }, large = long)
        MonthMenu(state, open) { open = false }
    }
}

/** Themed month picker: rounded white card, months grouped by year, current choice highlighted. */
@Composable
fun MonthMenu(state: AppState, open: Boolean, onDismiss: () -> Unit) {
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val cash by Repository.cash.collectAsStateWithLifecycle()
    val months = remember(bookings, cash) {
        (bookings.map { it.date.take(7) } + cash.map { it.date.take(7) } + LocalDate.now().toString().take(7))
            .filter { it.length == 7 }.distinct().sortedDescending()
    }
    DropdownMenu(
        expanded = open, onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp), containerColor = Glass.Card, shadowElevation = 12.dp,
        modifier = Modifier.width(230.dp),
    ) {
        T("Show figures for", 11.sp, Glass.Muted, FontWeight.SemiBold, Modifier.padding(start = 16.dp, top = 4.dp, bottom = 6.dp))
        MonthItem("All time", "Every month", state.month == "all") { state.month = "all"; onDismiss() }
        months.forEach { m ->
            val ym = runCatching { YearMonth.parse(m) }.getOrNull()
            val label = ym?.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)) ?: m
            val sub = (ym?.year?.toString() ?: "") + if (ym == YearMonth.now()) " · this month" else ""
            MonthItem(label, sub, state.month == m) { state.month = m; onDismiss() }
        }
    }
}

@Composable
private fun MonthItem(title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp).clip(RoundedCornerShape(14.dp))
            .background(if (selected) Glass.YellowSoft else Color.Transparent).clickable(onClick = onClick)
            .heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(if (selected) Glass.Yellow else Glass.TileBg), contentAlignment = Alignment.Center) {
            Lc(R.drawable.lc_calendar, if (selected) Glass.OnAccent else Glass.Muted, 16.dp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            T(title, 14.sp, Glass.Ink, if (selected) FontWeight.Bold else FontWeight.Medium)
            if (sub.isNotBlank()) T(sub, 11.sp, Glass.Muted)
        }
        if (selected) Lc(R.drawable.lc_check, Glass.Ink, 16.dp)
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
