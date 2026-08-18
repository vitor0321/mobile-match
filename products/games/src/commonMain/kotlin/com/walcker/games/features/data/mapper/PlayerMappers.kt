package com.walcker.games.features.data.mapper

import com.walcker.games.features.data.source.PlayerDetailsDto
import com.walcker.games.features.data.source.PlayerSearchResultDto
import com.walcker.games.features.domain.model.PlayerDetails
import com.walcker.games.features.domain.model.PlayerSearchResult
import com.walcker.games.features.domain.model.Sport

/**
 * Map search result DTO to domain model.
 */
internal fun PlayerSearchResultDto.toDomain(): PlayerSearchResult = PlayerSearchResult(
    userId = userId,
    displayName = displayName,
    photoUrl = photoUrl,
    averageRating = rating,
    totalRatings = ratingCount,
    favoriteSports = sports.mapNotNull { sportName ->
        try {
            Sport.valueOf(sportName)
        } catch (e: IllegalArgumentException) {
            null
        }
    },
    matchesOrganized = matchesOrganized,
    matchesParticipated = matchesParticipated,
)

/**
 * Map details DTO to domain model.
 */
internal fun PlayerDetailsDto.toDomain(): PlayerDetails = PlayerDetails(
    userId = userId,
    displayName = displayName,
    photoUrl = photoUrl,
    email = email,
    bio = bio,
    averageRating = rating,
    totalRatings = ratingCount,
    matchesOrganized = matchesOrganized,
    matchesParticipated = matchesParticipated,
    favoriteSports = sports.mapNotNull { sportName ->
        try {
            Sport.valueOf(sportName)
        } catch (e: IllegalArgumentException) {
            null
        }
    },
    city = city,
    neighborhood = neighborhood,
    locationRadius = radiusKm,
    joinRate = joinRate,
    cancelRate = cancelRate,
    memberSince = memberSinceSeconds,
)
