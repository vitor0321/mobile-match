package com.walcker.match.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class AppBootstrapState(
    private val onFirstFrameRendered: (() -> Unit)? = null,
) {
    var showBootstrapLoading by mutableStateOf(true)
        private set

    fun markFirstFrameRendered() {
        if (!showBootstrapLoading) return
        onFirstFrameRendered?.invoke()
        showBootstrapLoading = false
    }
}
