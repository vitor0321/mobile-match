package com.walcker.identity.fake

import com.walcker.identity.features.data.verification.EmailVerificationThrottle

internal class FakeEmailVerificationThrottle(
    var shouldAutoSend: Boolean = true,
) : EmailVerificationThrottle {
    val markedSentUids: MutableList<String> = mutableListOf()

    override suspend fun shouldAutoSend(uid: String): Boolean = shouldAutoSend

    override suspend fun markSent(uid: String) {
        markedSentUids += uid
    }
}
