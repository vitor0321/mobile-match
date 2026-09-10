package com.walcker.games.features.ui.shared.manageMatch.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.features.domain.shared.model.Participant
import com.walcker.games.features.domain.shared.model.PlayerRatingSummary
import com.walcker.games.features.domain.shared.model.Rating
import com.walcker.games.strings.ManageMatchStrings
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun RateablePlayersList(
    confirmedPlayers: List<Participant>,
    strings: ManageMatchStrings,
    organizerRatingsGiven: Map<String, Rating>,
    currentUserId: String?,
    participantRatings: Map<String, PlayerRatingSummary>,
    onRatePlayer: (userId: String, displayName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xs),
    ) {
        confirmedPlayers.forEach { participant ->
            val alreadyRated = organizerRatingsGiven.containsKey(participant.userId)
            ParticipantRow(
                participant = participant,
                statusLabel = strings.confirmedTag,
                paidLabel = strings.paidTag,
                rateLabel = if (alreadyRated) strings.editRatingAction else strings.rateAction,
                alreadyRated = alreadyRated,
                canRate = participant.userId != currentUserId,
                ratingSummary = participantRatings[participant.userId],
                ratingsCountLabel = strings.ratingsCount,
                onRatePlayer = onRatePlayer,
            )
        }
    }
}
