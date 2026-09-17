package com.walcker.identity.data.verification

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.walcker.identity.features.data.verification.DataStoreEmailVerificationThrottle
import com.walcker.identity.features.data.verification.EMAIL_AUTO_SEND_WINDOW_MS
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataStoreEmailVerificationThrottleTest {
    private var nowMs = 1_000_000L

    private fun throttle() =
        DataStoreEmailVerificationThrottle(
            dataStore =
                PreferenceDataStoreFactory.createWithPath(
                    produceFile = { "build/test-email-throttle-${Random.nextInt()}.preferences_pb".toPath() },
                ),
            nowMs = { nowMs },
        )

    @Test
    fun `never sent means auto send`() =
        runTest {
            assertTrue(throttle().shouldAutoSend("uid-a"))
        }

    @Test
    fun `sent inside the window blocks auto send`() =
        runTest {
            val throttle = throttle()
            throttle.markSent("uid-a")

            nowMs += EMAIL_AUTO_SEND_WINDOW_MS - 1

            assertFalse(throttle.shouldAutoSend("uid-a"))
        }

    @Test
    fun `window elapsed allows auto send again`() =
        runTest {
            val throttle = throttle()
            throttle.markSent("uid-a")

            nowMs += EMAIL_AUTO_SEND_WINDOW_MS

            assertTrue(throttle.shouldAutoSend("uid-a"))
        }

    @Test
    fun `another account on the same device does not inherit the window`() =
        runTest {
            val throttle = throttle()
            throttle.markSent("uid-a")

            assertTrue(throttle.shouldAutoSend("uid-b"))
        }
}
