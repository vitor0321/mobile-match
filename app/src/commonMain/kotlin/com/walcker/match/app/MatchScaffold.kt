package com.walcker.match.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
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
import com.walcker.games.features.ui.shared.matchDetail.MatchDetailBottomSheet
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
import com.walcker.match.navigator.MatchDetailCoordinator
import com.walcker.match.navigator.TabCoordinator
import org.koin.compose.koinInject

@Composable
internal fun MatchScaffold() {
    AuthenticatedShell()
}

internal fun MatchBottomBarTab.toMainTab(): MainTab = MainTab.entries[ordinal]

internal fun AppShellStrings.labelFor(tab: MatchBottomBarTab): String =
    when (tab) {
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

@Composable
private fun AuthenticatedShell() {
    val gamesDestination = koinInject<GamesDestination>()
    val identityDestination = koinInject<IdentityDestination>()
    val tabCoordinator = koinInject<TabCoordinator>()
    val deepLinkCoordinator = koinInject<DeepLinkCoordinator>()
    val loginCoordinator = koinInject<LoginCoordinator>()
    val matchDetailCoordinator = koinInject<MatchDetailCoordinator>()
    val navigatorHolder = koinInject<NavigatorHolder>()
    val sessionHolder = koinInject<SessionHolder>()
    val strings = rememberAppShellStrings()
    val (selectedTab, setSelectedTab) = remember { mutableStateOf(MainTab.Home) }
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
                    matchDetailCoordinator.open(link.matchId)
                }
            }
        }
    }

    LaunchedEffect(loginCoordinator) {
        loginCoordinator.requests.collect { setShowLogin(true) }
    }

    LaunchedEffect(loginCoordinator) {
        loginCoordinator.dismissals.collect { setShowLogin(false) }
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated == true) setShowLogin(false)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .consumeWindowInsets(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)),
            ) {
                val tabScreen =
                    when (selectedTab) {
                        MainTab.Home -> gamesDestination.gameList()
                        MainTab.Search -> gamesDestination.search()
                        MainTab.Create -> gamesDestination.create()
                        MainTab.MyMatches -> gamesDestination.myMatches()
                        MainTab.PlayerProfile -> gamesDestination.playerProfile()
                    }
                key(selectedTab) {
                    AttachedNavigator(
                        screen = tabScreen,
                        navigatorHolder = navigatorHolder,
                        attachEnabled = !showLogin,
                    )
                }
            }

            MatchBottomBar(
                selectedTab = MatchBottomBarTab.entries[selectedTab.index],
                onTabSelected = { tab -> setSelectedTab(tab.toMainTab()) },
                label = { tab -> strings.labelFor(tab) },
                showDot = { tab -> tab == MatchBottomBarTab.Activity },
            )
        }

        if (showLogin) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(CedarTokens.colors.canvas),
            ) {
                AttachedNavigator(
                    screen = identityDestination.login(),
                    navigatorHolder = navigatorHolder,
                    attachEnabled = true,
                    onBackPressed = {
                        setShowLogin(false)
                        true
                    },
                )
            }
        }

        MatchDetailBottomSheet()
    }
}
