package com.walcker.games.features.data.shared.source

import com.walcker.games.fake.DocumentWrite
import com.walcker.games.fake.FakeFirestoreClient
import com.walcker.games.fake.FakeLocationProvider
import com.walcker.games.fake.FakeSessionHolder
import com.walcker.games.fake.FunctionCall
import com.walcker.games.fake.testUserSession
import com.walcker.games.features.domain.shared.model.CancelMatchOutcome
import com.walcker.games.features.domain.shared.model.CreateMatchRequest
import com.walcker.games.features.domain.shared.model.JoinMatchOutcome
import com.walcker.games.features.domain.shared.model.RecurrenceOption
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.match.core.geo.Coordinates
import com.walcker.match.core.geo.DefaultCenter
import com.walcker.match.core.geo.encodeGeoHash
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreGameSourceTest {
    private val center = Coordinates(lat = -23.5505, lng = -46.6333)

    private val ruleRequiredOnCreate =
        setOf(
            "sport",
            "venueName",
            "address",
            "neighborhood",
            "city",
            "lat",
            "lng",
            "geohash",
            "startsAtSeconds",
            "durationMin",
            "totalSlots",
            "confirmedCount",
            "priceCents",
            "currencyCode",
            "status",
            "organizerName",
            "organizerId",
            "organizerRating",
            "organizerRatingCount",
            "matchRating",
            "matchRatingCount",
            "participants",
        )

    private val ruleEditableFields =
        setOf(
            "sport",
            "venueName",
            "address",
            "neighborhood",
            "city",
            "lat",
            "lng",
            "geohash",
            "startsAtSeconds",
            "durationMin",
            "totalSlots",
            "priceCents",
            "status",
            "recurrence",
            "seriesId",
            "teamCount",
            "teamAssignments",
            "playersPerTeam",
        )

    private fun buildSource(
        firestore: FakeFirestoreClient = FakeFirestoreClient(),
        sessionHolder: FakeSessionHolder = FakeSessionHolder(testUserSession(uid = "organizer-1", displayName = "Org")),
        locationProvider: FakeLocationProvider = FakeLocationProvider(locationResult = Result.success(center)),
    ) = FirestoreGameSource(firestore, sessionHolder, locationProvider)

    private fun request(
        pricePerPlayer: String? = null,
        recurrence: RecurrenceOption = RecurrenceOption.NONE,
    ) = CreateMatchRequest(
        sport = Sport.FUTSAL,
        venueName = "Green Ball",
        neighborhood = "Centro",
        city = "São Paulo",
        address = "Rua A, 1",
        lat = center.lat,
        lng = center.lng,
        geohash = encodeGeoHash(center),
        startsAtSeconds = 2_000_000_000L,
        durationMin = 60,
        totalPlayers = 10,
        recurrence = recurrence,
        pricePerPlayer = pricePerPlayer,
    )

    private fun matchDoc(
        at: Coordinates = center,
        status: String = "OPEN",
        startsAtSeconds: Long = 2_000_000_000L,
        participants: List<String> = emptyList(),
        omit: String? = null,
    ): Map<String, Any?> =
        mapOf(
            "sport" to "FUTSAL",
            "venueName" to "Green Ball",
            "neighborhood" to "Centro",
            "city" to "São Paulo",
            "address" to "Rua A, 1",
            "lat" to at.lat,
            "lng" to at.lng,
            "geohash" to encodeGeoHash(at),
            "startsAtSeconds" to startsAtSeconds,
            "durationMin" to 60L,
            "confirmedCount" to 0L,
            "totalSlots" to 10L,
            "priceCents" to 0L,
            "currencyCode" to "BRL",
            "organizerName" to "Org",
            "organizerId" to "organizer-1",
            "status" to status,
            "participants" to participants,
        ).filterKeys { it != omit }

    private fun FakeFirestoreClient.createdMatch(): Map<String, Any?> = writes.single { it.kind == DocumentWrite.Kind.ADD }.data

    @Test
    fun `a new match carries every field the security rules require`() =
        runTest {
            val firestore = FakeFirestoreClient()

            buildSource(firestore).createMatch(request())

            val written = firestore.createdMatch()
            assertEquals(emptySet(), ruleRequiredOnCreate - written.keys)
            assertEquals("OPEN", written["status"])
            assertEquals(0, written["confirmedCount"])
            assertEquals(emptyList<String>(), written["participants"])
            assertEquals("organizer-1", written["organizerId"])
            assertEquals("Org", written["organizerName"])
            assertEquals(3, (written["currencyCode"] as String).length)
        }

    @Test
    fun `numeric fields the rules check as int are written as integers`() =
        runTest {
            val firestore = FakeFirestoreClient()

            buildSource(firestore).createMatch(request(pricePerPlayer = "12.50"))

            val written = firestore.createdMatch()
            val integerFields = listOf("startsAtSeconds", "durationMin", "totalSlots", "confirmedCount", "priceCents", "organizerRatingCount", "matchRatingCount")
            for (field in integerFields) {
                val value = written[field]
                assertTrue(value is Int || value is Long, "$field should be an integer but was ${value?.let { it::class.simpleName }}")
            }
        }

    @Test
    fun `the price is stored in cents`() =
        runTest {
            val firestore = FakeFirestoreClient()

            buildSource(firestore).createMatch(request(pricePerPlayer = "12.50"))

            assertEquals(1250, firestore.createdMatch()["priceCents"])
        }

    @Test
    fun `no price or an unreadable price is a free match`() =
        runTest {
            for (price in listOf(null, "", "abc")) {
                val firestore = FakeFirestoreClient()
                buildSource(firestore).createMatch(request(pricePerPlayer = price))
                assertEquals(0, firestore.createdMatch()["priceCents"], "price '$price'")
            }
        }

    @Test
    fun `the organizer reputation is the rating as organizer, not as player`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed(
                        "profiles/organizer-1",
                        mapOf("rating" to 2L, "ratingCount" to 30L, "asOrganizerRating" to 4.5, "asOrganizerRatingCount" to 7L),
                    )
                }

            buildSource(firestore).createMatch(request())

            val written = firestore.createdMatch()
            assertEquals(4.5, written["organizerRating"])
            assertEquals(7, written["organizerRatingCount"])
        }

    @Test
    fun `the match rating comes from the template of the same organizer, venue and sport`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed(
                        "matchTemplates/t1",
                        mapOf("organizerId" to "organizer-1", "venueName" to "Green Ball", "sport" to "FUTSAL", "rating" to 4.5, "ratingCount" to 3L),
                    )
                    seed(
                        "matchTemplates/t2",
                        mapOf("organizerId" to "organizer-1", "venueName" to "Outra Quadra", "sport" to "FUTSAL", "rating" to 1.0, "ratingCount" to 9L),
                    )
                }

            buildSource(firestore).createMatch(request())

            val written = firestore.createdMatch()
            assertEquals(4.5, written["matchRating"])
            assertEquals(3, written["matchRatingCount"])
        }

    @Test
    fun `a first match without history starts with zero ratings`() =
        runTest {
            val firestore = FakeFirestoreClient()

            buildSource(firestore).createMatch(request())

            val written = firestore.createdMatch()
            assertEquals(0.0, written["organizerRating"])
            assertEquals(0.0, written["matchRating"])
        }

    @Test
    fun `a recurring match becomes the first of its own series`() =
        runTest {
            val firestore = FakeFirestoreClient()

            val id = buildSource(firestore).createMatch(request(recurrence = RecurrenceOption.WEEKLY))

            assertEquals(id, firestore.data("matches/$id")?.get("seriesId"))
        }

    @Test
    fun `a single match has no series`() =
        runTest {
            val firestore = FakeFirestoreClient()

            val id = buildSource(firestore).createMatch(request())

            assertFalse(firestore.data("matches/$id").orEmpty().containsKey("seriesId"))
        }

    @Test
    fun `creating a match without a signed in user fails`() =
        runTest {
            assertFailsWith<IllegalStateException> {
                buildSource(sessionHolder = FakeSessionHolder(session = null)).createMatch(request())
            }
        }

    @Test
    fun `editing a match only touches fields the rules let the organizer change`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { seed("matches/m1", matchDoc()) }

            buildSource(firestore).updateMatch("m1", request(pricePerPlayer = "20"))

            val update = firestore.writes.single { it.kind == DocumentWrite.Kind.UPDATE }
            assertEquals(emptySet(), update.data.keys - ruleEditableFields)
            assertEquals(2000, update.data["priceCents"])
            assertEquals("organizer-1", firestore.data("matches/m1")?.get("organizerId"))
        }

    @Test
    fun `saving teams only touches fields the rules let the organizer change`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { seed("matches/m1", matchDoc()) }

            buildSource(firestore).setTeamAssignments("m1", teamCount = 2, playersPerTeam = 5, assignments = mapOf("a" to 0, "b" to 1))

            val update = firestore.writes.single { it.kind == DocumentWrite.Kind.UPDATE }
            assertEquals(emptySet(), update.data.keys - ruleEditableFields)
            assertEquals(mapOf("a" to 0, "b" to 1), update.data["teamAssignments"])
        }

    @Test
    fun `joining maps every status the function answers with`() =
        runTest {
            val firestore = FakeFirestoreClient()
            val source = buildSource(firestore)

            firestore.functionResults["joinMatch"] = Result.success(mapOf("status" to "confirmed"))
            assertEquals(JoinMatchOutcome.Confirmed("m1"), source.joinGame("m1"))

            firestore.functionResults["joinMatch"] = Result.success(mapOf("status" to "waitlist", "position" to 3L))
            assertEquals(JoinMatchOutcome.Waitlist("m1", position = 3), source.joinGame("m1"))

            firestore.functionResults["joinMatch"] = Result.success(mapOf("status" to "already_joined"))
            assertEquals(JoinMatchOutcome.AlreadyJoined("m1"), source.joinGame("m1"))

            assertEquals(FunctionCall("joinMatch", mapOf("matchId" to "m1")), firestore.functionCalls.first())
        }

    @Test
    fun `an unknown join status is an error, not a silent success`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { functionResults["joinMatch"] = Result.success(mapOf("status" to "banana")) }

            assertFailsWith<IllegalStateException> { buildSource(firestore).joinGame("m1") }
        }

    @Test
    fun `a failed join is rethrown`() =
        runTest {
            val cause = IllegalStateException("full")
            val firestore = FakeFirestoreClient().apply { functionResults["joinMatch"] = Result.failure(cause) }

            val thrown = assertFailsWith<IllegalStateException> { buildSource(firestore).joinGame("m1") }
            assertEquals(cause, thrown)
        }

    @Test
    fun `leaving tells who was promoted from the waitlist`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { functionResults["leaveMatch"] = Result.success(mapOf("promotedUserId" to "p2")) }

            assertEquals("p2", buildSource(firestore).leaveMatch("m1").promotedUserId)
        }

    @Test
    fun `cancelling maps both statuses and rejects unknown ones`() =
        runTest {
            val firestore = FakeFirestoreClient()
            val source = buildSource(firestore)

            firestore.functionResults["cancelMatch"] = Result.success(mapOf("status" to "cancelled"))
            assertEquals(CancelMatchOutcome.Cancelled("m1"), source.cancelMatch("m1"))

            firestore.functionResults["cancelMatch"] = Result.success(mapOf("status" to "already_cancelled"))
            assertEquals(CancelMatchOutcome.AlreadyCancelled("m1"), source.cancelMatch("m1"))

            firestore.functionResults["cancelMatch"] = Result.success(mapOf("status" to "???"))
            assertFailsWith<IllegalStateException> { source.cancelMatch("m1") }
        }

    @Test
    fun `organizer actions call their functions with the expected arguments`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { functionResults["banPlayerFromMatch"] = Result.success(mapOf("promotedUserId" to "p9")) }
            val source = buildSource(firestore)

            source.setVipStatus("m1", "p1", isVip = true)
            source.confirmWaitlistedPlayer("m1", "p2")
            val promoted = source.banPlayerFromMatch("m1", "p3")
            source.cancelMatchSeries("m1")

            assertEquals(
                listOf(
                    FunctionCall("setVipStatus", mapOf("matchId" to "m1", "targetUserId" to "p1", "isVip" to true)),
                    FunctionCall("confirmWaitlistedPlayer", mapOf("matchId" to "m1", "targetUserId" to "p2")),
                    FunctionCall("banPlayerFromMatch", mapOf("matchId" to "m1", "targetUserId" to "p3")),
                    FunctionCall("cancelMatchSeries", mapOf("matchId" to "m1")),
                ),
                firestore.functionCalls,
            )
            assertEquals("p9", promoted)
        }

    @Test
    fun `a failed organizer action is rethrown`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { functionResults["setVipStatus"] = Result.failure(IllegalStateException("denied")) }

            assertFailsWith<IllegalStateException> { buildSource(firestore).setVipStatus("m1", "p1", isVip = true) }
        }

    @Test
    fun `my matches are the ones I take part in, soonest first`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed("matches/late", matchDoc(startsAtSeconds = 3_000L, participants = listOf("me")))
                    seed("matches/early", matchDoc(startsAtSeconds = 1_000L, participants = listOf("me", "x")))
                    seed("matches/other", matchDoc(startsAtSeconds = 2_000L, participants = listOf("x")))
                }

            val games = buildSource(firestore).matchesForUser("me")

            assertEquals(listOf("early", "late"), games.map { it.id })
        }

    @Test
    fun `my matches degrade to an empty list when the query fails`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { failingPaths["matches"] = IllegalStateException("offline") }

            assertEquals(emptyList(), buildSource(firestore).matchesForUser("me"))
        }

    @Test
    fun `nearby matches are open, inside the radius and sorted by distance`() =
        runTest {
            val near = Coordinates(lat = center.lat + 0.01, lng = center.lng)
            val nearer = Coordinates(lat = center.lat + 0.001, lng = center.lng)
            val far = Coordinates(lat = center.lat + 0.5, lng = center.lng)
            val firestore =
                FakeFirestoreClient().apply {
                    seed("matches/near", matchDoc(at = near))
                    seed("matches/nearer", matchDoc(at = nearer))
                    seed("matches/far", matchDoc(at = far))
                    seed("matches/cancelled", matchDoc(at = nearer, status = "CANCELLED"))
                }

            val page = buildSource(firestore).openGamesNear(center, radiusKm = 5.0, cursors = null)

            assertEquals(listOf("nearer", "near"), page.games.map { it.id })
        }

    @Test
    fun `without location permission the search is centered on the default city`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { seed("matches/default", matchDoc(at = DefaultCenter)) }
            val source = buildSource(firestore, locationProvider = FakeLocationProvider(permissionGranted = false))

            val page = source.openGames(radiusKm = 5.0, cursors = null)

            assertEquals(listOf("default"), page.games.map { it.id })
        }

    @Test
    fun `a malformed match in a full page does not stop the pagination`() =
        runTest {
            val firestore = FakeFirestoreClient()
            repeat(30) { index ->
                val spot = Coordinates(lat = center.lat + index * 0.0001, lng = center.lng)
                firestore.seed("matches/m$index", matchDoc(at = spot, omit = if (index == 5) "venueName" else null))
            }
            firestore.seed("matches/m30", matchDoc(at = Coordinates(lat = center.lat + 30 * 0.0001, lng = center.lng)))
            val source = buildSource(firestore)

            val first = source.openGamesNear(center, radiusKm = 5.0, cursors = null)
            val second = source.openGamesNear(center, radiusKm = 5.0, cursors = first.rangeCursors)

            val seen = (first.games + second.games).map { it.id }.toSet()
            assertTrue("m30" in seen, "a match after the full page was never loaded; cursors were ${first.rangeCursors}")
        }

    @Test
    fun `many matches at the same address all show up across pages`() =
        runTest {
            val firestore = FakeFirestoreClient()
            repeat(31) { index -> firestore.seed("matches/same-$index", matchDoc()) }
            val source = buildSource(firestore)

            val first = source.openGamesNear(center, radiusKm = 5.0, cursors = null)
            val second = source.openGamesNear(center, radiusKm = 5.0, cursors = first.rangeCursors)

            assertEquals(31, (first.games + second.games).map { it.id }.toSet().size)
        }

    @Test
    fun `a match is fetched by id`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { seed("matches/m1", matchDoc()) }

            assertEquals("m1", buildSource(firestore).getGameById("m1").id)
        }

    @Test
    fun `a missing match is an error`() =
        runTest {
            assertFailsWith<IllegalStateException> { buildSource().getGameById("nope") }
        }

    @Test
    fun `observing a match that disappears reports a failure`() =
        runTest {
            val result = buildSource().observeMatch("gone").first()

            assertTrue(result.isFailure)
        }

    @Test
    fun `participants are split into confirmed and a waitlist ordered by position`() =
        runTest {
            val firestore =
                FakeFirestoreClient().apply {
                    seed("matches/m1/participants/a", mapOf("userId" to "a", "joinedAt" to 1L, "isConfirmed" to true))
                    seed("matches/m1/participants/w2", mapOf("userId" to "w2", "joinedAt" to 2L, "isConfirmed" to false, "positionInWaitlist" to 2L))
                    seed("matches/m1/participants/w1", mapOf("userId" to "w1", "joinedAt" to 3L, "isConfirmed" to false, "positionInWaitlist" to 1L))
                    seed("matches/m1/participants/b", mapOf("userId" to "b", "joinedAt" to 4L, "isConfirmed" to true))
                }

            val summary = buildSource(firestore).observeParticipants("m1").first().getOrThrow()

            assertEquals(listOf("a", "b"), summary.confirmed.map { it.userId })
            assertEquals(listOf("w1", "w2"), summary.waitlist.map { it.userId })
            assertEquals(2, summary.confirmedCount)
        }

    @Test
    fun `a participant without a name still shows up`() =
        runTest {
            val firestore = FakeFirestoreClient().apply { seed("matches/m1/participants/a", mapOf("joinedAt" to 1L)) }

            val summary = buildSource(firestore).observeParticipants("m1").first().getOrThrow()

            val participant = summary.confirmed.single()
            assertEquals("a", participant.userId)
            assertNull(participant.photoUrl)
            assertIs<String>(participant.displayName)
        }
}
