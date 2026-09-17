package com.walcker.identity.features.ui.verification.phone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun PhoneVerificationEvents(
    stepModel: PhoneVerificationStepModel,
    content: @Composable (onEvent: (PhoneVerificationInternalRoute) -> Unit) -> Unit,
) {
    val onEvent = remember(stepModel) { { event: PhoneVerificationInternalRoute -> stepModel.onEvent(event) } }
    content(onEvent)
}
