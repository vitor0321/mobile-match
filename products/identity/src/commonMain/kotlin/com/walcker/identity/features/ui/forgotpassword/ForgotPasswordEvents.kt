package com.walcker.identity.features.ui.forgotpassword

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun ForgotPasswordEvents(
    stepModel: ForgotPasswordStepModel,
    content: @Composable (onEvent: (ForgotPasswordInternalRoute) -> Unit) -> Unit,
) {
    val onEvent = remember(stepModel) { { event: ForgotPasswordInternalRoute -> stepModel.onEvent(event) } }
    content(onEvent)
}
