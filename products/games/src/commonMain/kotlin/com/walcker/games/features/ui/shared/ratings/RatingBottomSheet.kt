package com.walcker.games.features.ui.shared.ratings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.features.domain.shared.model.ReportReason
import com.walcker.games.features.ui.shared.ratings.component.RatingForm
import com.walcker.games.strings.RatingStrings
import com.walcker.games.strings.ReportStrings
import com.walcker.match.cedar.components.CedarFloatingDialog

@Composable
internal fun RatingBottomSheet(
    isVisible: Boolean,
    playerName: String,
    strings: RatingStrings,
    reportStrings: ReportStrings,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String, reportReason: ReportReason?, reportDetails: String) -> Unit,
    initialRating: Int = 5,
    initialComment: String = "",
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (isVisible) {
        CedarFloatingDialog(onDismiss = onDismiss) {
            RatingForm(
                playerName = playerName,
                strings = strings,
                reportStrings = reportStrings,
                onSubmit = onSubmit,
                initialRating = initialRating,
                initialComment = initialComment,
                isLoading = isLoading,
                modifier = modifier,
            )
        }
    }
}
