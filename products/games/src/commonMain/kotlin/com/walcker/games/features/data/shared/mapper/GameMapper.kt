package com.walcker.games.features.data.shared.mapper

import com.walcker.games.features.domain.shared.model.Game
import com.walcker.games.features.domain.shared.model.MatchStatus
import com.walcker.games.features.domain.shared.model.RecurrenceOption
import com.walcker.games.features.domain.shared.model.Sport
import com.walcker.match.core.payments.formatCurrencyCents
import com.walcker.match.firestore.DocumentSnapshot

internal fun DocumentSnapshot.toGame(): Game? {
    return try {
        val startsAtSeconds = readStartsAtSeconds() ?: return null

        val sportName = getString("sport") ?: return null
        val sport = runCatching { Sport.valueOf(sportName) }.getOrNull() ?: return null

        Game(
            id = id,
            sport = sport,
            venueName = getString("venueName") ?: return null,
            neighborhood = getString("neighborhood") ?: return null,
            city = getString("city") ?: return null,
            address = getString("address") ?: return null,
            lat = getDouble("lat") ?: return null,
            lng = getDouble("lng") ?: return null,
            geohash = getString("geohash") ?: return null,
            startsAtSeconds = startsAtSeconds,
            durationMin = (getLong("durationMin")?.toInt()) ?: 60,
            confirmedPlayers = (getLong("confirmedCount")?.toInt()) ?: 0,
            totalPlayers = (getLong("totalSlots")?.toInt()) ?: 1,
            pricePerPlayer =
                (getLong("priceCents")?.toInt() ?: 0)
                    .takeIf { it > 0 }
                    ?.let { formatCurrencyCents(it, getString("currencyCode") ?: "BRL") },
            priceCents = getLong("priceCents")?.toInt() ?: 0,
            currencyCode = getString("currencyCode") ?: "BRL",
            organizerName = getString("organizerName") ?: return null,
            organizerId = getString("organizerId") ?: return null,
            organizerRating = getDouble("organizerRating") ?: 0.0,
            organizerRatingCount = getLong("organizerRatingCount")?.toInt() ?: 0,
            matchRating = getDouble("matchRating") ?: 0.0,
            matchRatingCount = getLong("matchRatingCount")?.toInt() ?: 0,
            status =
                getString("status")
                    ?.let { runCatching { MatchStatus.valueOf(it.uppercase()) }.getOrNull() }
                    ?: MatchStatus.OPEN,
            participants =
                (getList("participants") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList(),
            recurrence =
                getString("recurrence")
                    ?.let { runCatching { RecurrenceOption.valueOf(it) }.getOrNull() }
                    ?: RecurrenceOption.NONE,
            seriesId = getString("seriesId"),
        )
    } catch (e: Exception) {
        null
    }
}

private fun DocumentSnapshot.readStartsAtSeconds(): Long? {
    getLong("startsAtSeconds")?.let { return it }
    getLong("startsAt")?.let { return it }

    val tsMap = getMap("startsAt")
    (tsMap?.get("seconds") as? Number)?.toLong()?.let { return it }

    return null
}
