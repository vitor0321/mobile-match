package com.walcker.games.features.data.source

import com.walcker.games.features.domain.model.CreateMatchRequest
import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.Participant
import com.walcker.games.features.domain.model.ParticipantsSummary
import com.walcker.games.features.domain.model.Sport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.random.Random

/**
 * Fonte temporária, em memória, para o app rodar antes do backend existir.
 * Substituir por Firestore/API quando o match sair do papel.
 *
 * Sample data with São Paulo locations (near Av. Paulista area).
 */
internal class InMemoryGameSource : GameSource {

    private val games = MutableStateFlow(SAMPLE_GAMES)

    override suspend fun openGames(): List<Game> = games.value

    override suspend fun joinGame(gameId: String) {
        games.update { current ->
            current.map { game ->
                if (game.id == gameId && game.hasOpenSlots) {
                    game.copy(confirmedPlayers = game.confirmedPlayers + 1)
                } else {
                    game
                }
            }
        }
    }

    override suspend fun createMatch(request: CreateMatchRequest): String {
        val matchId = "match_${Random.nextInt(10000)}"
        val organizerId = "user_anon"
        val newGame = Game(
            id = matchId,
            sport = request.sport,
            venueName = request.venueName,
            neighborhood = request.neighborhood,
            city = request.city,
            address = request.address,
            lat = request.lat,
            lng = request.lng,
            geohash = request.geohash,
            startsAtSeconds = request.startsAtSeconds,
            durationMin = request.durationMin,
            confirmedPlayers = 1, // Organizer
            totalPlayers = request.totalPlayers,
            pricePerPlayer = request.pricePerPlayer,
            organizerName = "Anonymous",
            organizerId = organizerId,
            organizerRating = 4.0,
            participants = listOf(organizerId),
        )
        games.update { it + newGame }
        return matchId
    }

    override suspend fun matchesForUser(userId: String): List<Game> {
        return games.value.filter { game ->
            game.organizerId == userId || userId in game.participants
        }
    }

    override suspend fun getGameById(gameId: String): Game {
        return games.value.firstOrNull { it.id == gameId }
            ?: throw IllegalStateException("Game with id '$gameId' not found")
    }

    override fun observeParticipants(matchId: String): Flow<Result<ParticipantsSummary>> {
        // Derive a fake participant list from the Game data we already have
        val game = games.value.firstOrNull { it.id == matchId }
        return if (game == null) {
            MutableStateFlow(Result.success(ParticipantsSummary(emptyList(), emptyList(), 0, 0)))
        } else {
            val confirmed = game.participants.mapIndexed { index, userId ->
                Participant(
                    userId = userId,
                    displayName = if (userId == game.organizerId) game.organizerName else "Jogador ${index + 1}",
                    photoUrl = null,
                    joinedAt = 1_726_000_000L + index.toLong() * 60L,
                    isConfirmed = true,
                    positionInWaitlist = null,
                    hasPaid = index < game.confirmedPlayers,
                )
            }
            MutableStateFlow(
                Result.success(
                    ParticipantsSummary(
                        confirmed = confirmed,
                        waitlist = emptyList(),
                        confirmedCount = confirmed.size,
                        totalSlots = game.totalPlayers,
                    )
                )
            )
        }
    }

    private companion object {
        // Current time: ~Aug 14 2026 15:00 UTC, so +3 hours = ~18:00 São Paulo time
        // Use Unix timestamp for "today at 20:00" (1723663200 = ~Aug 15 2026 01:00 UTC = Aug 15 04:00 SP)
        val TODAY_AT_20H = 1723713600L // Approximate
        val TOMORROW_AT_19H30 = 1723797000L
        val THURSDAY_AT_21H = 1723881600L

        val SAMPLE_GAMES = listOf(
            Game(
                id = "1",
                sport = Sport.FUTSAL,
                venueName = "Green Ball",
                neighborhood = "União dos Cegos",
                city = "São Paulo",
                address = "Rua A, 123",
                lat = -23.55,
                lng = -46.63,
                geohash = "6gyf4bf8m",
                startsAtSeconds = TODAY_AT_20H,
                durationMin = 60,
                confirmedPlayers = 12,
                totalPlayers = 14,
                pricePerPlayer = "R$ 20",
                organizerName = "Vitor",
                organizerId = "user_vitor",
                organizerRating = 4.8,
            ),
            Game(
                id = "2",
                sport = Sport.SOCIETY,
                venueName = "Arena Central",
                neighborhood = "Bela Vista",
                city = "São Paulo",
                address = "Av. Paulista, 456",
                lat = -23.561,
                lng = -46.654,
                geohash = "6gyf4bhp5",
                startsAtSeconds = TOMORROW_AT_19H30,
                durationMin = 90,
                confirmedPlayers = 10,
                totalPlayers = 12,
                pricePerPlayer = "R$ 25",
                organizerName = "Rafael",
                organizerId = "user_rafael",
                organizerRating = 5.0,
            ),
            Game(
                id = "3",
                sport = Sport.VOLEI,
                venueName = "Quadra do Parque",
                neighborhood = "Consolação",
                city = "São Paulo",
                address = "Parque Trianon, 789",
                lat = -23.5609,
                lng = -46.6614,
                geohash = "6gyf4bgpx",
                startsAtSeconds = THURSDAY_AT_21H,
                durationMin = 120,
                confirmedPlayers = 10,
                totalPlayers = 12,
                pricePerPlayer = null,
                organizerName = "Camila",
                organizerId = "user_camila",
                organizerRating = 4.9,
            ),
        )
    }
}
