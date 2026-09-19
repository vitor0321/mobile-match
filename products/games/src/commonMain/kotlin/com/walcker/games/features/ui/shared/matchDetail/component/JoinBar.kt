package com.walcker.games.features.ui.shared.matchDetail.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.match.cedar.components.CedarAvailabilityButton
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun JoinBar(
    label: String,
    priceLabel: String?,
    enabled: Boolean,
    isLoading: Boolean,
    useAvailabilityTone: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = CedarTokens.elevation.overlay,
    ) {
        Box(
            modifier =
                Modifier.padding(
                    horizontal = CedarTokens.spacing.lg,
                    vertical = CedarTokens.spacing.sm,
                ),
        ) {
            val text = if (priceLabel != null && enabled) "$label · $priceLabel" else label
            if (useAvailabilityTone) {
                CedarAvailabilityButton(
                    text = text,
                    onClick = onClick,
                    enabled = enabled,
                    loading = isLoading,
                )
            } else {
                CedarPrimaryButton(
                    text = text,
                    onClick = onClick,
                    enabled = enabled,
                    loading = isLoading,
                )
            }
        }
    }
}
