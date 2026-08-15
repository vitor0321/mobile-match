package com.walcker.games.features.domain.usecase

import com.walcker.games.features.domain.repository.GameRepository

/**
 * Cancels a match the user organizes. No-op stub for now.
 * Real implementation will require a Cloud Function callable that
 * enforces auth + status transitions server-side.
 */
internal interface CancelMatchUseCase {
    suspend operator fun invoke(gameId: String): Result<Unit>
}

internal class CancelMatchUseCaseImpl(
    private val repository: GameRepository,
) : CancelMatchUseCase {
    override suspend operator fun invoke(gameId: String): Result<Unit> =
        repository.cancelMatch(gameId)
}

/**
 * Leaves a match the user joined (not organized). No-op stub for now.
 */
internal interface LeaveMatchUseCase {
    suspend operator fun invoke(gameId: String): Result<Unit>
}

internal class LeaveMatchUseCaseImpl(
    private val repository: GameRepository,
) : LeaveMatchUseCase {
    override suspend operator fun invoke(gameId: String): Result<Unit> =
        repository.leaveMatch(gameId)
}
