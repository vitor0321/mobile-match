package com.walcker.identity.features.ui.signup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.identity.features.data.remote.isAppleSignInAvailable
import com.walcker.identity.features.ui.common.AuthFormMessage
import com.walcker.identity.features.ui.common.AuthScaffold
import com.walcker.identity.features.ui.common.SocialDivider
import com.walcker.identity.strings.LocalIdentityStrings
import com.walcker.identity.strings.WithIdentityStrings
import com.walcker.match.cedar.PasswordOutlinedTextField
import com.walcker.match.cedar.components.CedarIcons
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.tokens.CedarTokens

internal class SignUpStep : Screen {
    override val key: String get() = "sign-up"

    @Composable
    override fun Content() {
        WithIdentityStrings {
            val stepModel = koinScreenModel<SignUpStepModel>()
            val state by stepModel.state.collectAsState()

            SignUpEvents(stepModel = stepModel) { onEvent ->
                SignUpScreen(
                    state = state,
                    isAppleSignInAvailable = isAppleSignInAvailable,
                    onFullNameChanged = { onEvent(SignUpInternalRoute.OnFullNameChanged(it)) },
                    onEmailChanged = { onEvent(SignUpInternalRoute.OnEmailChanged(it)) },
                    onPasswordChanged = { onEvent(SignUpInternalRoute.OnPasswordChanged(it)) },
                    onConfirmPasswordChanged = {
                        onEvent(SignUpInternalRoute.OnConfirmPasswordChanged(it))
                    },
                    onSubmit = { onEvent(SignUpInternalRoute.OnSubmitClicked) },
                    onGoogleSignIn = { onEvent(SignUpInternalRoute.OnGoogleSignInClicked) },
                    onAppleSignIn = { onEvent(SignUpInternalRoute.OnAppleSignInClicked) },
                    onLogin = { onEvent(SignUpInternalRoute.OnLoginClicked) },
                    onBack = { onEvent(SignUpInternalRoute.OnBackClicked) },
                )
            }
        }
    }
}

@Composable
internal fun SignUpScreen(
    state: SignUpState,
    onFullNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit,
    isAppleSignInAvailable: Boolean = false,
    onGoogleSignIn: () -> Unit = {},
    onAppleSignIn: () -> Unit = {},
) {
    val strings = LocalIdentityStrings.current.signUp
    val enabled = !state.isLoading

    val passwordsDiverge =
        state.confirmPassword.isNotEmpty() &&
            state.password != state.confirmPassword

    AuthScaffold(
        title = strings.title,
        subtitle = strings.subtitle,
        backContentDescription = strings.back,
        onBack = onBack,
    ) {
        OutlinedTextField(
            value = state.fullName,
            onValueChange = onFullNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(strings.fullNameLabel) },
            singleLine = true,
            shape = CedarTokens.radius.smShape,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            enabled = enabled,
        )

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(strings.emailLabel) },
            singleLine = true,
            shape = CedarTokens.radius.smShape,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            enabled = enabled,
        )

        PasswordOutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChanged,
            showPasswordLabel = strings.showPassword,
            hidePasswordLabel = strings.hidePassword,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(strings.passwordLabel) },
            supportingText = strings.passwordHelper,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
            enabled = enabled,
        )

        PasswordOutlinedTextField(
            value = state.confirmPassword,
            onValueChange = onConfirmPasswordChanged,
            showPasswordLabel = strings.showPassword,
            hidePasswordLabel = strings.hidePassword,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(strings.confirmPasswordLabel) },
            supportingText = strings.passwordMismatchError.takeIf { passwordsDiverge },
            isError = passwordsDiverge,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            enabled = enabled,
        )

        state.error?.let { error ->
            AuthFormMessage(text = error, isError = true)
        }

        CedarPrimaryButton(
            text = strings.submitButton,
            onClick = onSubmit,
            enabled = enabled,
            loading = state.isLoading,
            modifier = Modifier.padding(top = CedarTokens.spacing.xxs),
        )

        SocialDivider(
            label = strings.socialDivider,
            modifier = Modifier.padding(vertical = CedarTokens.spacing.xxs),
        )

        CedarSecondaryButton(
            text = strings.googleButton,
            onClick = onGoogleSignIn,
            enabled = enabled,
            leadingIcon = CedarIcons.Google,
            tintLeadingIcon = false,
        )

        if (isAppleSignInAvailable) {
            CedarSecondaryButton(
                text = strings.appleButton,
                onClick = onAppleSignIn,
                enabled = enabled,
                leadingIcon = CedarIcons.Apple,
            )
        }

        CedarTextButton(
            text = strings.loginButton,
            onClick = onLogin,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
