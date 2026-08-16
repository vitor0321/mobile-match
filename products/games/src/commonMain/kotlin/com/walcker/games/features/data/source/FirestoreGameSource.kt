package com.walcker.games.features.data.source

import com.walcker.games.features.data.mapper.toGame
import com.walcker.games.features.data.mapper.toParticipant
import com.walcker.games.features.domain.model.CreateMatchRequest
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.Participant
import com.walcker.games.features.domain.model.ParticipantsSummary
import com.walcker.identity.api.SessionHolder
import com.walcker.match.core.geo.Coordinates
import com.walcker.match.core.geo.boundsForRadius
import com.walcker.match.core.geo.distanceKm
import com.walcker.match.firestore.FirestoreClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Queries Firestore for open matches within a given radius, ordered by distance.
 *
 * Implementation strategy:
 * 1. Calculate geohash bounds for the radius from the user's location
 * 2. Query matches by geohash ranges (typically 4-9 queries)
 * 3. Filter client-side by exact distance (circle within box)
 * 4. Sort by distance, closest first
 *
 * Mirrors the GeoFire algorithm from core/geo.
 */
internal class FirestoreGameSource(
    private val firestore: FirestoreClient,
    private val sessionHolder: SessionHolder,
) : GameSource {

    override suspend fun openGames(): List<Game> {
        // TODO: Get user's current location from SessionHolder or LocationProvider
        // For now, use São Paulo center (debug)
        val userLocation = Coordinates(lat = -23.5505, lng = -46.6333)
        val radiusKm = 15.0 // TODO: Get from DataStore preferences

        return queryNearbyMatches(userLocation, radiusKm)
    }

    override suspend fun joinGame(gameId: String) {
        // TODO: Implement callable to joinMatch
        // This will be a Cloud Function callable that handles transactional join
        throw NotImplementedError("joinMatch callable not yet implemented")
    }

    override suspend fun createMatch(request: CreateMatchRequest): String {
        // Resolve the organizer (current authenticated user). If there is no
        // session we cannot attribute the match, so we refuse rather than
        // write a document owned by a placeholder id.
        val session = sessionHolder.currentUser.first()
            ?: throw IllegalStateException("Cannot create match: no authenticated user")
        val organizerId = session.uid
        val organizerName = session.displayName ?: "Anonymous"

        // Map CreateMatchRequest to Firestore document data (without ID, will be generated)
        val data = mapOf(
            "sport" to request.sport.name,
            "venueName" to request.venueName,
            "neighborhood" to request.neighborhood,
            "city" to request.city,
            "address" to request.address,
            "lat" to request.lat,
            "lng" to request.lng,
            "geohash" to request.geohash,
            "startsAtSeconds" to request.startsAtSeconds,
            "durationMin" to request.durationMin,
            "confirmedPlayers" to 1, // Organizer is the first player
            "totalPlayers" to request.totalPlayers,
            "pricePerPlayer" to request.pricePerPlayer,
            "status" to "OPEN",
            "organizerName" to organizerName,
            "organizerId" to organizerId,
            "organizerRating" to 4.5, // TODO: Get from user profile
            "participants" to listOf(organizerId), // Organizer is auto-added
        )

        // Add the document (Firestore auto-generates the ID)
        return firestore.collection("matches").add(data).getOrThrow()
    }

    override suspend fun matchesForUser(userId: String): List<Game> {
        // Firestore `array-contains` lets us query matches where userId appears
        // in the denormalized `participants` array. We do not constrain by status
        // here — the use case decides active vs past.
        return firestore
            .collection("matches")
            .query()
            .where("participants", "array-contains", userId)
            .orderBy("startsAtSeconds")
            .get()
            .getOrNull()
            ?.mapNotNull { snapshot -> snapshot.toGame() }
            ?: emptyList()
    }

    private suspend fun queryNearbyMatches(
        center: Coordinates,
        radiusKm: Double,
    ): List<Game> {
        // Get geohash bounds that cover the circle
        val bounds = boundsForRadius(center, radiusKm)

        // Query all matching ranges in parallel
        val allMatches = coroutineScope {
            bounds.map { range ->
                async {
                    queryRange(range.start, range.endInclusive)
                }
            }.awaitAll().flatten()
        }

        // Filter by exact distance (remove matches outside the circle)
        val nearbyMatches = allMatches.filter { game ->
            val gameLocation = Coordinates(lat = game.lat, lng = game.lng)
            val distanceKm = distanceKm(center, gameLocation)
            distanceKm <= radiusKm
        }

        // Sort by distance (closest first)
        return nearbyMatches
            .sortedBy { game ->
                val gameLocation = Coordinates(lat = game.lat, lng = game.lng)
                distanceKm(center, gameLocation)
            }
    }

    private suspend fun queryRange(startHash: String, endHash: String): List<Game> {
        return firestore
            .collection("matches")
            .query()
            .where("status", "==", "OPEN") // Only open matches (Firestore stores as uppercase enum)
            .where("geohash", ">=", startHash)
            .where("geohash", "<=", endHash)
            .orderBy("geohash")
            .get()
            .getOrNull()
            ?.mapNotNull { snapshot -> snapshot.toGame() }
            ?: emptyList()
    }

    override suspend fun getGameById(gameId: String): Game {
        return firestore
            .document("matches/$gameId")
            .get()
            .getOrNull()
            ?.toGame()
            ?: throw IllegalStateException("Match not found: $gameId")
    }

    override fun observeParticipants(matchId: String): Flow<Result<ParticipantsSummary>> {
        // Observe the participants subcollection in real-time. Each participant
        // doc denormalizes its status (confirmed/waitlist) and position so the UI
        // does not need a second round-trip to the match doc.
        return firestore
            .collection("matches/$matchId/participants")
            .query()
            .orderBy("joinedAt")
            .snapshots()
            .map { snapshotResult ->
                snapshotResult.map { snapshots ->
                    val participants = snapshots.mapNotNull { it.toParticipant() }
                    val confirmed = participants.filter { it.isConfirmed }
                    val waitlist = participants
                        .filter { !it.isConfirmed }
                        .sortedBy { it.positionInWaitlist ?: Int.MAX_VALUE }

                    // totalSlots is denormalized on each participant doc; fall back
                    // to confirmed size when absent (e.g. very first snapshot).
                    val totalSlots = snapshots
                        .firstNotNullOfOrNull { it.getLong("totalSlots")?.toInt() }
                        ?: confirmed.size

                    ParticipantsSummary(
                        confirmed = confirmed,
                        waitlist = waitlist,
                        confirmedCount = confirmed.size,
                        totalSlots = totalSlots.coerceAtLeast(confirmed.size),
                    )
                }
            }
    }
}

