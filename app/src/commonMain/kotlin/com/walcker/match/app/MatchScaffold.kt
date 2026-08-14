package com.walcker.match.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import com.walcker.identity.api.SessionHolder
import com.walcker.match.cedar.CedarLoadingIndicator
import com.walcker.match.navigator.GamesDestination
import com.walcker.match.navigator.IdentityDestination
import org.koin.compose.koinInject

/**
 * Root navigation shell with auth gate and bottom bar navigation.
 *
 * - If not authenticated: shows login screen from IdentityDestination
 * - If authenticated: shows tabbed navigation with 5 tabs (Home, Search, Create, MyMatches, Profile)
 * - Simple state-based tab switching with bottom bar
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

@Composable
private fun AuthenticatedShell() {
    val gamesDestination = koinInject<GamesDestination>()
    val (selectedTab, setSelectedTab) = remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Main content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            when (selectedTab) {
                0 -> Navigator(screen = gamesDestination.gameList())
                1 -> Navigator(screen = gamesDestination.search())
                2 -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { Text("Create — Coming in Phase 1") }

                3 -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { Text("MyMatches — Coming in Phase 1") }

                4 -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { Text("Profile — Coming in Phase 1") }
            }
        }

        // Bottom navigation bar
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            val tabs = listOf("🏠 Home", "🔍 Search", "➕ Create", "⚽ Matches", "👤 Profile")

            tabs.forEachIndexed { tabIndex, label ->
                NavigationBarItem(
                    selected = selectedTab == tabIndex,
                    onClick = { setSelectedTab(tabIndex) },
                    icon = { },
                    label = { Text(label) },
                    alwaysShowLabel = true,
                )
            }
        }
    }
}
