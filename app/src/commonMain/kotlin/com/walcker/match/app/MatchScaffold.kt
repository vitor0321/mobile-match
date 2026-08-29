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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.walcker.games.features.ui.notifications.NotificationHistoryStep
import com.walcker.identity.api.SessionHolder
import com.walcker.match.app.strings.AppShellStrings
import com.walcker.match.app.strings.rememberAppShellStrings
import com.walcker.match.cedar.components.MatchBottomBar
import com.walcker.match.cedar.components.MatchBottomBarTab
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.navigation.NavigatorHolder
import com.walcker.match.navigator.DeepLink
import com.walcker.match.navigator.DeepLinkCoordinator
import com.walcker.match.navigator.GamesDestination
import com.walcker.match.navigator.IdentityDestination
import com.walcker.match.navigator.LoginCoordinator
import com.walcker.match.navigator.MainTab
import com.walcker.match.navigator.TabCoordinator
import org.koin.compose.koinInject

@Composable
internal fun MatchScaffold() {
    AuthenticatedShell()
}

private fun MatchBottomBarTab.toMainTab(): MainTab = MainTab.entries[ordinal]

private fun AppShellStrings.labelFor(tab: MatchBottomBarTab): String = when (tab) {
    MatchBottomBarTab.Home -> homeTab
    MatchBottomBarTab.Search -> searchTab
    MatchBottomBarTab.Create -> createTab
    MatchBottomBarTab.Activity -> activityTab
    MatchBottomBarTab.Profile -> profileTab
}

@Composable
private fun AttachedNavigator(
    screen: Screen,
    navigatorHolder: NavigatorHolder,
    attachEnabled: Boolean,
    onBackPressed: (Screen) -> Boolean = { true },
) {
    Navigator(screen = screen, onBackPressed = onBackPressed) {
        DisposableEffect(it, attachEnabled) {
            if (attachEnabled) navigatorHolder.attach(it)
            onDispose { navigatorHolder.detach(it) }
        }
        CurrentScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedShell() {
    val gamesDestination = koinInject<GamesDestination>()
    val identityDestination = koinInject<IdentityDestination>()
    val tabCoordinator = koinInject<TabCoordinator>()
    val deepLinkCoordinator = koinInject<DeepLinkCoordinator>()
    val loginCoordinator = koinInject<LoginCoordinator>()
    val navigatorHolder = koinInject<NavigatorHolder>()
    val sessionHolder = koinInject<SessionHolder>()
    val strings = rememberAppShellStrings()
    val (selectedTab, setSelectedTab) = remember { mutableStateOf(MainTab.Home) }
    val (showNotificationHistory, setShowNotificationHistory) = remember { mutableStateOf(false) }
    val (detailScreen, setDetailScreen) = remember { mutableStateOf<Screen?>(null) }
    val (showMapView, setShowMapView) = remember { mutableStateOf(false) }
    val (showLogin, setShowLogin) = remember { mutableStateOf(false) }

    val isAuthenticated: Boolean? by remember(sessionHolder) {
        sessionHolder.isAuthenticated
    }.collectAsState(initial = null)

    LaunchedEffect(tabCoordinator) {
        tabCoordinator.tabs.collect { tab -> setSelectedTab(tab) }
    }

    LaunchedEffect(deepLinkCoordinator) {
        deepLinkCoordinator.links.collect { link ->
            when (link) {
                is DeepLink.OpenMatch -> {
                    setDetailScreen(gamesDestination.matchDetail(link.matchId))
                }
            }
        }
    }

    LaunchedEffect(loginCoordinator) {
        loginCoordinator.requests.collect { setShowLogin(true) }
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated == true) setShowLogin(false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    IconButton(
                        onClick = {
                            if (isAuthenticated == true) {
                                setShowNotificationHistory(true)
                            } else {
                                loginCoordinator.requestLogin()
                            }
                        },
                    ) {
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

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                if (detailScreen == null) {
                    val tabScreen = when (selectedTab) {
                        MainTab.Home ->
                            if (showMapView) gamesDestination.map() else gamesDestination.gameList()
                        MainTab.Search -> gamesDestination.search()
                        MainTab.Create -> gamesDestination.create()
                        MainTab.MyMatches -> gamesDestination.myMatches()
                        MainTab.PlayerProfile -> gamesDestination.playerProfile()
                    }
                    key(selectedTab, showMapView) {
                        AttachedNavigator(
                            screen = tabScreen,
                            navigatorHolder = navigatorHolder,
                            attachEnabled = !showLogin,
                        )
                    }
                } else {
                    key(detailScreen) {
                        AttachedNavigator(
                            screen = detailScreen!!,
                            navigatorHolder = navigatorHolder,
                            attachEnabled = !showLogin,
                            onBackPressed = { setDetailScreen(null); true },
                        )
                    }
                }
            }

            MatchBottomBar(
                selectedTab = MatchBottomBarTab.entries[selectedTab.index],
                onTabSelected = { tab -> setSelectedTab(tab.toMainTab()) },
                label = { tab -> strings.labelFor(tab) },
            )
        }

        if (showLogin) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CedarTokens.colors.canvas),
            ) {
                AttachedNavigator(
                    screen = identityDestination.login(),
                    navigatorHolder = navigatorHolder,
                    attachEnabled = true,
                    onBackPressed = { setShowLogin(false); true },
                )
            }
        }
    }

    NotificationHistoryStep(
        isVisible = showNotificationHistory,
        onDismiss = { setShowNotificationHistory(false) },
    )
}
