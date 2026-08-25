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
 * What a tag means, which is the only thing a call site should have to decide.
 */
public enum class CedarTagTone {
    /** Neutral fact: a role, a category. */
    Neutral,

    /** Something the user should notice but not act on: "Aberta". */
    Info,

    /** There are slots left. Same green as [SlotBadge], same meaning. */
    Available,

    /** Cancelled, full, expired. */
    Danger,
}

/**
 * A small non-interactive label: role badges, match status, category chips.
 *
 * Not a chip — chips are tappable, and every one of these is read-only. The app
 * used a disabled `AssistChip` for the role badge, which announces itself to a
 * screen reader as a disabled button and invites a tap that does nothing.
 */
@Composable
public fun CedarTag(
    label: String,
    modifier: Modifier = Modifier,
    tone: CedarTagTone = CedarTagTone.Neutral,
) {
    val background = when (tone) {
        CedarTagTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant
        CedarTagTone.Info -> MaterialTheme.colorScheme.primaryContainer
        CedarTagTone.Available -> CedarTokens.colors.availableContainer
        CedarTagTone.Danger -> MaterialTheme.colorScheme.errorContainer
    }
    val foreground = when (tone) {
        CedarTagTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        CedarTagTone.Info -> MaterialTheme.colorScheme.onPrimaryContainer
        CedarTagTone.Available -> CedarTokens.colors.availableText
        CedarTagTone.Danger -> MaterialTheme.colorScheme.onErrorContainer
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
                vertical = CedarTokens.spacing.xxs,
            ),
    )
}
