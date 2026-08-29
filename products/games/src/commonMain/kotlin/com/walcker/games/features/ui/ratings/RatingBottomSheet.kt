package com.walcker.games.features.ui.ratings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.walcker.games.features.domain.model.RatingDimensions
import com.walcker.games.strings.RatingStrings
import com.walcker.match.cedar.tokens.CedarTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RatingBottomSheet(
    isVisible: Boolean,
    playerName: String,
    strings: RatingStrings,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String, dimensions: RatingDimensions) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (isVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            shape = CedarTokens.radius.sheet,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            RatingForm(
                playerName = playerName,
                strings = strings,
                onSubmit = onSubmit,
                isLoading = isLoading,
                modifier = modifier,
            )
        }
    }
}
