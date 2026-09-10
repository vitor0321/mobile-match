package com.walcker.games.features.ui.shared.matchDetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.walcker.games.strings.MatchDetailStrings
import com.walcker.match.cedar.components.CedarFloatingDialog
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.CedarStarPicker
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun MatchRatingBottomSheet(
    isVisible: Boolean,
    strings: MatchDetailStrings,
    starContentDescription: (Int) -> String,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (isVisible) {
        var rating by remember { mutableStateOf(5) }

        CedarFloatingDialog(onDismiss = onDismiss) {
            Column(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = CedarTokens.spacing.lg,
                            vertical = CedarTokens.spacing.md,
                        ),
                verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
            ) {
                CedarSectionHeader(title = strings.rateMatchSheetTitle)
                CedarStarPicker(
                    rating = rating,
                    onRatingChange = { rating = it },
                    starContentDescription = starContentDescription,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                CedarPrimaryButton(
                    text = strings.rateMatchAction,
                    onClick = { onSubmit(rating) },
                    loading = isLoading,
                )
            }
        }
    }
}
