package com.walcker.match.cedar.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom navigation bar with the 5 main destinations of the app:
 * Home, Search, Create, Chats, Profile.
 *
 * Stateful (owns the selected tab) — callers wrap it in a Scaffold and
 * react to [onTabSelected] to swap screens.
 */
@Composable
public fun MatchBottomBar(
    selectedTab: MatchBottomBarTab = MatchBottomBarTab.Home,
    onTabSelected: (MatchBottomBarTab) -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    NavigationBar(modifier = modifier) {
        MatchBottomBarTab.values().forEach { tab ->
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}

/** Stable identifier for each bottom bar destination. */
public enum class MatchBottomBarTab(
    val label: String,
    val icon: ImageVector,
) {
    Home("Início", Icons.Filled.Home),
    Search("Buscar", Icons.Filled.Search),
    Create("Criar", Icons.Filled.AddCircle),
    Chats("Chats", Icons.Filled.Chat),
    Profile("Perfil", Icons.Filled.Person),
}
