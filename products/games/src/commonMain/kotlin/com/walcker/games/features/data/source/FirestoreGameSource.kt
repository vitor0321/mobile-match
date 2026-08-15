package com.walcker.games.features.data.source

import com.walcker.games.features.data.mapper.toGame
import com.walcker.games.features.domain.model.CreateMatchRequest
import com.walcker.games.features.domain.model.Game
import com.walcker.match.core.geo.Coordinates
import com.walcker.match.core.geo.boundsForRadius
import com.walcker.match.core.geo.distanceKm
import com.walcker.match.firestore.FirestoreClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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
            "organizerName" to "Anonymous", // TODO: Get from SessionHolder
            "organizerId" to "user123", // TODO: Get from SessionHolder
            "organizerRating" to 4.5, // TODO: Get from user profile
            "participants" to listOf("user123"), // Organizer is auto-added
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
}

