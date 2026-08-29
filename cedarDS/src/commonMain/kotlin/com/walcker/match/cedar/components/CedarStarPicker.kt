package com.walcker.match.cedar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val StarTouchTarget = 48.dp
private val DefaultStarSize = 32.dp

@Composable
public fun CedarStarPicker(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    starContentDescription: (Int) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxStars: Int = 5,
    starSize: Dp = DefaultStarSize,
) {
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(maxStars) { index ->
            val value = index + 1
            val isFilled = value <= rating
            val label = starContentDescription(value)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(StarTouchTarget)
                    .selectable(
                        selected = value == rating,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onRatingChange(value) },
                    )
                    .semantics { contentDescription = label },
            ) {
                Icon(
                    imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = if (isFilled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    modifier = Modifier.size(starSize),
                )
            }
        }
    }
}
