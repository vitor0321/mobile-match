package com.walcker.games.features.data.shared.source

import com.walcker.games.fake.DocumentWrite
import com.walcker.games.fake.FakeFirestoreClient
import com.walcker.games.features.domain.shared.model.Availability
import com.walcker.games.features.domain.shared.model.Sport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreAvailabilitySourceTest {
    private val privatePath = "profiles/u1/private/data"
    private val profilePath = "profiles/u1"

    private val profileEditableFields =
        setOf("fullName", "nickname", "avatarUrl", "position", "level", "sports", "city", "neighborhood", "updatedAt")

    @Test
    fun `someone without private data is unavailable`() =
        runTest {
            val availability = FirestoreAvailabilitySource(FakeFirestoreClient()).observe("u1").first().getOrThrow()

            assertEquals(Availability.Unavailable, availability)
        }

    @Test
    fun `availability, its deadline and the sports are read back`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed(privatePath, mapOf("isAvailable" to true, "availableUntil" to 1_700_000_000_000L, "availableSports" to listOf("FUTSAL", "VOLEI")))
                }

            val availability = FirestoreAvailabilitySource(firestore).observe("u1").first().getOrThrow()

            assertTrue(availability.isAvailable)
            assertEquals(1_700_000_000_000L, availability.availableUntilMs)
            assertEquals(setOf(Sport.FUTSAL, Sport.VOLEI), availability.sports)
        }

    @Test
    fun `a sport the app no longer knows is ignored instead of breaking the profile`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { seed(privatePath, mapOf("availableSports" to listOf("FUTSAL", "CURLING", 3L))) }

            val availability = FirestoreAvailabilitySource(firestore).observe("u1").first().getOrThrow()

            assertEquals(setOf(Sport.FUTSAL), availability.sports)
        }

    @Test
    fun `turning availability on keeps the rest of the private data`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { seed(privatePath, mapOf("lat" to -23.5, "radiusKm" to 15L)) }

            FirestoreAvailabilitySource(firestore).setAvailable("u1", isAvailable = true, availableUntilMs = null)

            val data = firestore.data(privatePath).orEmpty()
            assertEquals(true, data["isAvailable"])
            assertNull(data["availableUntil"])
            assertEquals(-23.5, data["lat"])
            assertEquals(15L, data["radiusKm"])
        }

    @Test
    fun `the private data writes never touch the phone the rules protect`() =
        runTest {
            val firestore = FakeFirestoreClient()
            val source = FirestoreAvailabilitySource(firestore)

            source.setAvailable("u1", isAvailable = false, availableUntilMs = 1L)
            source.setAvailableSports("u1", setOf(Sport.FUTSAL))

            val privateWrites = firestore.writes.filter { it.path == privatePath }
            assertTrue(privateWrites.isNotEmpty())
            assertTrue(privateWrites.all { it.kind == DocumentWrite.Kind.MERGE })
            assertFalse(privateWrites.any { "phone" in it.data })
        }

    @Test
    fun `favorite sports are mirrored on the public profile with an editable field only`() =
        runTest {
            val firestore = FakeFirestoreClient()

            FirestoreAvailabilitySource(firestore).setAvailableSports("u1", setOf(Sport.FUTSAL, Sport.SOCIETY))

            assertEquals(setOf("FUTSAL", "SOCIETY"), (firestore.data(privatePath)?.get("availableSports") as List<*>).toSet())
            val profileWrite = firestore.writes.single { it.path == profilePath }
            assertEquals(DocumentWrite.Kind.MERGE, profileWrite.kind)
            assertEquals(emptySet(), profileWrite.data.keys - profileEditableFields)
            assertEquals(setOf("FUTSAL", "SOCIETY"), (profileWrite.data["sports"] as List<*>).toSet())
        }

    @Test
    fun `when the private write fails the public profile is left alone`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { failingPaths[privatePath] = IllegalStateException("offline") }

            val result = FirestoreAvailabilitySource(firestore).setAvailableSports("u1", setOf(Sport.FUTSAL))

            assertTrue(result.isFailure)
            assertTrue(firestore.writes.none { it.path == profilePath })
        }
}
