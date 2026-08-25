package com.walcker.identity.features.ui.forgotpassword

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
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.tokens.CedarTokens

internal class ForgotPasswordStep : Screen {

    override val key: String get() = "forgot-password"

    @Composable
    override fun Content() {
        WithIdentityStrings {
            val stepModel = koinScreenModel<ForgotPasswordStepModel>()
            val state by stepModel.state.collectAsState()

            ForgotPasswordEvents(stepModel = stepModel) { onEvent ->
                ForgotPasswordScreen(
                    state = state,
                    onEmailChanged = { onEvent(ForgotPasswordInternalRoute.OnEmailChanged(it)) },
                    onSubmit = { onEvent(ForgotPasswordInternalRoute.OnSubmitClicked) },
                    onBack = { onEvent(ForgotPasswordInternalRoute.OnBackClicked) },
                )
            }
        }
    }
}

/**
 * Recuperação de senha: um campo, um botão.
 *
 * Depois do envio bem-sucedido o formulário sai da tela e fica só a confirmação com
 * o caminho de volta. Antes o campo e o botão continuavam ali, do mesmo jeito, e
 * nada dizia que a tarefa tinha acabado — dava para reenviar sem perceber.
 */
@Composable
internal fun ForgotPasswordScreen(
    state: ForgotPasswordState,
    onEmailChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    val strings = LocalIdentityStrings.current.forgotPassword
    val enabled = !state.isLoading

    AuthScaffold(
        title = strings.title,
        subtitle = strings.subtitle,
        backContentDescription = strings.back,
        onBack = onBack,
    ) {
        if (state.isSuccess) {
            AuthFormMessage(text = strings.successMessage, isError = false)
        } else {
            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.emailLabel) },
                singleLine = true,
                shape = CedarTokens.radius.smShape,
                isError = state.error != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
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
        }

        CedarTextButton(
            text = strings.backToLoginButton,
            onClick = onBack,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
