package com.walcker.match.cedar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.walcker.match.cedar.tokens.CedarTokens

private val CheckCircleSize = 88.dp
private val CheckIconSize = 44.dp

@Composable
public fun CedarSuccessScreen(
    title: String,
    subtitle: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    summary: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = CedarTokens.spacing.lg,
                    vertical = CedarTokens.spacing.xxl,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
    ) {
        Surface(
            shape = CircleShape,
            color = CedarTokens.colors.availableContainer,
            contentColor = CedarTokens.colors.availableText,
            modifier = Modifier.size(CheckCircleSize),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(CheckIconSize),
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Assertive
                },
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (summary != null) {
            Card(
                shape = CedarTokens.radius.lgShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = CedarTokens.elevation.flat),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = CedarTokens.spacing.md),
            ) {
                Column(
                    modifier = Modifier.padding(CedarTokens.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
                ) {
                    summary()
                }
            }
        }

        CedarPrimaryButton(
            text = primaryActionLabel,
            onClick = onPrimaryAction,
            modifier = Modifier.padding(top = CedarTokens.spacing.lg),
        )
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            CedarSecondaryButton(
                text = secondaryActionLabel,
                onClick = onSecondaryAction,
            )
        }
    }
}

@Composable
public fun CedarCodeBlock(
    label: String,
    code: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = code,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
