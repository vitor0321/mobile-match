package com.walcker.match.cedar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Read-only star rating, with half-star precision.
 *
 * Stateless by design: the caller owns the value. Rounding is visual only —
 * `4.3` draws four filled stars plus a half, `4.9` draws five.
 *
 * @param rating value in `0f..maxStars`; out-of-range values are clamped
 * @param contentDescription accessibility label; the individual icons are
 *        merged away so a screen reader announces the rating once
 */
@Composable
public fun RatingStars(
    rating: Float,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Dp = 16.dp,
    filledTint: Color = MaterialTheme.colorScheme.primary,
    emptyTint: Color = MaterialTheme.colorScheme.outlineVariant,
    contentDescription: String? = null,
) {
    val safeRating = rating.coerceIn(0f, maxStars.toFloat())

    Row(
        modifier = if (contentDescription != null) {
            modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
        } else {
            modifier
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(maxStars) { index ->
            val remainder = safeRating - index
            val (icon, tint) = when {
                remainder >= FULL_STAR_THRESHOLD -> Icons.Filled.Star to filledTint
                remainder >= HALF_STAR_THRESHOLD -> Icons.Filled.StarHalf to filledTint
                else -> Icons.Outlined.StarOutline to emptyTint
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(starSize),
            )
        }
    }
}

private const val FULL_STAR_THRESHOLD = 0.75f
private const val HALF_STAR_THRESHOLD = 0.25f
