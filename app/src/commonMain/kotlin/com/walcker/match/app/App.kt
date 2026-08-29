package com.walcker.match.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.walcker.match.cedar.CedarTheme

@Composable
internal fun App(onFirstFrameRendered: (() -> Unit)? = null) {
    CedarTheme {
        LaunchedEffect(Unit) { onFirstFrameRendered?.invoke() }
        MatchScaffold()
    }
}