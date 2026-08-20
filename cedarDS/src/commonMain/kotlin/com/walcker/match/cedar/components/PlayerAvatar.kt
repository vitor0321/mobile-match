package com.walcker.match.cedar.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/** Avatar sizes used across the app. */
public enum class PlayerAvatarSize(public val dp: Dp) {
    /** 32dp — stacked participant lists. */
    Small(32.dp),

    /** 48dp — search results, rating cards. */
    Medium(48.dp),

    /** 80dp — the profile header. */
    Large(80.dp),
}

/**
 * A player's photo, or their initials when there is no photo.
 *
 * Today four screens each roll their own version of this, and three of them show
 * an empty grey circle when `photoUrl` is null. Initials are a better default:
 * they identify the person and they never fail to load.
 *
 * @param displayName used for the initials and for the accessibility label.
 */
@Composable
public fun PlayerAvatar(
    displayName: String,
    modifier: Modifier = Modifier,
    photoUrl: String? = null,
    size: PlayerAvatarSize = PlayerAvatarSize.Medium,
) {
    Surface(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        if (!photoUrl.isNullOrEmpty()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size.dp),
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initialsOf(displayName),
                    // Scales with the circle instead of a fixed style, so the
                    // 32dp and the 80dp avatars stay visually consistent.
                    fontSize = (size.dp.value * INITIALS_RATIO).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private const val INITIALS_RATIO = 0.4f

/**
 * First letter of the first and last word — "João Pedro Silva" becomes "JS".
 * Falls back to a single character, and to "?" for a blank name.
 */
internal fun initialsOf(displayName: String): String {
    val words = displayName.trim().split(" ").filter { it.isNotBlank() }
    return when (words.size) {
        0 -> "?"
        1 -> words.first().take(1).uppercase()
        else -> (words.first().take(1) + words.last().take(1)).uppercase()
    }
}
