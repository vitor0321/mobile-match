package com.walcker.games.features.data.platform

import com.walcker.games.features.data.source.FirestorePlayerSource
import com.walcker.games.features.data.source.PlayerSource
import com.walcker.games.features.data.source.RatingSource
import com.walcker.match.firestore.FirestoreClient

/**
 * Factory for creating platform-specific PlayerSource implementations.
 *
 * Android and iOS both use Firestore, so no expect/actual needed for now.
 * This factory exists for future extensibility.
 */
internal fun createPlayerSource(
    firestore: FirestoreClient,
    ratingSource: RatingSource,
): PlayerSource = FirestorePlayerSource(firestore, ratingSource)
