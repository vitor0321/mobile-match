package com.walcker.games.features.data.shared.source

import com.walcker.games.features.data.shared.mapper.toGame
import com.walcker.games.features.data.shared.mapper.toParticipant
import com.walcker.games.features.domain.shared.model.CancelMatchOutcome
import com.walcker.games.features.domain.shared.model.CreateMatchRequest
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.JoinMatchOutcome
import com.walcker.games.features.domain.shared.model.LeaveMatchOutcome
import com.walcker.games.features.domain.shared.model.ParticipantsSummary
import com.walcker.games.features.domain.shared.model.RecurrenceOption
import com.walcker.identity.api.SessionHolder
import com.walcker.match.core.geo.Coordinates
import com.walcker.match.core.geo.DefaultCenter
import com.walcker.match.core.geo.boundsForRadius
import com.walcker.match.core.geo.distanceKm
import com.walcker.match.core.location.LocationProvider
import com.walcker.match.firestore.FirestoreClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

internal class FirestoreGameSource(
    private val firestore: FirestoreClient,
    private val sessionHolder: SessionHolder,
    private val locationProvider: LocationProvider,
) : GameSource {
    override suspend fun openGames(radiusKm: Double): List<Game> {
        val userLocation = resolveUserLocation()
        return queryNearbyMatches(userLocation, radiusKm)
    }

    private suspend fun resolveUserLocation(): Coordinates {
        if (!locationProvider.requestPermission()) return DefaultCenter
        return locationProvider.currentLocation().getOrNull() ?: DefaultCenter
    }

    override suspend fun joinGame(gameId: String): JoinMatchOutcome {
        val result =
            firestore.callFunction(
                name = "joinMatch",
                data = mapOf("matchId" to gameId),
            )
        return result.fold(
            onSuccess = { payload -> mapJoinMatchResponse(gameId, payload) },
            onFailure = { error -> throw error },
        )
    }

    private fun mapJoinMatchResponse(
        matchId: String,
        payload: Map<String, Any?>,
    ): JoinMatchOutcome =
        when (payload["status"]) {
            "confirmed" -> JoinMatchOutcome.Confirmed(matchId)
            "waitlist" -> {
                val position = (payload["position"] as? Number)?.toInt() ?: 0
                JoinMatchOutcome.Waitlist(matchId, position = position)
            }
            "already_joined" -> JoinMatchOutcome.AlreadyJoined(matchId)
            else -> throw IllegalStateException(
                "Unexpected joinMatch response status: ${payload["status"]}",
            )
        }

    override suspend fun leaveMatch(gameId: String): LeaveMatchOutcome {
        val result =
            firestore.callFunction(
                name = "leaveMatch",
                data = mapOf("matchId" to gameId),
            )
        return result.fold(
            onSuccess = { payload ->
                LeaveMatchOutcome(
                    matchId = gameId,
                    promotedUserId = payload["promotedUserId"] as? String,
                )
            },
            onFailure = { error -> throw error },
        )
    }

    override suspend fun cancelMatch(gameId: String): CancelMatchOutcome {
        val result =
            firestore.callFunction(
                name = "cancelMatch",
                data = mapOf("matchId" to gameId),
            )
        return result.fold(
            onSuccess = { payload ->
                when (payload["status"]) {
                    "cancelled" -> CancelMatchOutcome.Cancelled(gameId)
                    "already_cancelled" -> CancelMatchOutcome.AlreadyCancelled(gameId)
                    else -> throw IllegalStateException(
                        "Unexpected cancelMatch response status: ${payload["status"]}",
                    )
                }
            },
            onFailure = { error -> throw error },
        )
    }

    override suspend fun createMatch(request: CreateMatchRequest): String {
        val session =
            sessionHolder.currentUser.first()
                ?: throw IllegalStateException("Cannot create match: no authenticated user")
        val organizerId = session.uid
        val organizerName = session.displayName ?: "Anonymous"

        val data =
            mapOf(
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
                "recurrence" to request.recurrence.name,
                "confirmedCount" to 1,
                "totalSlots" to request.totalPlayers,
                "priceCents" to (
                    request.pricePerPlayer
                        ?.toDoubleOrNull()
                        ?.times(100)
                        ?.roundToInt() ?: 0
                ),
                "status" to "OPEN",
                "organizerName" to organizerName,
                "organizerId" to organizerId,
                "organizerRating" to 4.5, // TODO: Get from user profile
                "participants" to listOf(organizerId),
            )

        val matchId = firestore.collection("matches").add(data).getOrThrow()

        if (request.recurrence != RecurrenceOption.NONE) {
            firestore.document("matches/$matchId").update(mapOf("seriesId" to matchId)).getOrThrow()
        }

        return matchId
    }

    override suspend fun updateMatch(
        matchId: String,
        request: CreateMatchRequest,
    ) {
        val data =
            mapOf(
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
                "recurrence" to request.recurrence.name,
                "totalSlots" to request.totalPlayers,
                "priceCents" to (
                    request.pricePerPlayer
                        ?.toDoubleOrNull()
                        ?.times(100)
                        ?.roundToInt() ?: 0
                ),
            )

        firestore.document("matches/$matchId").update(data).getOrThrow()
    }

    override suspend fun matchesForUser(userId: String): List<Game> =
        firestore
            .collection("matches")
            .query()
            .where("participants", "array-contains", userId)
            .orderBy("startsAtSeconds")
            .get()
            .getOrNull()
            ?.mapNotNull { snapshot -> snapshot.toGame() }
            ?: emptyList()

    private suspend fun queryNearbyMatches(
        center: Coordinates,
        radiusKm: Double,
    ): List<Game> {
        val bounds = boundsForRadius(center, radiusKm)

        val allMatches =
            coroutineScope {
                bounds
                    .map { range ->
                        async {
                            queryRange(range.start, range.endInclusive)
                        }
                    }.awaitAll()
                    .flatten()
            }

        val nearbyMatches =
            allMatches.filter { game ->
                val gameLocation = Coordinates(lat = game.lat, lng = game.lng)
                val distanceKm = distanceKm(center, gameLocation)
                distanceKm <= radiusKm
            }

        return nearbyMatches
            .sortedBy { game ->
                val gameLocation = Coordinates(lat = game.lat, lng = game.lng)
                distanceKm(center, gameLocation)
            }
    }

    private suspend fun queryRange(
        startHash: String,
        endHash: String,
    ): List<Game> =
        firestore
            .collection("matches")
            .query()
            .where("status", "==", "OPEN")
            .where("geohash", ">=", startHash)
            .where("geohash", "<=", endHash)
            .orderBy("geohash")
            .get()
            .getOrNull()
            ?.mapNotNull { snapshot -> snapshot.toGame() }
            ?: emptyList()

    override suspend fun getGameById(gameId: String): Game =
        firestore
            .document("matches/$gameId")
            .get()
            .getOrNull()
            ?.toGame()
            ?: throw IllegalStateException("Match not found: $gameId")

    override fun observeParticipants(matchId: String): Flow<Result<ParticipantsSummary>> =
        firestore
            .collection("matches/$matchId/participants")
            .query()
            .orderBy("joinedAt")
            .snapshots()
            .map { snapshotResult ->
                snapshotResult.map { snapshots ->
                    val participants = snapshots.mapNotNull { it.toParticipant() }
                    val confirmed = participants.filter { it.isConfirmed }
                    val waitlist =
                        participants
                            .filter { !it.isConfirmed }
                            .sortedBy { it.positionInWaitlist ?: Int.MAX_VALUE }

                    val totalSlots =
                        snapshots
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

    override fun observeMatch(matchId: String): Flow<Result<Game>> =
        firestore
            .document("matches/$matchId")
            .snapshots()
            .map { snapshotResult ->
                snapshotResult.mapCatching { snapshot ->
                    snapshot?.toGame()
                        ?: throw IllegalStateException("Match not found: $matchId")
                }
            }
}
