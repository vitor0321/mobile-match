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

/**
 * The five destinations of the app.
 *
 * `Chats` is gone. It pointed at a feature that does not exist — there is no chat
 * anywhere in the codebase — so a fifth of the navigation was a dead end. The
 * redesign calls this slot "Atividade", and the app already has the two screens
 * that belong there: notification history and my matches.
 *
 * The enum carries no label any more. It used to hold hardcoded pt-BR strings,
 * which put user-facing copy inside a module with no strings layer and made the
 * design system untranslatable. Labels now come from the caller, same rule the
 * rest of the components follow.
 */
public enum class MatchBottomBarTab(public val icon: ImageVector) {
    Home(Icons.Filled.Home),
    Search(Icons.Filled.Search),
    Create(Icons.Filled.AddCircle),
    Activity(Icons.Filled.Notifications),
    Profile(Icons.Filled.Person),
}

/**
 * Bottom navigation.
 *
 * @param label resolves each tab's caption. Also used as the icon's content
 *   description, so the bar is usable with a screen reader.
 * @param badgeCount unread items per tab — the reason "Atividade" is worth a slot
 *   at all. Return 0 for no badge.
 */
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

/** Caps the badge so a long-absent user does not get a three-digit pill. */
private fun Int.badgeLabel(): String = if (this > MAX_BADGE_COUNT) "$MAX_BADGE_COUNT+" else toString()

private const val MAX_BADGE_COUNT = 99
