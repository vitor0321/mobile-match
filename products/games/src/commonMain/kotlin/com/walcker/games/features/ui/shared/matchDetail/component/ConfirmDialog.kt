package com.walcker.games.features.ui.shared.matchDetail.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.tokens.CedarTokens

private val DIALOG_SPINNER = 16.dp

@Composable
internal fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    isWorking: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        shape = CedarTokens.radius.lgShape,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isWorking) {
                if (isWorking) {
                    CedarLoading(contentDescription = confirmLabel, size = DIALOG_SPINNER)
                } else {
                    Text(confirmLabel)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}
