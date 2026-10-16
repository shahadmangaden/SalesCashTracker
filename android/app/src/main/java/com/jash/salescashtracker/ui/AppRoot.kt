package com.jash.salescashtracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.data.Booking
import com.jash.salescashtracker.data.CashEntry
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.data.SyncState
import java.time.LocalDate

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Sales("Sales", Icons.Filled.ShoppingCart),
    Cash("Cash", Icons.Filled.Star),
    Reports("Reports", Icons.AutoMirrored.Filled.List),
    Settings("Settings", Icons.Filled.Settings),
}

sealed class Route {
    data class EditSale(val booking: Booking?, val isCopy: Boolean = false, val nonce: Long = System.nanoTime()) : Route()
    data class EditCash(val entry: CashEntry?, val nonce: Long = System.nanoTime()) : Route()
    data object QuickAdd : Route()
    data object ListsEditor : Route()
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
}

@Composable
fun AppRoot() {
    val user by Repository.user.collectAsStateWithLifecycle()
    val sync by Repository.sync.collectAsStateWithLifecycle()
    when {
        sync == SyncState.Checking -> GlassBackground {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        user == null -> AuthScreen()
        else -> MainShell()
    }
}

@Composable
fun MainShell() {
    val state = remember { AppState() }
    val top = state.routes.lastOrNull()

    BackHandler(enabled = top != null || state.tab != Tab.Home) {
        if (top != null) state.pop() else state.tab = Tab.Home
    }

    GlassBackground {
        // Tab content
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            when (state.tab) {
                Tab.Home -> HomeScreen(state)
                Tab.Sales -> SalesScreen(state)
                Tab.Cash -> CashScreen(state)
                Tab.Reports -> ReportsScreen(state)
                Tab.Settings -> SettingsScreen(state)
            }
        }

        // Floating glass tab bar + round add button (iOS 26 style)
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.weight(1f).height(64.dp).glass(RoundedCornerShape(32.dp), strong = true, elevation = 18.dp).padding(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tab.entries.forEach { t ->
                    val on = state.tab == t
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(28.dp))
                            .background(if (on) Color(0x1F0A84FF) else Color.Transparent)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { state.tab = t },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(t.icon, t.label, tint = if (on) Glass.Tint else Glass.Muted, modifier = Modifier.size(22.dp))
                        Text(t.label, fontSize = 10.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = if (on) Glass.Tint else Glass.Muted, maxLines = 1)
                    }
                }
            }
            Box(
                Modifier.size(64.dp).glass(CircleShape, strong = true, elevation = 18.dp).padding(6.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF3D9BFF), Color(0xFF0A62FF))))
                    .clickable { state.push(Route.QuickAdd) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Add, "Add", tint = Color.White, modifier = Modifier.size(30.dp)) }
        }

        // Sheets
        AnimatedVisibility(visible = top != null, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color(0x66101828)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { state.pop() })
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
                    Route.ListsEditor -> ListsEditorSheet(state)
                }
            }
        }
    }
}

/** Glass "period" pill with month menu, used in page headers. */
@Composable
fun PeriodPill(state: AppState) {
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val cash by Repository.cash.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    val months = remember(bookings, cash) {
        (bookings.map { it.date.take(7) } + cash.map { it.date.take(7) } + LocalDate.now().toString().take(7))
            .filter { it.length == 7 }.distinct().sortedDescending()
    }
    Box {
        GlassPill(if (state.month == "all") "All time" else monthName(state.month), { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("All time") }, onClick = { state.month = "all"; open = false })
            months.forEach { m -> DropdownMenuItem(text = { Text(monthName(m)) }, onClick = { state.month = m; open = false }) }
        }
    }
}

/** Shared bottom spacing so lists clear the floating tab bar. */
@Composable
fun TabBarSpacer() = Spacer(Modifier.height(120.dp).navigationBarsPadding())

/** Sync dot + text for headers. */
@Composable
fun SyncChip() {
    val sync by Repository.sync.collectAsStateWithLifecycle()
    val (text, color) = when (sync) {
        SyncState.Synced -> "Synced" to Color(0xFF1A7F37)
        SyncState.Syncing -> "Syncing" to Glass.Tint
        SyncState.Offline -> "Offline" to Color(0xFFB25E00)
        SyncState.Error -> "Sync error" to Color(0xFFC62828)
        else -> "" to Glass.Muted
    }
    if (text.isNotEmpty()) {
        Row(
            Modifier.height(30.dp).glass(RoundedCornerShape(50), strong = true, elevation = 4.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.size(6.dp))
            Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
