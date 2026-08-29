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
import com.walcker.identity.features.ui.common.AuthFormMessage
import com.walcker.identity.features.ui.common.AuthScaffold
import com.walcker.identity.strings.LocalIdentityStrings
import com.walcker.identity.strings.WithIdentityStrings
import com.walcker.match.cedar.PasswordOutlinedTextField
import com.walcker.match.cedar.components.CedarPrimaryButton
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
                    onEmailChanged = { onEvent(SignUpInternalRoute.OnEmailChanged(it)) },
                    onPasswordChanged = { onEvent(SignUpInternalRoute.OnPasswordChanged(it)) },
                    onConfirmPasswordChanged = {
                        onEvent(SignUpInternalRoute.OnConfirmPasswordChanged(it))
                    },
                    onSubmit = { onEvent(SignUpInternalRoute.OnSubmitClicked) },
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
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit,
) {
    val strings = LocalIdentityStrings.current.signUp
    val enabled = !state.isLoading

    val passwordsDiverge = state.confirmPassword.isNotEmpty() &&
        state.password != state.confirmPassword

    AuthScaffold(
        title = strings.title,
        subtitle = strings.subtitle,
        backContentDescription = strings.back,
        onBack = onBack,
    ) {
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(strings.emailLabel) },
            singleLine = true,
            shape = CedarTokens.radius.smShape,
            keyboardOptions = KeyboardOptions(
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
            keyboardOptions = KeyboardOptions(
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
            keyboardOptions = KeyboardOptions(
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

        CedarTextButton(
            text = strings.loginButton,
            onClick = onLogin,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
