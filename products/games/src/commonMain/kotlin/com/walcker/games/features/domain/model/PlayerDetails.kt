package com.walcker.games.features.domain.model

/**
 * Detailed player profile, fetched when the user opens someone from search.
 *
 * Mirrors the real shape of `profiles/{uid}` — see `onUserCreate` in
 * `functions/src/index.ts` and `profileEditableFields()` in `firestore.rules`.
 * Experience stats (matches organized/played, join and cancel rates) are not
 * here because no writer produces them yet; they come back in Phase 6 together
 * with the trigger that maintains them.
 *
 * @param memberSinceMs epoch millis of profile creation; `0` when unknown
 */
internal data class PlayerDetails(
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val averageRating: Float,
    val totalRatings: Int,
    val favoriteSports: List<Sport>,
    val city: String?,
    val neighborhood: String?,
    val memberSinceMs: Long,
)
