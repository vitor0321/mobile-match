package com.walcker.games.features.ui.shared.manageMatch.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.tokens.CedarTokens

private val ActionIconSize = 28.dp
private val CompactIconSize = 20.dp
private val CompactHeight = 72.dp
private const val DisabledAlpha = 0.4f

@Composable
internal fun MatchActionCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isWorking: Boolean = false,
    isDanger: Boolean = false,
    isCompact: Boolean = false,
) {
    val contentColor =
        if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val effectiveColor = if (enabled) contentColor else contentColor.copy(alpha = DisabledAlpha)
    val iconSize = if (isCompact) CompactIconSize else ActionIconSize

    Surface(
        modifier =
            modifier
                .then(if (isCompact) Modifier.height(CompactHeight) else Modifier.aspectRatio(1f))
                .clickable(enabled = enabled && !isWorking, onClick = onClick),
        shape = CedarTokens.radius.lgShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, effectiveColor.copy(alpha = DisabledAlpha)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(CedarTokens.spacing.md),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(iconSize),
                contentAlignment = Alignment.Center,
            ) {
                if (isWorking) {
                    CedarLoading(contentDescription = label, size = iconSize)
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = effectiveColor,
                        modifier = Modifier.size(iconSize),
                    )
                }
            }

            Text(
                text = label,
                style = if (isCompact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                color = effectiveColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = CedarTokens.spacing.xxs),
            )
        }
    }
}
