package com.walcker.games.features.ui.shared.manageMatch.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.strings.ManageMatchStrings
import com.walcker.games.strings.sportName
import com.walcker.match.cedar.components.CedarTag
import com.walcker.match.cedar.components.CedarTagTone
import com.walcker.match.cedar.tokens.CedarTokens
import com.walcker.match.core.datetime.formatDayLabel
import com.walcker.match.core.datetime.formatTimeRange

private val ChipIconSize = 16.dp
private val EditButtonSize = 48.dp

@Composable
internal fun MatchHeaderCard(
    match: Game,
    vipCount: Int,
    manage: ManageMatchStrings,
    onEditMatch: () -> Unit,
    modifier: Modifier = Modifier,
    canEdit: Boolean = true,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CedarTokens.radius.lgShape,
                ).padding(CedarTokens.spacing.md),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
            ) {
                Text(
                    text = manage.nextMatchLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = match.venueName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            CedarTag(label = sportName(match.sport).uppercase(), tone = CedarTagTone.Info)

            IconButton(
                onClick = onEditMatch,
                enabled = canEdit,
                modifier = Modifier.size(EditButtonSize),
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = manage.editMatchContentDescription,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderChip(icon = Icons.Filled.CalendarMonth, text = formatDayLabel(match.startsAtSeconds))
            HeaderChip(
                icon = Icons.Filled.Schedule,
                text = formatTimeRange(startsAtSeconds = match.startsAtSeconds, durationMin = match.durationMin),
            )
            HeaderChip(icon = Icons.Filled.LocationOn, text = match.neighborhood)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = CedarTokens.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        ) {
            HeaderStat(
                value = match.confirmedPlayers.toString(),
                label = manage.statConfirmedLabel,
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            HeaderStat(
                value = vipCount.toString(),
                label = manage.statVipsLabel,
                valueColor = CedarTokens.colors.vip,
                modifier = Modifier.weight(1f),
            )
            HeaderStat(
                value = match.teamCount.toString(),
                label = manage.statTeamsLabel,
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeaderChip(
    icon: ImageVector,
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(ChipIconSize),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HeaderStat(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
