package com.walcker.games.features.data.shared.platform

import com.walcker.games.features.data.shared.source.FirestorePlayerSource
import com.walcker.games.features.data.shared.source.PlayerSource
import com.walcker.games.features.data.shared.source.RatingSource
import com.walcker.match.firestore.FirestoreClient

internal fun createPlayerSource(
    firestore: FirestoreClient,
    ratingSource: RatingSource,
): PlayerSource = FirestorePlayerSource(firestore, ratingSource)
