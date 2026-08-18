package com.walcker.games.features.data.repository

import com.walcker.games.features.data.mapper.toDomain
import com.walcker.games.features.data.source.PlayerSource
import com.walcker.games.features.domain.error.GamesError
import com.walcker.games.features.domain.model.PlayerDetails
import com.walcker.games.features.domain.model.PlayerSearchFilters
import com.walcker.games.features.domain.model.PlayerSearchResult
import com.walcker.games.features.domain.model.Rating
import com.walcker.games.features.domain.repository.PlayerRepository

/**
 * Implementation of [PlayerRepository].
 *
 * Handles data layer logic: calling [PlayerSource] for Firestore queries,
 * mapping DTOs to domain models, and error handling.
 */
internal class PlayerRepositoryImpl(
    private val source: PlayerSource,
) : PlayerRepository {

    override suspend fun searchPlayers(filters: PlayerSearchFilters): Result<List<PlayerSearchResult>> =
        source.searchPlayers(filters)
            .mapCatching { dtos -> dtos.map { it.toDomain() } }
            .recoverCatching { error ->
                throw mapToGamesError(error)
            }

    override suspend fun getPlayerDetails(userId: String): Result<PlayerDetails> =
        source.getPlayerDetails(userId)
            .mapCatching { it.toDomain() }
            .recoverCatching { error ->
                throw mapToGamesError(error)
            }

    override suspend fun getPlayerRatings(
        userId: String,
        limit: Int,
        cursor: String?,
    ): Result<List<Rating>> =
        source.getPlayerRatings(userId, limit, cursor)
            .recoverCatching { error ->
                throw mapToGamesError(error)
            }

    /**
     * Convert exceptions from the source layer to domain [GamesError].
     */
    private fun mapToGamesError(error: Throwable): GamesError = when {
        error is GamesError -> error
        error.message?.contains("not found", ignoreCase = true) == true ->
            GamesError.NotFound(error.message ?: "Jogador não encontrado")
        error.message?.contains("permission denied", ignoreCase = true) == true ->
            GamesError.PermissionDenied(error.message ?: "Sem permissão para acessar")
        else ->
            GamesError.Unknown(error.message ?: "Erro ao buscar dados do jogador")
    }
}
