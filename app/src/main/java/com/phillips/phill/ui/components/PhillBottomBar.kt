package com.phillips.phill.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.phillips.phill.navigation.CommsKey
import com.phillips.phill.navigation.MoreHubKey
import com.phillips.phill.navigation.DashboardKey
import com.phillips.phill.navigation.JobQueueKey
import com.phillips.phill.navigation.ScheduleKey

/**
 * Bottom navigation bar with 5 tabs per implementation plan Phase 0:
 * Dashboard, Schedule, Jobs, Comms, More
 *
 * "More" navigates to CustomerList as the entry point for the overflow menu.
 * Full "More" menu (Clients, Billing, Analytics, Settings) will be built on
 * the CustomerListScreen itself in Phase 3.
 */
@Composable
fun PhillBottomBar(
    backStack: List<NavKey>,
    onNavigate: (NavKey) -> Unit
) {
    val currentKey = backStack.lastOrNull()

    NavigationBar {
        BottomBarTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentKey == tab.key,
                onClick = { onNavigate(tab.key) },
                icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}

private enum class BottomBarTab(
    val key: NavKey,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD(DashboardKey, "Dashboard", Icons.Filled.Home),
    SCHEDULE(ScheduleKey, "Schedule", Icons.Filled.CalendarMonth),
    JOBS(JobQueueKey, "Jobs", Icons.Filled.Build),
    COMMS(CommsKey, "Comms", Icons.AutoMirrored.Filled.Message),
    MORE(MoreHubKey, "More", Icons.Filled.Menu)
}
