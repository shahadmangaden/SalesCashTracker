package com.jash.salescashtracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jash.salescashtracker.data.Booking
import com.jash.salescashtracker.data.CashEntry
import com.jash.salescashtracker.data.Repository
import com.jash.salescashtracker.data.SyncState
import com.jash.salescashtracker.ui.theme.AppColors
import java.time.LocalDate

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Dashboard", Icons.Filled.Home),
    Sales("Sales", Icons.Filled.ShoppingCart),
    Cash("Cash Paid", Icons.Filled.Star),
    Agents("Agents", Icons.AutoMirrored.Filled.List),
    More("More", Icons.Filled.Menu),
}

sealed class Route {
    data class EditBooking(val booking: Booking?, val isCopy: Boolean = false) : Route()
    data class EditCash(val entry: CashEntry?) : Route()
    data object ListsEditor : Route()
    data object Expiry : Route()
}

/** UI-wide state shared by the tabs. */
class AppState {
    var tab by mutableStateOf(Tab.Home)
    var month by mutableStateOf("all")              // "all" or "yyyy-MM"
    var agentFilter by mutableStateOf("")
    val routes = mutableStateListOf<Route>()
    fun push(r: Route) { routes.add(r) }
    fun pop() { if (routes.isNotEmpty()) routes.removeAt(routes.lastIndex) }
    fun inPeriod(iso: String) = month == "all" || iso.startsWith(month)
}

@Composable
fun AppRoot() {
    val user by Repository.user.collectAsStateWithLifecycle()
    val sync by Repository.sync.collectAsStateWithLifecycle()
    when {
        sync == SyncState.Checking -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        user == null -> AuthScreen()
        else -> MainScaffold()
    }
}

@Composable
fun MainScaffold() {
    val state = remember { AppState() }
    val current = state.routes.lastOrNull()

    BackHandler(enabled = current != null || state.tab != Tab.Home) {
        if (current != null) state.pop() else state.tab = Tab.Home
    }

    if (current != null) {
        // key() gives each opened form its own fresh state
        key(current) {
        when (current) {
            is Route.EditBooking -> BookingForm(current.booking, current.isCopy, onClose = { state.pop() }, onSaveAndNew = { next ->
                state.pop(); state.push(Route.EditBooking(next, isCopy = false))
            })
            is Route.EditCash -> CashForm(current.entry, onClose = { state.pop() })
            Route.ListsEditor -> ListsEditorScreen(onBack = { state.pop() })
            Route.Expiry -> ExpiryScreen(onBack = { state.pop() }, onOpen = { state.push(Route.EditBooking(it)) })
        }
        }
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = AppColors.GroupedBg,
        bottomBar = {
            NavigationBar(containerColor = Color(0xF7F9F9F9), tonalElevation = 0.dp) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = state.tab == t,
                        onClick = { state.tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppColors.Blue,
                            selectedTextColor = AppColors.Blue,
                            indicatorColor = Color(0xFFE3F0FF),
                            unselectedIconColor = AppColors.Secondary,
                            unselectedTextColor = AppColors.Secondary,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(bottom = pad.calculateBottomPadding())) {
            when (state.tab) {
                Tab.Home -> DashboardScreen(state)
                Tab.Sales -> SalesScreen(state)
                Tab.Cash -> CashScreen(state)
                Tab.Agents -> LedgerScreen(state)
                Tab.More -> MoreScreen(state)
            }
        }
    }
}

/** Large-title page used by every tab (title collapses on scroll, like iOS). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeTitlePage(
    title: String,
    actions: @Composable () -> Unit = {},
    floating: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        containerColor = AppColors.GroupedBg,
        floatingActionButton = floating,
        topBar = {
            LargeTopAppBar(
                title = { Text(title, maxLines = 1) },
                actions = { actions() },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = AppColors.GroupedBg,
                    scrolledContainerColor = Color(0xFFF9F9F9),
                ),
            )
        },
    ) { inner -> content(inner) }
}

/** Period (month) selector shown in the top bar of the data tabs. */
@Composable
fun PeriodButton(state: AppState) {
    val bookings by Repository.bookings.collectAsStateWithLifecycle()
    val cash by Repository.cash.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    val months = remember(bookings, cash) {
        (bookings.map { it.date.take(7) } + cash.map { it.date.take(7) } + LocalDate.now().toString().take(7))
            .filter { it.length == 7 }.distinct().sortedDescending()
    }
    Box {
        TextButton(onClick = { open = true }) {
            Icon(Icons.Filled.DateRange, null, modifier = Modifier.padding(end = 4.dp))
            Text(if (state.month == "all") "All months" else monthName(state.month))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("All months") }, onClick = { state.month = "all"; open = false })
            months.forEach { m ->
                DropdownMenuItem(text = { Text(monthName(m)) }, onClick = { state.month = m; open = false })
            }
        }
    }
}

/** Small sync status shown in top bars. */
@Composable
fun SyncBadge() {
    val sync by Repository.sync.collectAsStateWithLifecycle()
    val (text, color) = when (sync) {
        SyncState.Synced -> "Synced" to AppColors.GreenText
        SyncState.Syncing -> "Syncing…" to AppColors.Blue
        SyncState.Offline -> "Offline" to AppColors.OrangeText
        SyncState.Error -> "Sync error" to AppColors.RedText
        else -> "" to AppColors.Secondary
    }
    if (text.isNotEmpty()) {
        Icon(Icons.Filled.AccountCircle, null, tint = color, modifier = Modifier.padding(end = 2.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 12.dp))
    }
}
