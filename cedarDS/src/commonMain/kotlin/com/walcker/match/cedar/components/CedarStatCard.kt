package com.walcker.match.cedar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import com.walcker.match.cedar.tokens.CedarTokens
import kotlinx.collections.immutable.ImmutableList

@Immutable
public data class CedarStat(
    val value: String,
    val label: String,
    val accessibilityLabel: String? = null,
    val highlighted: Boolean = false,
)

@Composable
public fun CedarStatCard(
    stat: CedarStat,
    modifier: Modifier = Modifier,
) {
    val description = stat.accessibilityLabel
    Card(
        shape = CedarTokens.radius.mdShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.flat),
        modifier =
            if (description != null) {
                modifier.clearAndSetSemantics { contentDescription = description }
            } else {
                modifier
            },
    ) {
        Column(
            modifier = Modifier.padding(CedarTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
        ) {
            Text(
                text = stat.value,
                style = MaterialTheme.typography.headlineSmall,
                color =
                    if (stat.highlighted) {
                        CedarTokens.colors.availableText
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stat.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
public fun CedarStatRow(
    stats: ImmutableList<CedarStat>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
    ) {
        stats.forEach { stat ->
            CedarStatCard(stat = stat, modifier = Modifier.weight(1f))
        }
    }
}
