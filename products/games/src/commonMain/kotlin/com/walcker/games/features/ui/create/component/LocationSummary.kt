package com.walcker.games.features.ui.create.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.strings.CreateMatchStrings
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun LocationSummary(
    strings: CreateMatchStrings,
    address: String,
    neighborhood: String,
    city: String,
    isResolvingLocation: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
    ) {
        when {
            isResolvingLocation -> {
                Text(
                    text = strings.resolvingLocation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            address.isBlank() && neighborhood.isBlank() && city.isBlank() -> {
                Text(
                    text = strings.locationNotResolved,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                if (address.isNotBlank()) {
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                val neighborhoodCity =
                    listOf(neighborhood, city)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                if (neighborhoodCity.isNotBlank()) {
                    Text(
                        text = neighborhoodCity,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
