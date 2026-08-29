package com.walcker.match.cedar.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

public enum class MatchBottomBarTab(public val icon: ImageVector) {
    Home(Icons.Filled.Home),
    Search(Icons.Filled.Search),
    Create(Icons.Filled.AddCircle),
    Activity(Icons.Filled.Notifications),
    Profile(Icons.Filled.Person),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MatchBottomBar(
    selectedTab: MatchBottomBarTab,
    onTabSelected: (MatchBottomBarTab) -> Unit,
    label: (MatchBottomBarTab) -> String,
    modifier: Modifier = Modifier,
    badgeCount: (MatchBottomBarTab) -> Int = { 0 },
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = NavigationBarDefaults.Elevation,
    ) {
        MatchBottomBarTab.entries.forEach { tab ->
            val caption = label(tab)
            val count = badgeCount(tab)
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = {
                    if (count > 0) {
                        BadgedBox(badge = { Badge { Text(count.badgeLabel()) } }) {
                            Icon(tab.icon, contentDescription = caption)
                        }
                    } else {
                        Icon(tab.icon, contentDescription = caption)
                    }
                },
                label = {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

private fun Int.badgeLabel(): String = if (this > MAX_BADGE_COUNT) "$MAX_BADGE_COUNT+" else toString()

private const val MAX_BADGE_COUNT = 99
