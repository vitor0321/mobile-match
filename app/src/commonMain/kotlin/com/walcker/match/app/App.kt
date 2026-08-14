package com.walcker.match.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import com.walcker.match.cedar.CedarLoadingIndicator
import com.walcker.match.cedar.CedarTheme

@Composable
internal fun App(onFirstFrameRendered: (() -> Unit)? = null) {
    CedarTheme {
        val bootstrapState = remember(onFirstFrameRendered) {
            AppBootstrapState(onFirstFrameRendered = onFirstFrameRendered)
        }

        LaunchedEffect(bootstrapState) {
            withFrameNanos { }
            bootstrapState.markFirstFrameRendered()
        }

        if (bootstrapState.showBootstrapLoading) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                CedarLoadingIndicator()
            }
        } else {
            MatchScaffold()
        }
    }
}