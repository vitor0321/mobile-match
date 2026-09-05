@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import androidx.compose.runtime.Composable
import com.walcker.match.cedar.CedarTheme

@Composable
internal fun GamesSnapshotTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    CedarTheme(darkTheme = darkTheme) {
        content()
    }
}
