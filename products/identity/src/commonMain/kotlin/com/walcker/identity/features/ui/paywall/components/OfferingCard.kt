package com.walcker.identity.features.ui.paywall.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.walcker.identity.features.domain.billing.ProductOffering

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
    val border = if (isSelected) colors.primary else colors.outline
    val titleColor = if (isSelected) colors.onPrimaryContainer else colors.onSurface
    val bodyColor = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = background,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(background, RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = offering.title,
                    color = titleColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                SelectionIndicator(
                    isSelected = isSelected,
                    borderColor = border,
                    fillColor = colors.surface,
                )
            }
            Text(
                text = offering.description,
                color = bodyColor,
            )
            Text(
                text = offering.priceLabel,
                color = titleColor,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.fillMaxWidth())
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(border.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = if (isSelected) selectedLabel else selectHint,
                    color = border,
                    fontWeight = FontWeight.Medium,
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
            .size(20.dp)
            .border(width = 2.dp, color = borderColor, shape = CircleShape)
            .background(color = fillColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = borderColor, shape = CircleShape),
            )
        }
    }
}
