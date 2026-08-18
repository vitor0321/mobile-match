package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.RatingSort
import com.walcker.games.features.domain.model.RatingsPage
import com.walcker.games.features.domain.repository.PlayerRepository

/**
 * Fetches one page of the ratings a player received.
 *
 * Used by both the player details preview (first page only) and the full,
 * paginated ratings list.
 */
internal interface GetPlayerRatingsUseCase {
    suspend operator fun invoke(
        userId: String,
        limit: Int = DEFAULT_PAGE_SIZE,
        sort: RatingSort = RatingSort.RECENT,
        cursor: String? = null,
    ): Result<RatingsPage>

    companion object {
        /** Page size agreed for the ratings list (Phase 5 acceptance criteria). */
        const val DEFAULT_PAGE_SIZE: Int = 20
    }
}

internal class GetPlayerRatingsUseCaseImpl(
    private val repository: PlayerRepository,
) : GetPlayerRatingsUseCase {
    override suspend fun invoke(
        userId: String,
        limit: Int,
        sort: RatingSort,
        cursor: String?,
    ): Result<RatingsPage> = repository.getPlayerRatings(
        userId = userId,
        limit = limit,
        sort = sort,
        cursor = cursor,
    )
}
