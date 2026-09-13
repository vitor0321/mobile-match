package com.walcker.games.features.ui.shared.manageMatch.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.walcker.games.strings.ManageMatchStrings
import com.walcker.match.cedar.tokens.CedarTokens

internal val SkillRatingRange = 1..10

@Composable
internal fun SkillRatingDialog(
    myRating: Int?,
    isSaving: Boolean,
    strings: ManageMatchStrings,
    onRateSkill: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.skillRatingMenuTitle) },
        text = {
            ChipRangeSelector(
                selected = myRating ?: -1,
                range = SkillRatingRange,
                onSelected = { value ->
                    onRateSkill(value)
                    onDismiss()
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text(strings.skillRatingDialogClose) }
        },
        shape = CedarTokens.radius.lgShape,
    )
}
