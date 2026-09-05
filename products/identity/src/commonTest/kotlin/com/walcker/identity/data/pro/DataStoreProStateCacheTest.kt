package com.walcker.identity.data.pro

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.walcker.identity.features.data.pro.DataStoreProStateCache
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataStoreProStateCacheTest {
    @Test
    fun `When saving Pro state should persist round trip then read returns saved value`() =
        runTest {
            val dataStore =
                PreferenceDataStoreFactory.createWithPath(
                    produceFile = {
                        "build/test-pro-state-${Random.nextInt()}.preferences_pb".toPath()
                    },
                )
            val cache = DataStoreProStateCache(dataStore = dataStore)

            assertFalse(cache.read("uid-a"))

            cache.save("uid-a", true)

            assertTrue(cache.read("uid-a"))
        }

    @Test
    fun `When two users save state then each reads only its own namespace`() =
        runTest {
            val dataStore =
                PreferenceDataStoreFactory.createWithPath(
                    produceFile = {
                        "build/test-pro-state-users-${Random.nextInt()}.preferences_pb".toPath()
                    },
                )
            val cache = DataStoreProStateCache(dataStore = dataStore)

            cache.save("uid-a", true)
            cache.saveRegistrationDate("uid-a", "2026-08-01")
            cache.save("uid-b", false)

            assertTrue(cache.read("uid-a"))
            assertFalse(cache.read("uid-b"))
            assertTrue(cache.readRegistrationDate("uid-a") != null)
            assertTrue(cache.readRegistrationDate("uid-b") == null)
        }
}
