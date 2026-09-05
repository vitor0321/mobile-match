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

public enum class PlayerAvatarSize(
    public val dp: Dp,
) {
    Small(32.dp),

    Medium(48.dp),

    Large(80.dp),
}

@Composable
public fun PlayerAvatar(
    displayName: String,
    modifier: Modifier = Modifier,
    photoUrl: String? = null,
    size: PlayerAvatarSize = PlayerAvatarSize.Medium,
) {
    Surface(
        modifier =
            modifier
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

internal fun initialsOf(displayName: String): String {
    val words = displayName.trim().split(" ").filter { it.isNotBlank() }
    return when (words.size) {
        0 -> "?"
        1 -> words.first().take(1).uppercase()
        else -> (words.first().take(1) + words.last().take(1)).uppercase()
    }
}
