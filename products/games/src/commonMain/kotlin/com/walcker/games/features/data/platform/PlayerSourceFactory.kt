package com.walcker.games.features.data.platform

import com.walcker.games.features.data.source.FirestorePlayerSource
import com.walcker.games.features.data.source.PlayerSource
import com.walcker.games.features.data.source.RatingSource
import com.walcker.match.firestore.FirestoreClient

internal fun createPlayerSource(
    firestore: FirestoreClient,
    ratingSource: RatingSource,
): PlayerSource = FirestorePlayerSource(firestore, ratingSource)
