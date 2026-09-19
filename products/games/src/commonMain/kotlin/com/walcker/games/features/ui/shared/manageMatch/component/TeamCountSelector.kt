package com.walcker.games.features.ui.shared.manageMatch.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.walcker.match.cedar.tokens.CedarTokens

internal const val MIN_TEAM_COUNT = 2
internal const val MAX_TEAM_COUNT = 6
internal const val MIN_PLAYERS_PER_TEAM = 2
internal const val MAX_PLAYERS_PER_TEAM = 15
private val MinTouchTarget = 48.dp

@Composable
internal fun TeamCountSelector(
    selectedCount: Int,
    onCountSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChipRangeSelector(
        selected = selectedCount,
        range = MIN_TEAM_COUNT..MAX_TEAM_COUNT,
        onSelected = onCountSelected,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChipRangeSelector(
    selected: Int,
    range: IntRange,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        for (count in range) {
            val isSelected = count == selected
            val background = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val foreground = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier =
                    Modifier
                        .defaultMinSize(minWidth = MinTouchTarget, minHeight = MinTouchTarget)
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelected(count) })
                        .background(color = background, shape = CedarTokens.radius.pill)
                        .padding(horizontal = CedarTokens.spacing.md, vertical = CedarTokens.spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = foreground,
                )
            }
        }
    }
}
