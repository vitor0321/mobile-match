package com.walcker.games.features.data.mapper

import com.walcker.games.features.data.source.PlayerDetailsDto
import com.walcker.games.features.data.source.PlayerSearchResultDto
import com.walcker.games.features.domain.model.PlayerDetails
import com.walcker.games.features.domain.model.PlayerSearchResult
import com.walcker.games.features.domain.model.Sport

internal fun PlayerSearchResultDto.toDomain(): PlayerSearchResult = PlayerSearchResult(
    userId = userId,
    displayName = fullName,
    photoUrl = avatarUrl,
    averageRating = rating,
    totalRatings = ratingCount,
    favoriteSports = sports.toSports(),
)

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
    dimensionAverages = dimensionAverages,
)

private fun List<String>.toSports(): List<Sport> = mapNotNull { name ->
    Sport.entries.firstOrNull { it.name == name }
}
