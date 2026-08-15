package com.walcker.games.features.data.mapper

import com.walcker.games.features.domain.model.Game
import com.walcker.games.features.domain.model.MatchStatus
import com.walcker.games.features.domain.model.Sport
import com.walcker.match.firestore.DocumentSnapshot

/**
 * Mapping utilities for converting Firestore documents into the
 * domain [Game] model.
 *
 * Firestore stores match data denormalized on the `matches/{matchId}`
 * collection. The schema is documented in the migration plan.
 */

/**
 * Converts a Firestore document snapshot into a [Game], returning
 * `null` if any required field is missing or malformed.
 *
 * Required fields: id, sport, venue, neighborhood, city, address,
 * lat, lng, geohash, startsAt, organizerName, organizerId.
 *
 * Optional fields with defaults: durationMin (60), confirmedCount (0),
 * totalSlots (1), priceCents (null), organizerRating (5.0), status (OPEN).
 */
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
            pricePerPlayer = getString("priceCents")?.let { price ->
                // TODO: Use formatBRLCents from core/payments
                "R$ $price" // Placeholder
            },
            organizerName = getString("organizerName") ?: return null,
            organizerId = getString("organizerId") ?: return null,
            organizerRating = getDouble("organizerRating") ?: 5.0,
            status = getString("status")
                ?.let { runCatching { MatchStatus.valueOf(it.uppercase()) }.getOrNull() }
                ?: MatchStatus.OPEN,
            participants = (getList("participants") as? List<*>)
                ?.filterIsInstance<String>()
                ?: emptyList(),
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * Reads the Firestore `startsAt` field as seconds since epoch.
 *
 * Firestore's KMP SDK can return a Timestamp as either a Long (seconds)
 * or as a map (`{seconds, nanoseconds}`) depending on the platform.
 * This handles both shapes.
 */
private fun DocumentSnapshot.readStartsAtSeconds(): Long? {
    getLong("startsAtSeconds")?.let { return it }
    getLong("startsAt")?.let { return it }

    val tsMap = getMap("startsAt")
    (tsMap?.get("seconds") as? Number)?.toLong()?.let { return it }

    return null
}
