package com.walcker.identity.features.ui.paywall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

@Composable
internal fun PaywallEvents(
    stepModel: PaywallStepModel,
    onDismiss: () -> Unit,
    onRequireLogin: () -> Unit,
    onShowSnackbar: suspend (String) -> Unit,
    content: @Composable (onEvent: (PaywallInternalRoute) -> Unit) -> Unit,
) {
    val onEvent = remember(stepModel) { { event: PaywallInternalRoute -> stepModel.onEvent(event) } }

    LaunchedEffect(stepModel) {
        stepModel.events.collect { event ->
            when (event) {
                PaywallInternalEvents.Dismiss -> onDismiss()
                PaywallInternalEvents.RequireLogin -> onRequireLogin()
                is PaywallInternalEvents.ShowSnackbar -> onShowSnackbar(event.message)
            }
        }
    }

    content(onEvent)
}