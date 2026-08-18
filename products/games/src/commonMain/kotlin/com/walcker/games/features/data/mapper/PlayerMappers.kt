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
    displayName = fullName,
    photoUrl = avatarUrl,
    averageRating = rating,
    totalRatings = ratingCount,
    favoriteSports = sports.toSports(),
)

/**
 * Map details DTO to domain model.
 */
internal fun PlayerDetailsDto.toDomain(): PlayerDetails = PlayerDetails(
    userId = userId,
    displayName = fullName,
    photoUrl = avatarUrl,
    averageRating = rating,
    totalRatings = ratingCount,
    favoriteSports = sports.toSports(),
    city = city,
    neighborhood = neighborhood,
    memberSinceMs = createdAtMs,
)

/**
 * An unknown sport name is dropped rather than failing the whole profile: the
 * enum can lag behind what an older client wrote.
 */
private fun List<String>.toSports(): List<Sport> = mapNotNull { name ->
    Sport.entries.firstOrNull { it.name == name }
}
