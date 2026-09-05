package com.walcker.games.features.ui.shared.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.match.cedar.tokens.CedarTokens

private val BadgeSize = 48.dp
private val BadgeIconSize = 22.dp
private const val DisabledAlpha = 0.38f

internal fun Sport.icon(): ImageVector =
    when (this) {
        Sport.FUTEBOL -> Icons.Filled.SportsSoccer
        Sport.FUTSAL -> Icons.Filled.SportsHandball
        Sport.SOCIETY -> Icons.Filled.Grass
        Sport.VOLEI -> Icons.Filled.SportsVolleyball
        Sport.BASQUETE -> Icons.Filled.SportsBasketball
        Sport.BEACH_TENNIS -> Icons.Filled.BeachAccess
        Sport.TENIS -> Icons.Filled.SportsTennis
        Sport.PADEL -> Icons.Filled.SportsCricket
        Sport.FUTEVOLEI -> Icons.Filled.Waves
        Sport.PICKLEBALL -> Icons.Filled.SportsBaseball
        Sport.NATACAO -> Icons.Filled.Pool
    }

@Composable
internal fun SportBadge(
    sport: Sport,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SportBadgeBase(
        icon = sport.icon(),
        label = sport.label,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
internal fun AllSportsBadge(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SportBadgeBase(
        icon = Icons.Filled.Apps,
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
private fun SportBadgeBase(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val containerColor =
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Column(
        modifier =
            modifier
                .alpha(if (enabled) 1f else DisabledAlpha)
                .toggleable(
                    value = selected,
                    onValueChange = { onClick() },
                    enabled = enabled,
                    role = Role.Checkbox,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
    ) {
        Box(
            modifier =
                Modifier
                    .size(BadgeSize)
                    .background(color = containerColor, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(BadgeIconSize),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
