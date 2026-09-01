package com.walcker.games.features.data.shared.mapper

import com.walcker.games.fake.playerDetailsDto
import com.walcker.games.fake.playerSearchResultDto
import com.walcker.games.features.domain.shared.model.Sport
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerMappersTest {
    @Test
    fun `maps a search result dto into the domain model`() {
        val dto =
            playerSearchResultDto(userId = "player-9", fullName = "Bruno Lima", rating = 3.8f)
                .copy(avatarUrl = "https://example.com/bruno.jpg", sports = listOf("FUTSAL", "FUTEBOL"))

        val result = dto.toDomain()

        assertEquals("player-9", result.userId)
        assertEquals("Bruno Lima", result.displayName)
        assertEquals("https://example.com/bruno.jpg", result.photoUrl)
        assertEquals(3.8f, result.averageRating)
        assertEquals(dto.ratingCount, result.totalRatings)
        assertEquals(listOf(Sport.FUTSAL, Sport.FUTEBOL), result.favoriteSports)
    }

    @Test
    fun `an unrecognized sport name is dropped, not crashed on`() {
        val dto = playerSearchResultDto().copy(sports = listOf("FUTSAL", "QUIDDITCH"))

        assertEquals(listOf(Sport.FUTSAL), dto.toDomain().favoriteSports)
    }

    @Test
    fun `maps a details dto into the domain model`() {
        val dto = playerDetailsDto(userId = "player-9", fullName = "Bruno Lima")

        val details = dto.toDomain()

        assertEquals("player-9", details.userId)
        assertEquals("Bruno Lima", details.displayName)
        assertEquals(dto.city, details.city)
        assertEquals(dto.neighborhood, details.neighborhood)
        assertEquals(dto.createdAtMs, details.memberSinceMs)
        assertEquals(dto.dimensionAverages, details.dimensionAverages)
    }
}
