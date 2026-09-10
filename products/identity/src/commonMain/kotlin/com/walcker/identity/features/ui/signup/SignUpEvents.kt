package com.walcker.identity.features.ui.signup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun SignUpEvents(
    stepModel: SignUpStepModel,
    content: @Composable (onEvent: (SignUpInternalRoute) -> Unit) -> Unit,
) {
    val onEvent = remember(stepModel) { { event: SignUpInternalRoute -> stepModel.onEvent(event) } }
    content(onEvent)
}
