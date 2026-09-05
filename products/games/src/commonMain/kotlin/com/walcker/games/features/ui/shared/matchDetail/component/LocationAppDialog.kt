package com.walcker.games.features.ui.shared.matchDetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.walcker.match.cedar.components.CedarIcons
import com.walcker.match.cedar.tokens.CedarTokens

private val MinTouchTarget = 48.dp
private val LogoBadgeSize = 40.dp
private val LogoSize = 24.dp

@Composable
internal fun LocationAppDialog(
    title: String,
    googleMapsLabel: String,
    wazeLabel: String,
    cancelLabel: String,
    onGoogleMaps: () -> Unit,
    onWaze: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = CedarTokens.radius.lgShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = CedarTokens.elevation.overlay,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(CedarTokens.spacing.lg),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Column(
                    modifier = Modifier.padding(top = CedarTokens.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
                ) {
                    LocationAppOption(icon = CedarIcons.GoogleMaps, label = googleMapsLabel, onClick = onGoogleMaps)
                    LocationAppOption(icon = CedarIcons.Waze, label = wazeLabel, onClick = onWaze)
                }
                TextButton(
                    onClick = onDismiss,
                    modifier =
                        Modifier
                            .align(Alignment.End)
                            .padding(top = CedarTokens.spacing.sm),
                ) {
                    Text(cancelLabel)
                }
            }
        }
    }
}

@Composable
private fun LocationAppOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MinTouchTarget)
                .clip(CedarTokens.radius.mdShape)
                .clickable(onClick = onClick)
                .padding(horizontal = CedarTokens.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
    ) {
        Box(
            modifier =
                Modifier
                    .size(LogoBadgeSize)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(LogoSize),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
