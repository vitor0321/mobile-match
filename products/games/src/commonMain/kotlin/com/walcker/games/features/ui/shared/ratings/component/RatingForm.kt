package com.walcker.games.features.ui.shared.ratings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.walcker.games.features.domain.shared.model.ReportReason
import com.walcker.games.strings.RatingStrings
import com.walcker.games.strings.ReportStrings
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.CedarStarPicker
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.tokens.CedarTokens

private const val MAX_COMMENT_LENGTH = 500

@Composable
internal fun RatingForm(
    playerName: String,
    strings: RatingStrings,
    reportStrings: ReportStrings,
    onSubmit: (rating: Int, comment: String, reportReason: ReportReason?, reportDetails: String) -> Unit,
    initialRating: Int = 5,
    initialComment: String = "",
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var rating by remember(initialRating) { mutableStateOf(initialRating) }
    var comment by remember(initialComment) { mutableStateOf(initialComment) }
    var reportExpanded by remember { mutableStateOf(false) }
    var selectedReportReasonId by remember { mutableStateOf<String?>(null) }
    var reportDetails by remember { mutableStateOf("") }
    val selectedReportReason = selectedReportReasonId?.let { ReportReason.fromId(it) }

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
        CedarSectionHeader(title = strings.formTitle(playerName))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.xxs),
        ) {
            Text(
                text = strings.overallLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CedarStarPicker(
                rating = rating,
                onRatingChange = { rating = it },
                starContentDescription = strings.starContentDescription,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it.take(MAX_COMMENT_LENGTH) },
            label = { Text(strings.commentLabel) },
            placeholder = { Text(strings.commentPlaceholder) },
            minLines = 3,
            maxLines = 5,
            shape = CedarTokens.radius.smShape,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            enabled = !isLoading,
        )

        Text(
            text = strings.commentCounter(comment.length, MAX_COMMENT_LENGTH),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End),
        )

        CedarTextButton(
            text = reportStrings.sectionToggle,
            onClick = { reportExpanded = !reportExpanded },
            modifier = Modifier.fillMaxWidth(),
        )

        if (reportExpanded) {
            Column(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
            ) {
                ReportReason.entries.forEach { reason ->
                    val isSelected = reason == selectedReportReason
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    enabled = !isLoading,
                                    role = Role.RadioButton,
                                    onClick = { selectedReportReasonId = reason.id },
                                ).padding(vertical = CedarTokens.spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = isSelected, onClick = null, enabled = !isLoading)
                        Text(
                            text = reportStrings.reasonLabel(reason),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = reportDetails,
                onValueChange = { text ->
                    if (text.length <= ReportReason.MAX_DETAILS_LENGTH) reportDetails = text
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(reportStrings.detailsLabel) },
                placeholder = { Text(reportStrings.detailsPlaceholder) },
                minLines = 3,
                shape = CedarTokens.radius.smShape,
                enabled = !isLoading,
            )
        }

        CedarPrimaryButton(
            text = strings.submitAction,
            onClick = { onSubmit(rating, comment, selectedReportReason, reportDetails.trim()) },
            enabled = true,
            loading = isLoading,
        )
    }
}
