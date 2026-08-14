package com.walcker.match.cedar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.walcker.match.core.datetime.formatWhen

/**
 * Card displaying a single open match.
 *
 * Centralised here so every screen (home, search, future "your matches")
 * renders a match identically. i18n labels are provided by callers so
 * the design system stays free of product-specific strings.
 */
@Composable
public fun MatchCard(
    sportLabel: String,
    venueName: String,
    neighborhood: String,
    startsAtSeconds: Long,
    confirmedPlayers: Int,
    totalPlayers: Int,
    openSlots: Int,
    pricePerPlayer: String?,
    joinButtonLabel: String,
    playersAndSlotsLabel: String,
    perPlayerLabel: String?,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = sportLabel.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$venueName · $neighborhood",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = formatWhen(startsAtSeconds = startsAtSeconds),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = playersAndSlotsLabel,
                style = MaterialTheme.typography.bodyMedium,
            )
            pricePerPlayer?.let { price ->
                Text(
                    text = perPlayerLabel?.let { "$price $it" } ?: price,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(onClick = onJoinClick) { Text(joinButtonLabel) }
            }
        }
    }
}
