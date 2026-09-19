package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.model.RatingSort
import com.walcker.games.features.domain.shared.model.RatingsPage
import com.walcker.games.features.domain.shared.repository.PlayerRepository

internal interface GetPlayerRatingsUseCase {
    suspend operator fun invoke(
        userId: String,
        limit: Int = DEFAULT_PAGE_SIZE,
        sort: RatingSort = RatingSort.RECENT,
        cursor: String? = null,
    ): Result<RatingsPage>

    companion object {
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
    ): Result<RatingsPage> =
        repository.getPlayerRatings(
            userId = userId,
            limit = limit,
            sort = sort,
            cursor = cursor,
        )
}
