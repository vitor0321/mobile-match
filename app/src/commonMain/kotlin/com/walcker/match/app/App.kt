package com.walcker.match.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.walcker.match.cedar.CedarTheme
import com.walcker.match.navigator.HomeViewCoordinator
import org.koin.compose.koinInject

@Composable
internal fun App(onFirstFrameRendered: (() -> Unit)? = null) {
    CedarTheme {
        val homeViewCoordinator = koinInject<HomeViewCoordinator>()
        val isHomeDataReady by homeViewCoordinator.isHomeDataReady.collectAsState()

        LaunchedEffect(Unit) { onFirstFrameRendered?.invoke() }

        Box(modifier = Modifier.fillMaxSize()) {
            MatchScaffold()
            if (!isHomeDataReady) {
                SplashScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
