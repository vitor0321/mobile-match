package com.walcker.games.features.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.walcker.games.features.domain.model.ReportReason
import com.walcker.games.strings.ReportStrings
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarSectionHeader
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.tokens.CedarTokens

/**
 * Modal for reporting a player from a match.
 *
 * A report needs the match it happened in — the server refuses a report between
 * people who never played together — so this is reachable from the match detail
 * screen, not from a profile.
 *
 * Reason and free text are local to the sheet: they are a draft until sent, and
 * hoisting them into the screen state would mean threading two more events through
 * the model for no gain.
 *
 * The reasons were full-width `FilterChip`s. A chip is a filter you can combine;
 * this is one choice out of five, so they are radio rows now — which is also how a
 * screen reader will announce them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportBottomSheet(
    isVisible: Boolean,
    playerName: String,
    strings: ReportStrings,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (reason: ReportReason, details: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = CedarTokens.radius.sheet,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        // The id, not the enum: rememberSaveable's default saver has no support
        // for enum values in Compose Multiplatform.
        var selectedReasonId by rememberSaveable { mutableStateOf<String?>(null) }
        var details by rememberSaveable { mutableStateOf("") }
        val selectedReason = selectedReasonId?.let { ReportReason.fromId(it) }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = CedarTokens.spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.md),
        ) {
            CedarSectionHeader(
                title = strings.title(playerName),
                subtitle = strings.subtitle,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
            ) {
                ReportReason.entries.forEach { reason ->
                    val isSelected = reason == selectedReason
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                enabled = !isSubmitting,
                                role = Role.RadioButton,
                                onClick = { selectedReasonId = reason.id },
                            )
                            .padding(vertical = CedarTokens.spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isSelected,
                            // The row owns the click; a second target here would
                            // make the same choice tappable twice.
                            onClick = null,
                            enabled = !isSubmitting,
                        )
                        Text(
                            text = strings.reasonLabel(reason),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = details,
                onValueChange = { text ->
                    if (text.length <= ReportReason.MAX_DETAILS_LENGTH) details = text
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.detailsLabel) },
                placeholder = { Text(strings.detailsPlaceholder) },
                minLines = 3,
                shape = CedarTokens.radius.smShape,
                enabled = !isSubmitting,
            )

            CedarPrimaryButton(
                text = strings.submit,
                onClick = { selectedReason?.let { onSubmit(it, details.trim()) } },
                // No reason, no report: sending would only produce a record
                // moderation cannot act on.
                enabled = selectedReason != null,
                loading = isSubmitting,
            )
            CedarTextButton(
                text = strings.cancel,
                onClick = onDismiss,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(CedarTokens.spacing.xl))
        }
    }
}
