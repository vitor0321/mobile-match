package com.walcker.games.features.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.walcker.games.features.domain.model.ReportReason
import com.walcker.games.strings.ReportStrings

/**
 * Modal for reporting a player from a match.
 *
 * A report needs the match it happened in — the server refuses a report between
 * people who never played together — so this is reachable from the match detail
 * screen, not from a profile.
 *
 * Reason and free text are local to the sheet: they are a draft until sent, and
 * hoisting them into the screen state would mean threading two more events
 * through the model for no gain.
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        // The id, not the enum: rememberSaveable's default saver has no
        // support for enum values in Compose Multiplatform.
        var selectedReasonId by rememberSaveable { mutableStateOf<String?>(null) }
        var details by rememberSaveable { mutableStateOf("") }
        val selectedReason = selectedReasonId?.let { ReportReason.fromId(it) }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = strings.title(playerName),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = strings.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReportReason.entries.forEach { reason ->
                    FilterChip(
                        selected = reason == selectedReason,
                        onClick = { selectedReasonId = reason.id },
                        label = { Text(strings.reasonLabel(reason)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
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
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(strings.cancel)
                }
                Button(
                    onClick = { selectedReason?.let { onSubmit(it, details.trim()) } },
                    // No reason, no report: sending would only produce a record
                    // moderation cannot act on.
                    enabled = selectedReason != null && !isSubmitting,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Text(strings.submit)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
