package com.walcker.identity.features.data.verification

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first

internal const val EMAIL_AUTO_SEND_WINDOW_MS = 600_000L

internal interface EmailVerificationThrottle {
    suspend fun shouldAutoSend(uid: String): Boolean

    suspend fun markSent(uid: String)
}

internal class DataStoreEmailVerificationThrottle(
    private val dataStore: DataStore<Preferences>,
    private val nowMs: () -> Long,
) : EmailVerificationThrottle {
    override suspend fun shouldAutoSend(uid: String): Boolean {
        val lastSentAtMs = dataStore.data.first()[sentAtKey(uid)] ?: return true
        return nowMs() - lastSentAtMs >= EMAIL_AUTO_SEND_WINDOW_MS
    }

    override suspend fun markSent(uid: String) {
        dataStore.edit { preferences ->
            preferences[sentAtKey(uid)] = nowMs()
        }
    }
}

private fun sentAtKey(uid: String) = longPreferencesKey("email_verification_sent_at_$uid")
