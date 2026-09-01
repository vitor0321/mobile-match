package com.walcker.games.features.data.shared.repository

import com.walcker.games.fake.FakeAvailabilitySource
import com.walcker.games.features.domain.shared.model.Availability
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvailabilityRepositoryImplTest {
    @Test
    fun `observe forwards the source's flow`() =
        runTest {
            val availability = Availability(isAvailable = true, availableUntilMs = 1_700_000_000_000L)
            val source = FakeAvailabilitySource(observeResult = flowOf(Result.success(availability)))
            val repository = AvailabilityRepositoryImpl(source)

            assertEquals(availability, repository.observe("user-1").first().getOrThrow())
        }

    @Test
    fun `setAvailable delegates to the source`() =
        runTest {
            val source = FakeAvailabilitySource()
            val repository = AvailabilityRepositoryImpl(source)

            val result = repository.setAvailable("user-1", Availability(isAvailable = true))

            assertTrue(result.isSuccess)
            assertEquals(1, source.setAvailableCallCount)
        }
}
