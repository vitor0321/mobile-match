package com.walcker.identity.features.ui.verification.phone

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.walcker.identity.features.domain.phone.BRAZIL_DIAL_CODE
import com.walcker.identity.features.domain.phone.flagEmoji
import com.walcker.identity.features.ui.common.AuthFormMessage
import com.walcker.identity.features.ui.common.AuthScaffold
import com.walcker.identity.strings.LocalIdentityStrings
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.tokens.CedarTokens

private val CountryFieldMinHeight = 56.dp
private val CountryFieldBorderWidth = 1.dp

@Composable
internal fun PhoneVerificationScreen(
    state: PhoneVerificationState,
    countryName: String,
    onCountryClick: () -> Unit,
    onNationalNumberChanged: (String) -> Unit,
    onSendCode: () -> Unit,
    onCodeChanged: (String) -> Unit,
    onConfirmCode: () -> Unit,
    onResend: () -> Unit,
    onChangeNumber: () -> Unit,
    onLogout: () -> Unit,
    mode: PhoneVerificationMode = PhoneVerificationMode.Verify,
    onBack: () -> Unit = {},
) {
    val strings = LocalIdentityStrings.current.verification
    val enabled = !state.isLoading
    val isChanging = mode == PhoneVerificationMode.Change

    AuthScaffold(
        title = if (isChanging) strings.changePhoneTitle else strings.phoneTitle,
        subtitle = if (isChanging) strings.changePhoneSubtitle else strings.phoneSubtitle,
        backContentDescription = strings.back.takeIf { isChanging },
        onBack = onBack.takeIf { isChanging },
    ) {
        when (state.phase) {
            PhoneVerificationPhase.Number -> {
                CountryField(
                    flag = flagEmoji(state.country.isoCode),
                    countryName = countryName,
                    dialCode = state.country.dialCode,
                    label = strings.countryLabel,
                    enabled = enabled,
                    onClick = onCountryClick,
                )

                OutlinedTextField(
                    value = state.nationalNumber,
                    onValueChange = onNationalNumberChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.phoneNumberLabel) },
                    prefix = { Text("+${state.country.dialCode} ") },
                    singleLine = true,
                    shape = CedarTokens.radius.smShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSendCode() }),
                    visualTransformation =
                        if (state.country.dialCode == BRAZIL_DIAL_CODE) {
                            BrazilianPhoneVisualTransformation
                        } else {
                            VisualTransformation.None
                        },
                    enabled = enabled,
                )

                state.message?.let { message -> AuthFormMessage(text = message, isError = true) }

                CedarPrimaryButton(
                    text = strings.sendCodeButton,
                    onClick = onSendCode,
                    enabled = enabled,
                    loading = state.isLoading,
                    modifier = Modifier.padding(top = CedarTokens.spacing.xxs),
                )
            }

            PhoneVerificationPhase.Code -> {
                Text(
                    text = strings.codeSentTo(state.sentToE164.orEmpty()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                OutlinedTextField(
                    value = state.code,
                    onValueChange = onCodeChanged,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { contentType = ContentType.SmsOtpCode },
                    label = { Text(strings.codeLabel) },
                    singleLine = true,
                    shape = CedarTokens.radius.smShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirmCode() }),
                    enabled = enabled,
                )

                state.message?.let { message -> AuthFormMessage(text = message, isError = true) }

                CedarPrimaryButton(
                    text = strings.confirmCodeButton,
                    onClick = onConfirmCode,
                    enabled = enabled && state.code.length == PHONE_CODE_LENGTH,
                    loading = state.isLoading,
                    modifier = Modifier.padding(top = CedarTokens.spacing.xxs),
                )

                CedarSecondaryButton(
                    text =
                        if (state.resendAvailableInSeconds > 0) {
                            strings.resendIn(state.resendAvailableInSeconds)
                        } else {
                            strings.resendCodeButton
                        },
                    onClick = onResend,
                    enabled = enabled && state.resendAvailableInSeconds == 0,
                )

                CedarTextButton(
                    text = strings.changeNumberButton,
                    onClick = onChangeNumber,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (!isChanging) {
            CedarTextButton(
                text = strings.logoutButton,
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CountryField(
    flag: String,
    countryName: String,
    dialCode: String,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CedarTokens.radius.smShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(CountryFieldBorderWidth, MaterialTheme.colorScheme.outline),
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = CountryFieldMinHeight),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = CedarTokens.spacing.md, vertical = CedarTokens.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
        ) {
            Text(
                text = flag,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.clearAndSetSemantics { },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$countryName (+$dialCode)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
