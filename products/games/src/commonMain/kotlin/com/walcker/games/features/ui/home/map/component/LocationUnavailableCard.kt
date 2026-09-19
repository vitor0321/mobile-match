package com.walcker.games.features.ui.home.map.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.strings.MapStrings
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun LocationUnavailableCard(
    strings: MapStrings,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = CedarTokens.radius.lgShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.overlay),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(CedarTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
        ) {
            Text(
                text = strings.locationUnavailableTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = strings.locationUnavailableBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CedarTextButton(text = strings.retry, onClick = onRetry)
        }
    }
}
