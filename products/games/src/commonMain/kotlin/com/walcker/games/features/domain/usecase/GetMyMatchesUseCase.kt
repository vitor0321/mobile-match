package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.MatchRole
import com.walcker.games.features.domain.model.MatchStatus
import com.walcker.games.features.domain.repository.GameRepository
import com.walcker.games.features.domain.repository.MyMatch

/**
 * Returns the user's matches split into "active" (upcoming or in-progress) and
 * "past" (finished/cancelled or in the past).
 */
internal data class MyMatches(
    val active: List<MyMatch>,
    val past: List<MyMatch>,
)

internal interface GetMyMatchesUseCase {
    suspend operator fun invoke(userId: String, nowSeconds: Long): Result<MyMatches>
}

internal class GetMyMatchesUseCaseImpl(
    private val repository: GameRepository,
) : GetMyMatchesUseCase {
    override suspend operator fun invoke(userId: String, nowSeconds: Long): Result<MyMatches> {
        return repository.getMyMatches(userId).map { matches ->
            val (active, past) = matches.partition { isActive(it.game, nowSeconds) }
            MyMatches(active = active, past = past)
        }
    }

    private fun isActive(game: Game, nowSeconds: Long): Boolean {
        if (game.status == MatchStatus.CANCELLED || game.status == MatchStatus.FINISHED) return false
        val endSeconds = game.startsAtSeconds + game.durationMin.toLong() * 60
        return endSeconds >= nowSeconds
    }
}
