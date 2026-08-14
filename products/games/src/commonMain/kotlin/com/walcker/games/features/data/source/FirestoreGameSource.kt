package com.walcker.games.features.data.source

import com.walcker.games.features.data.mapper.toGame
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

