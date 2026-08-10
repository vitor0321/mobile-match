package com.walcker.identity.features.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun LoginEvents(
    stepModel: LoginStepModel,
    content: @Composable (onEvent: (LoginInternalRoute) -> Unit) -> Unit,
) {
    val onEvent = remember(stepModel) { { event: LoginInternalRoute -> stepModel.onEvent(event) } }
    content(onEvent)
}

