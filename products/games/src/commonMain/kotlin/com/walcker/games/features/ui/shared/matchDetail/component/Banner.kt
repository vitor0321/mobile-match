package com.walcker.games.features.ui.shared.matchDetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.walcker.match.cedar.tokens.CedarTokens
import kotlinx.coroutines.delay

private const val AUTO_DISMISS_MS = 5_000L

@Composable
internal fun Banner(
    message: String,
    container: Color,
    onContainer: Color,
    dismissContentDescription: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnDismiss = rememberUpdatedState(onDismiss)
    LaunchedEffect(message) {
        delay(AUTO_DISMISS_MS)
        currentOnDismiss.value()
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CedarTokens.spacing.lg,
                    vertical = CedarTokens.spacing.xs,
                ).background(color = container, shape = CedarTokens.radius.smShape)
                .padding(
                    start = CedarTokens.spacing.md,
                    top = CedarTokens.spacing.xs,
                    bottom = CedarTokens.spacing.xs,
                ),
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = onContainer,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = dismissContentDescription,
                tint = onContainer,
            )
        }
    }
}
