package com.walcker.match.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import com.walcker.games.features.ui.notifications.NotificationHistoryStep
import com.walcker.identity.api.SessionHolder
import com.walcker.match.app.strings.AppShellStrings
import com.walcker.match.app.strings.rememberAppShellStrings
import com.walcker.match.cedar.components.MatchBottomBar
import com.walcker.match.cedar.components.MatchBottomBarTab
import com.walcker.match.navigator.DeepLink
import com.walcker.match.navigator.DeepLinkCoordinator
import com.walcker.match.navigator.GamesDestination
import com.walcker.match.navigator.IdentityDestination
import com.walcker.match.navigator.MainTab
import com.walcker.match.navigator.TabCoordinator
import org.koin.compose.koinInject

/**
 * Root navigation shell with auth gate and bottom bar navigation.
 *
 * - If not authenticated: shows login screen from IdentityDestination
 * - If authenticated: shows tabbed navigation with 5 tabs (Home, Search, Create, Activity, Profile)
 * - Simple state-based tab switching with bottom bar; any Screen can ask to
 *   switch via the shared [TabCoordinator].
 */
@Composable
internal fun MatchScaffold() {
    val sessionHolder = koinInject<SessionHolder>()
    val isAuthenticated by sessionHolder.isAuthenticated.collectAsState(false)

    if (isAuthenticated) {
        AuthenticatedShell()
    } else {
        LoginShell()
    }
}

@Composable
private fun LoginShell() {
    val identityDestination = koinInject<IdentityDestination>()
    Navigator(screen = identityDestination.login())
}

/**
 * The bottom bar speaks [MatchBottomBarTab]; routing speaks [MainTab]. The two
 * enums are parallel — same order, same count — so the fourth slot the design
 * renames to "Atividade" is the [MainTab.MyMatches] screen underneath.
 */
private fun MatchBottomBarTab.toMainTab(): MainTab = MainTab.entries[ordinal]

private fun AppShellStrings.labelFor(tab: MatchBottomBarTab): String = when (tab) {
    MatchBottomBarTab.Home -> homeTab
    MatchBottomBarTab.Search -> searchTab
    MatchBottomBarTab.Create -> createTab
    MatchBottomBarTab.Activity -> activityTab
    MatchBottomBarTab.Profile -> profileTab
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedShell() {
    val gamesDestination = koinInject<GamesDestination>()
    val tabCoordinator = koinInject<TabCoordinator>()
    val deepLinkCoordinator = koinInject<DeepLinkCoordinator>()
    val strings = rememberAppShellStrings()
    val (selectedTab, setSelectedTab) = remember { mutableStateOf(MainTab.Home) }
    val (showNotificationHistory, setShowNotificationHistory) = remember { mutableStateOf(false) }
    val (detailScreen, setDetailScreen) = remember { mutableStateOf<Screen?>(null) }
    val (showMapView, setShowMapView) = remember { mutableStateOf(false) }

    // Listen for cross-screen tab requests (e.g. "after creating a match, switch
    // to My Matches"). The shell owns the selected tab so this is the one place
    // that needs to react.
    LaunchedEffect(tabCoordinator) {
        tabCoordinator.tabs.collect { tab -> setSelectedTab(tab) }
    }

    // Listen for deep link navigation events (e.g. "open match detail")
    LaunchedEffect(deepLinkCoordinator) {
        deepLinkCoordinator.links.collect { link ->
            when (link) {
                is DeepLink.OpenMatch -> {
                    setDetailScreen(gamesDestination.matchDetail(link.matchId))
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top app bar with notification bell and map toggle (on Home tab only)
        TopAppBar(
            title = { Text(strings.appTitle) },
            actions = {
                if (selectedTab == MainTab.Home) {
                    IconButton(onClick = { setShowMapView(!showMapView) }) {
                        Icon(
                            imageVector = if (showMapView) {
                                Icons.AutoMirrored.Filled.List
                            } else {
                                Icons.Filled.Map
                            },
                            contentDescription = if (showMapView) {
                                strings.showListAction
                            } else {
                                strings.showMapAction
                            },
                        )
                    }
                }
                IconButton(onClick = { setShowNotificationHistory(true) }) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = strings.notificationsAction,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )

        // Main content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Tab navigation
            if (detailScreen == null) {
                when (selectedTab) {
                    MainTab.Home -> Navigator(
                        screen = if (showMapView) gamesDestination.map() else gamesDestination.gameList()
                    )
                    MainTab.Search -> Navigator(screen = gamesDestination.search())
                    MainTab.Create -> Navigator(screen = gamesDestination.create())
                    MainTab.MyMatches -> Navigator(screen = gamesDestination.myMatches())
                    MainTab.PlayerProfile -> Navigator(screen = gamesDestination.playerProfile())
                }
            } else {
                // Show detail screen overlay
                Navigator(
                    screen = detailScreen!!,
                    onBackPressed = { setDetailScreen(null); true },
                )
            }
        }

        MatchBottomBar(
            selectedTab = MatchBottomBarTab.entries[selectedTab.index],
            onTabSelected = { tab -> setSelectedTab(tab.toMainTab()) },
            label = { tab -> strings.labelFor(tab) },
        )
    }

    // Notification history modal
    NotificationHistoryStep(
        isVisible = showNotificationHistory,
        onDismiss = { setShowNotificationHistory(false) },
    )
}
