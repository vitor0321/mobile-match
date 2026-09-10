package com.walcker.games.features.domain.shared.usecase

import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.MatchStatus
import com.walcker.games.features.domain.shared.repository.GameRepository
import com.walcker.games.features.domain.shared.repository.MyMatch

internal data class MyMatches(
    val active: List<MyMatch>,
    val past: List<MyMatch>,
)

internal interface GetMyMatchesUseCase {
    suspend operator fun invoke(
        userId: String,
        nowSeconds: Long,
    ): Result<MyMatches>
}

internal class GetMyMatchesUseCaseImpl(
    private val repository: GameRepository,
) : GetMyMatchesUseCase {
    override suspend operator fun invoke(
        userId: String,
        nowSeconds: Long,
    ): Result<MyMatches> =
        repository.getMyMatches(userId).map { matches ->
            val (active, past) = matches.partition { isActive(it.game, nowSeconds) }
            MyMatches(active = active, past = past)
        }

    private fun isActive(
        game: Game,
        nowSeconds: Long,
    ): Boolean {
        if (game.status == MatchStatus.CANCELLED || game.status == MatchStatus.FINISHED) return false
        return !game.isOver(nowSeconds)
    }
}
