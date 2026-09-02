@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.walcker.games.screenshot

import com.walcker.games.features.data.shared.model.NotificationHistoryItem
import com.walcker.games.features.domain.shared.model.DimensionAverage
import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.MatchRole
import com.walcker.games.features.domain.shared.model.MatchStatus
import com.walcker.games.features.domain.shared.model.Participant
import com.walcker.games.features.domain.shared.model.ParticipantsSummary
import com.walcker.games.features.domain.shared.model.PlayerDetails
import com.walcker.games.features.domain.shared.model.PlayerSearchResult
import com.walcker.games.features.domain.shared.model.Rating
import com.walcker.games.features.domain.shared.model.RatingDimension
import com.walcker.games.features.domain.shared.model.RatingDistribution
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.games.features.domain.shared.repository.MyMatch

internal fun fakeGame(
    id: String = "match-1",
    sport: Sport = Sport.FUTSAL,
    venueName: String = "Quadra Central",
    neighborhood: String = "Centro",
    city: String = "São Paulo",
    address: String = "Rua Um, 100",
    startsAtSeconds: Long = 4_102_444_800L,
    durationMin: Int = 60,
    confirmedPlayers: Int = 6,
    totalPlayers: Int = 10,
    pricePerPlayer: String? = "R$ 15,00",
    organizerName: String = "Ana Souza",
    organizerId: String = "organizer-1",
    organizerRating: Double = 4.5,
    status: MatchStatus = MatchStatus.OPEN,
    participants: List<String> = emptyList(),
): Game =
    Game(
        id = id,
        sport = sport,
        venueName = venueName,
        neighborhood = neighborhood,
        city = city,
        address = address,
        lat = -23.55,
        lng = -46.63,
        geohash = "6gyf4",
        startsAtSeconds = startsAtSeconds,
        durationMin = durationMin,
        confirmedPlayers = confirmedPlayers,
        totalPlayers = totalPlayers,
        pricePerPlayer = pricePerPlayer,
        organizerName = organizerName,
        organizerId = organizerId,
        organizerRating = organizerRating,
        status = status,
        participants = participants,
    )

internal fun fakeRating(
    id: String = "rating-1",
    matchId: String = "match-1",
    ratedUserId: String = "player-1",
    raterUserId: String = "player-2",
    stars: Int = 5,
    comment: String = "Jogou muito bem, pontual e educado.",
    createdAtMs: Long = 1_760_000_000_000L,
): Rating =
    Rating(
        id = id,
        matchId = matchId,
        ratedUserId = ratedUserId,
        raterUserId = raterUserId,
        rating = stars,
        comment = comment,
        createdAtMs = createdAtMs,
    )

internal fun fakeMyMatch(
    game: Game = fakeGame(),
    role: MatchRole = MatchRole.PARTICIPANT,
): MyMatch = MyMatch(game = game, role = role)

internal fun fakeParticipant(
    userId: String = "player-1",
    displayName: String = "Bruno Lima",
    isConfirmed: Boolean = true,
    positionInWaitlist: Int? = null,
    hasPaid: Boolean = false,
): Participant =
    Participant(
        userId = userId,
        displayName = displayName,
        photoUrl = null,
        joinedAt = 1_760_000_000_000L,
        isConfirmed = isConfirmed,
        positionInWaitlist = positionInWaitlist,
        hasPaid = hasPaid,
    )

internal fun fakeParticipantsSummary(
    confirmed: List<Participant> = listOf(fakeParticipant()),
    waitlist: List<Participant> = emptyList(),
    confirmedCount: Int = confirmed.size,
    totalSlots: Int = 10,
): ParticipantsSummary =
    ParticipantsSummary(
        confirmed = confirmed,
        waitlist = waitlist,
        confirmedCount = confirmedCount,
        totalSlots = totalSlots,
    )

internal fun fakePlayerSearchResult(
    userId: String = "player-1",
    displayName: String = "Bruno Lima",
    averageRating: Float = 4.6f,
    totalRatings: Int = 12,
    favoriteSports: List<Sport> = listOf(Sport.FUTSAL, Sport.FUTEBOL),
): PlayerSearchResult =
    PlayerSearchResult(
        userId = userId,
        displayName = displayName,
        photoUrl = null,
        averageRating = averageRating,
        totalRatings = totalRatings,
        favoriteSports = favoriteSports,
    )

internal fun fakePlayerDetails(
    userId: String = "player-1",
    displayName: String = "Bruno Lima",
    averageRating: Float = 4.6f,
    totalRatings: Int = 12,
    favoriteSports: List<Sport> = listOf(Sport.FUTSAL, Sport.FUTEBOL),
    city: String? = "São Paulo",
    neighborhood: String? = "Centro",
    dimensionAverages: Map<RatingDimension, DimensionAverage> = emptyMap(),
): PlayerDetails =
    PlayerDetails(
        userId = userId,
        displayName = displayName,
        photoUrl = null,
        averageRating = averageRating,
        totalRatings = totalRatings,
        favoriteSports = favoriteSports,
        city = city,
        neighborhood = neighborhood,
        memberSinceMs = 1_700_000_000_000L,
        dimensionAverages = dimensionAverages,
    )

internal fun fakeRatingDistribution(counts: List<Int> = listOf(1, 1, 2, 3, 5)): RatingDistribution = RatingDistribution(counts = counts)

internal fun fakeNotification(
    id: String = "notification-1",
    title: String = "Nova vaga perto de você",
    body: String = "Abriu uma vaga na Quadra Central às 19h.",
    receivedAt: Long = 1_760_000_000_000L,
    isRead: Boolean = false,
): NotificationHistoryItem =
    NotificationHistoryItem(
        id = id,
        title = title,
        body = body,
        receivedAt = receivedAt,
        isRead = isRead,
    )
