package com.walcker.games.features.ui.shared.matchDetail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun LabelledValue(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
