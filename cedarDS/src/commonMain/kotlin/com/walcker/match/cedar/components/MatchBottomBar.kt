package com.walcker.match.cedar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.walcker.match.cedar.tokens.CedarTokens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

private const val BottomBarContainerAlpha = 0.96f
private const val SelectedPillAlpha = 0.16f
private const val HazeTintAlpha = 0.35f
private val MinTouchTarget = 48.dp
private val HazeBlurRadius = 20.dp

public val LocalBottomBarInset: ProvidableCompositionLocal<Dp> = staticCompositionLocalOf { 0.dp }

public enum class MatchBottomBarTab(
    public val icon: ImageVector,
) {
    Home(Icons.Outlined.Home),
    Search(Icons.Outlined.Search),
    Create(Icons.Outlined.AddCircle),
    Activity(Icons.Outlined.Notifications),
    Profile(Icons.Outlined.Person),
}

@Composable
public fun MatchBottomBar(
    selectedTab: MatchBottomBarTab,
    onTabSelected: (MatchBottomBarTab) -> Unit,
    label: (MatchBottomBarTab) -> String,
    modifier: Modifier = Modifier,
    showDot: (MatchBottomBarTab) -> Boolean = { false },
    hazeState: HazeState? = null,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    Surface(
        modifier =
            modifier
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
                .fillMaxWidth()
                .padding(horizontal = CedarTokens.spacing.md, vertical = CedarTokens.spacing.xs)
                .let { pillModifier ->
                    if (hazeState != null) {
                        pillModifier
                            .clip(CedarTokens.radius.pill)
                            .hazeEffect(
                                state = hazeState,
                                style =
                                    HazeStyle(
                                        backgroundColor = surfaceColor,
                                        blurRadius = HazeBlurRadius,
                                        tints = listOf(HazeTint(color = surfaceColor.copy(alpha = HazeTintAlpha))),
                                    ),
                            )
                    } else {
                        pillModifier
                    }
                },
        shape = CedarTokens.radius.pill,
        color = if (hazeState != null) Color.Transparent else surfaceColor.copy(alpha = BottomBarContainerAlpha),
        shadowElevation = CedarTokens.elevation.overlay,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CedarTokens.spacing.xs, vertical = CedarTokens.spacing.xxs),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MatchBottomBarTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                MatchBottomBarItem(
                    icon = tab.icon,
                    caption = label(tab),
                    selected = selected,
                    showDot = showDot(tab),
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MatchBottomBarItem(
    icon: ImageVector,
    caption: String,
    selected: Boolean,
    showDot: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Column(
        modifier =
            modifier
                .defaultMinSize(minHeight = MinTouchTarget)
                .clip(CedarTokens.radius.pill)
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = SelectedPillAlpha)
                    } else {
                        Color.Transparent
                    },
                ).selectable(selected = selected, onClick = onClick, role = Role.Tab)
                .semantics(mergeDescendants = true) { }
                .padding(vertical = CedarTokens.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
    ) {
        if (showDot) {
            BadgedBox(badge = { Badge() }) {
                Icon(imageVector = icon, contentDescription = null, tint = contentColor)
            }
        } else {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}
