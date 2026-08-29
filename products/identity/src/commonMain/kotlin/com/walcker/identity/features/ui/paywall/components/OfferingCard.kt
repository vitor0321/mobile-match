package com.walcker.identity.features.ui.paywall.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.walcker.identity.features.domain.billing.ProductOffering
import com.walcker.match.cedar.tokens.CedarTokens

private val IndicatorSize = 20.dp
private val IndicatorDotSize = 8.dp
private val IndicatorBorderWidth = 2.dp

private const val HINT_BACKGROUND_ALPHA = 0.12f

@Composable
internal fun OfferingCard(
    offering: ProductOffering,
    isSelected: Boolean,
    selectedLabel: String,
    selectHint: String,
    colors: ColorScheme,
    onClick: () -> Unit,
) {
    val background = if (isSelected) colors.primaryContainer else colors.surface
    val accent = if (isSelected) colors.primary else colors.outline
    val titleColor = if (isSelected) colors.onPrimaryContainer else colors.onSurface
    val bodyColor = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CedarTokens.radius.mdShape,
        color = background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = isSelected,
                    role = Role.RadioButton,
                    onClick = onClick,
                )
                .padding(CedarTokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = offering.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    modifier = Modifier.weight(1f),
                )
                SelectionIndicator(
                    isSelected = isSelected,
                    borderColor = accent,
                    fillColor = colors.surface,
                )
            }
            Text(
                text = offering.description,
                style = MaterialTheme.typography.bodyMedium,
                color = bodyColor,
            )
            Text(
                text = offering.priceLabel,
                style = MaterialTheme.typography.titleLarge,
                color = titleColor,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = CedarTokens.spacing.xxs)
                    .background(
                        color = accent.copy(alpha = HINT_BACKGROUND_ALPHA),
                        shape = CedarTokens.radius.smShape,
                    )
                    .padding(
                        horizontal = CedarTokens.spacing.sm,
                        vertical = CedarTokens.spacing.xs,
                    ),
            ) {
                Text(
                    text = if (isSelected) selectedLabel else selectHint,
                    style = MaterialTheme.typography.labelLarge,
                    color = titleColor,
                )
            }
        }
    }
}

@Composable
private fun SelectionIndicator(
    isSelected: Boolean,
    borderColor: Color,
    fillColor: Color,
) {
    Box(
        modifier = Modifier
            .size(IndicatorSize)
            .border(width = IndicatorBorderWidth, color = borderColor, shape = CircleShape)
            .background(color = fillColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(IndicatorDotSize)
                    .background(color = borderColor, shape = CircleShape),
            )
        }
    }
}
