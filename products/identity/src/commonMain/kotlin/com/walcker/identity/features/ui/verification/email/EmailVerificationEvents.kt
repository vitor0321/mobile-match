package com.walcker.identity.features.ui.verification.email

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun EmailVerificationEvents(
    stepModel: EmailVerificationStepModel,
    content: @Composable (onEvent: (EmailVerificationInternalRoute) -> Unit) -> Unit,
) {
    val onEvent = remember(stepModel) { { event: EmailVerificationInternalRoute -> stepModel.onEvent(event) } }
    content(onEvent)
}
