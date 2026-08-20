package com.walcker.match.cedar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.walcker.match.cedar.tokens.CedarTokens

/**
 * How many slots a match still has — the single most important fact in a list of
 * matches, and the reason green exists in this design system.
 *
 * Deliberately not shaped like a button. No elevation, no border, no ripple: in
 * the Figma the pill sits where a CTA would sit, and the first thing to check in
 * usability testing is whether people try to tap it.
 *
 * Two changes from the mockup:
 * - **Text comes from the caller.** The old version built "2 vagas" / "LOTADO"
 *   inside the design system, which put pt-BR copy in a module that has no strings
 *   layer and cannot be translated.
 * - **12sp floor.** The Figma sets this at 10sp and 11sp.
 *
 * @param label already pluralised by the caller: "2 vagas", "1 vaga", "Lotado".
 * @param openSlots drives the colour only. Zero means the match is full.
 */
@Composable
public fun SlotBadge(
    label: String,
    openSlots: Int,
    modifier: Modifier = Modifier,
) {
    val hasRoom = openSlots > 0
    val background = if (hasRoom) {
        CedarTokens.colors.available
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val foreground = if (hasRoom) {
        CedarTokens.colors.onAvailable
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = label,
        color = foreground,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(color = background, shape = CedarTokens.radius.pill)
            .padding(
                horizontal = CedarTokens.spacing.sm,
                vertical = CedarTokens.spacing.xs,
            ),
    )
}
