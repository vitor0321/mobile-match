package com.walcker.games.features.domain.repository

import com.walcker.games.features.domain.model.CreateMatchRequest
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.MatchRole
import com.walcker.games.features.domain.model.ParticipantsSummary
import kotlinx.coroutines.flow.Flow

internal data class MyMatch(
    val game: Game,
    val role: MatchRole,
)

internal interface GameRepository {
    /**
     * Reactive stream of cached matches.
     *
     * Always emits the current cache contents, then re-emits whenever
     * [refresh] succeeds. UI should observe this rather than calling
     * [openGames] directly so it benefits from the offline-first cache.
     */
    fun observeMatches(): Flow<List<Game>>

    /**
     * Triggers a network refresh of the match list.
     *
     * On success, updates the cache and returns [Result.success].
     * On failure, returns [Result.failure] with a [com.walcker.games.features.domain.error.GamesError].
     */
    suspend fun refresh(): Result<Unit>

    /**
     * @deprecated use [observeMatches] for reactive UI. Kept for callers that
     * want a one-shot read of the current cache.
     */
    suspend fun openGames(): Result<List<Game>>

    suspend fun joinGame(gameId: String): Result<Unit>

    /**
     * Creates a new match in Firestore.
     *
     * Returns the generated match ID on success.
     */
    suspend fun createMatch(request: CreateMatchRequest): Result<String>

    /**
     * Returns matches the user has organized or joined, tagged with [MatchRole].
     *
     * Active = startsAtSeconds is in the future and status is OPEN or FULL.
     * Past = startsAtSeconds is in the past or status is FINISHED/CANCELLED.
     */
    suspend fun getMyMatches(userId: String): Result<List<MyMatch>>

    /**
     * Cancels a match. Stub: returns PermissionDenied until Cloud Function
     * callable is wired up.
     */
    suspend fun cancelMatch(gameId: String): Result<Unit>

    /**
     * Leaves a match. Stub: returns PermissionDenied until Cloud Function
     * callable is wired up.
     */
    suspend fun leaveMatch(gameId: String): Result<Unit>

    /**
     * Fetches a single game/match by ID from Firestore.
     *
     * Used to load match details when navigating from notifications or other deep links.
     */
    suspend fun getGameById(gameId: String): Result<Game>

    /**
     * Observes the participant list of a match in real-time.
     *
     * Emits whenever someone joins, leaves, is promoted from waitlist,
     * or has their payment status updated. The Flow completes when the
     * caller stops collecting (e.g., when leaving the screen).
     */
    fun observeParticipants(matchId: String): Flow<Result<ParticipantsSummary>>

    /**
     * Observes a single match document in real-time.
     *
     * Emits whenever the match metadata changes: status transitions
     * (OPEN → FULL → FINISHED/CANCELLED), confirmedCount, or slot count.
     * The Flow completes when the caller stops collecting.
     */
    fun observeMatch(matchId: String): Flow<Result<Game>>
}
