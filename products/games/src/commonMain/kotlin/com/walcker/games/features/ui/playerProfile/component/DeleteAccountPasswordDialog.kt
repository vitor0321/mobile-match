package com.walcker.games.features.ui.playerProfile.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.walcker.games.strings.PlayerProfileStrings
import com.walcker.match.cedar.PasswordOutlinedTextField
import com.walcker.match.cedar.components.CedarLoading
import com.walcker.match.cedar.tokens.CedarTokens

private val DIALOG_SPINNER = 16.dp

@Composable
internal fun DeleteAccountPasswordDialog(
    password: String,
    error: String?,
    isWorking: Boolean,
    strings: PlayerProfileStrings,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.deleteAccountPasswordTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm)) {
                Text(strings.deleteAccountPasswordBody)
                PasswordOutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    showPasswordLabel = strings.showPassword,
                    hidePasswordLabel = strings.hidePassword,
                    label = { Text(strings.deleteAccountPasswordLabel) },
                    isError = error != null,
                    enabled = !isWorking,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { contentType = ContentType.Password },
                )
                error?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        },
        shape = CedarTokens.radius.lgShape,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isWorking && password.isNotBlank()) {
                if (isWorking) {
                    CedarLoading(contentDescription = strings.deleteAccountPasswordConfirm, size = DIALOG_SPINNER)
                } else {
                    Text(strings.deleteAccountPasswordConfirm)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) { Text(strings.deleteAccountDialogDismiss) }
        },
    )
}
