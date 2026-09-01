package com.walcker.games.features.data.shared.repository

import com.walcker.games.features.data.shared.cache.InMemoryPlayerCache
import com.walcker.games.features.data.shared.mapper.toDomain
import com.walcker.games.features.data.shared.source.PlayerSource
import com.walcker.games.features.domain.shared.error.GamesError
import com.walcker.games.features.domain.shared.model.PlayerDetails
import com.walcker.games.features.domain.shared.model.PlayerSearchFilters
import com.walcker.games.features.domain.shared.model.PlayerSearchResults
import com.walcker.games.features.domain.shared.model.RatingSort
import com.walcker.games.features.domain.shared.model.RatingsPage
import com.walcker.games.features.domain.shared.repository.PlayerRepository

internal class PlayerRepositoryImpl(
    private val source: PlayerSource,
    private val cache: InMemoryPlayerCache,
) : PlayerRepository {
    override suspend fun searchPlayers(
        filters: PlayerSearchFilters,
    ): Result<PlayerSearchResults> {
        cache.searchResults(filters)?.let { return Result.success(it) }

        return source
            .searchPlayers(filters)
            .mapCatching { page ->
                PlayerSearchResults(
                    players = page.players.map { it.toDomain() },
                    reachedLimit = page.reachedLimit,
                )
            }.onSuccess { results -> cache.putSearchResults(filters, results) }
            .recoverCatching { error -> throw mapToGamesError(error) }
    }

    override suspend fun getPlayerDetails(userId: String): Result<PlayerDetails> {
        cache.details(userId)?.let { return Result.success(it) }

        return source
            .getPlayerDetails(userId)
            .mapCatching { it.toDomain() }
            .onSuccess { player -> cache.putDetails(userId, player) }
            .recoverCatching { error -> throw mapToGamesError(error) }
    }

    override suspend fun getPlayerRatings(
        userId: String,
        limit: Int,
        sort: RatingSort,
        cursor: String?,
    ): Result<RatingsPage> =
        source
            .getPlayerRatings(userId, limit, sort, cursor)
            .recoverCatching { error -> throw mapToGamesError(error) }

    private fun mapToGamesError(error: Throwable): GamesError =
        when {
            error is GamesError -> error
            error is NoSuchElementException ->
                GamesError.NotFound("Jogador")
            error.message?.contains("permission denied", ignoreCase = true) == true ->
                GamesError.PermissionDenied(error.message ?: "Sem permissão para acessar")
            else ->
                GamesError.Unknown(error.message ?: "Erro ao buscar dados do jogador")
        }
}
