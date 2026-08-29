package com.walcker.games.features.domain.usecase

import com.walcker.games.fake.FakeGameRepository
import com.walcker.games.fake.game
import com.walcker.games.features.domain.model.MatchRole
import com.walcker.games.features.domain.model.MatchStatus
import com.walcker.games.features.domain.repository.MyMatch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetMyMatchesUseCaseTest {

    private val start = 1_000_000L
    private val end = start + 60 * 60
    private val repository = FakeGameRepository()
    private val useCase = GetMyMatchesUseCaseImpl(repository)

    private fun myMatch(
        id: String,
        startsAtSeconds: Long = start,
        durationMin: Int = 60,
        status: MatchStatus = MatchStatus.OPEN,
    ) = MyMatch(
        game = game(
            id = id,
            startsAtSeconds = startsAtSeconds,
            durationMin = durationMin,
            status = status,
        ),
        role = MatchRole.PARTICIPANT,
    )

    @Test
    fun `partida em andamento fica em ativas`() = runTest {
        repository.myMatches = Result.success(listOf(myMatch("em-andamento")))

        val result = useCase(userId = "u1", nowSeconds = end - 1).getOrThrow()

        assertEquals(listOf("em-andamento"), result.active.map { it.game.id })
        assertTrue(result.past.isEmpty())
    }

    @Test
    fun `partida vira passada no instante em que acaba`() = runTest {
        repository.myMatches = Result.success(listOf(myMatch("acabou")))

        val result = useCase(userId = "u1", nowSeconds = end).getOrThrow()

        assertTrue(result.active.isEmpty())
        assertEquals(listOf("acabou"), result.past.map { it.game.id })
    }

    @Test
    fun `cancelada e passada mesmo antes do horario`() = runTest {
        repository.myMatches = Result.success(
            listOf(myMatch("cancelada", status = MatchStatus.CANCELLED)),
        )

        val result = useCase(userId = "u1", nowSeconds = start - 5_000).getOrThrow()

        assertTrue(result.active.isEmpty())
        assertEquals(listOf("cancelada"), result.past.map { it.game.id })
    }

    @Test
    fun `separa as duas listas preservando a ordem de entrada`() = runTest {
        repository.myMatches = Result.success(
            listOf(
                myMatch("passada-1", startsAtSeconds = start - 10_000),
                myMatch("futura-1", startsAtSeconds = start + 10_000),
                myMatch("passada-2", startsAtSeconds = start - 20_000),
                myMatch("futura-2", startsAtSeconds = start + 20_000),
            ),
        )

        val result = useCase(userId = "u1", nowSeconds = start).getOrThrow()

        assertEquals(listOf("futura-1", "futura-2"), result.active.map { it.game.id })
        assertEquals(listOf("passada-1", "passada-2"), result.past.map { it.game.id })
    }

    @Test
    fun `falha do repositorio sobe sem virar lista vazia`() = runTest {
        repository.myMatches = Result.failure(IllegalStateException("offline"))

        val result = useCase(userId = "u1", nowSeconds = start)

        assertTrue(result.isFailure)
    }
}
