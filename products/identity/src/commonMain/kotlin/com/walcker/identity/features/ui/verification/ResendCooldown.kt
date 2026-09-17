package com.walcker.identity.features.ui.verification

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val RESEND_COOLDOWN_SECONDS = 60

private const val ONE_SECOND_MS = 1_000L

internal fun CoroutineScope.launchResendCooldown(onTick: (remainingSeconds: Int) -> Unit): Job =
    launch {
        for (remaining in RESEND_COOLDOWN_SECONDS downTo 1) {
            onTick(remaining)
            delay(ONE_SECOND_MS)
        }
        onTick(0)
    }
