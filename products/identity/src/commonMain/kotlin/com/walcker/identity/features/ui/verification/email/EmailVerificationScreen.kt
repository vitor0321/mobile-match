package com.walcker.identity.features.ui.verification.email

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.walcker.identity.features.ui.common.AuthFormMessage
import com.walcker.identity.features.ui.common.AuthScaffold
import com.walcker.identity.strings.LocalIdentityStrings
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.tokens.CedarTokens

@Composable
internal fun EmailVerificationScreen(
    state: EmailVerificationState,
    onConfirmed: () -> Unit,
    onResend: () -> Unit,
    onLogout: () -> Unit,
) {
    val strings = LocalIdentityStrings.current.verification
    val isBusy = state.isSending || state.isChecking

    AuthScaffold(
        title = strings.emailTitle,
        subtitle = strings.emailSubtitle,
        backContentDescription = null,
        onBack = null,
    ) {
        Text(
            text = strings.emailSentLead,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = state.email,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = strings.emailSentAction,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = strings.emailSpamHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.message?.let { message ->
            AuthFormMessage(text = message, isError = state.isMessageError)
        }

        CedarPrimaryButton(
            text = strings.emailConfirmedButton,
            onClick = onConfirmed,
            enabled = !isBusy,
            loading = state.isChecking,
            modifier = Modifier.padding(top = CedarTokens.spacing.xxs),
        )

        CedarSecondaryButton(
            text =
                if (state.resendAvailableInSeconds > 0) {
                    strings.resendIn(state.resendAvailableInSeconds)
                } else {
                    strings.emailResendButton
                },
            onClick = onResend,
            enabled = state.resendAvailableInSeconds == 0 && !isBusy,
            loading = state.isSending,
        )

        CedarTextButton(
            text = strings.logoutButton,
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
