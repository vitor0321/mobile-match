package com.walcker.games.features.ui.playerProfile.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.walcker.games.strings.PlayerProfileStrings
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun AvailabilityCard(
    isAvailable: Boolean,
    isUpdating: Boolean,
    strings: PlayerProfileStrings,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    availableUntilTonight: Boolean = false,
    onUntilTonightChange: (Boolean) -> Unit = {},
) {
    Card(
        shape = CedarTokens.radius.lgShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.flat),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(CedarTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.availabilityTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text =
                            if (isAvailable) {
                                strings.availabilityOnDescription
                            } else {
                                strings.availabilityOffDescription
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = isAvailable,
                    onCheckedChange = onCheckedChange,
                    enabled = !isUpdating,
                )
            }

            if (isAvailable) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = strings.availableUntilTonightLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = availableUntilTonight,
                        onCheckedChange = onUntilTonightChange,
                        enabled = !isUpdating,
                    )
                }
            }
        }
    }
}
