package com.walcker.identity.features.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.identity.features.data.remote.isAppleSignInAvailable
import com.walcker.identity.features.ui.common.AuthFormMessage
import com.walcker.identity.features.ui.common.AuthScaffold
import com.walcker.identity.strings.LocalIdentityStrings
import com.walcker.identity.strings.WithIdentityStrings
import com.walcker.match.cedar.PasswordOutlinedTextField
import com.walcker.match.cedar.components.CedarPrimaryButton
import com.walcker.match.cedar.components.CedarSecondaryButton
import com.walcker.match.cedar.components.CedarTextButton
import com.walcker.match.cedar.tokens.CedarTokens

internal class LoginStep : Screen {

    override val key: String get() = "login"

    @Composable
    override fun Content() {
        WithIdentityStrings {
            val stepModel = koinScreenModel<LoginStepModel>()
            val state by stepModel.state.collectAsState()

            LoginEvents(stepModel = stepModel) { onEvent ->
                LoginScreen(
                    state = state,
                    isAppleSignInAvailable = isAppleSignInAvailable,
                    onEmailChanged = { onEvent(LoginInternalRoute.OnEmailChanged(it)) },
                    onPasswordChanged = { onEvent(LoginInternalRoute.OnPasswordChanged(it)) },
                    onSubmit = { onEvent(LoginInternalRoute.OnSubmitClicked) },
                    onGoogleSignIn = { onEvent(LoginInternalRoute.OnGoogleSignInClicked) },
                    onAppleSignIn = { onEvent(LoginInternalRoute.OnAppleSignInClicked) },
                    onSignUp = { onEvent(LoginInternalRoute.OnSignUpClicked) },
                    onForgotPassword = { onEvent(LoginInternalRoute.OnForgotPasswordClicked) },
                    onBack = { onEvent(LoginInternalRoute.OnBackClicked) },
                )
            }
        }
    }
}

/**
 * Entrar com e-mail e senha, ou com Google/Apple.
 *
 * A hierarquia dos botões era plana: cinco botões de largura cheia empilhados, todos
 * com o mesmo peso visual — o "Entrar" não parecia mais importante que o "Esqueci
 * minha senha". Agora [CedarPrimaryButton] é o único cheio de cor, os provedores
 * sociais são secundários abaixo de um divisor, e os dois links são texto.
 *
 * Os campos ganharam tipo de teclado e ação de IME: o e-mail avança para a senha, a
 * senha envia o formulário. Antes o teclado mostrava "Enter" nos dois e o usuário
 * tinha que fechá-lo para achar o botão.
 */
@Composable
internal fun LoginScreen(
    state: LoginState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    onBack: () -> Unit,
    isAppleSignInAvailable: Boolean = false,
    onAppleSignIn: () -> Unit = {},
) {
    val strings = LocalIdentityStrings.current.login
    val enabled = !state.isLoading

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
            isError = state.error != null,
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
            isError = state.error != null,
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
            text = strings.forgotPasswordButton,
            onClick = onForgotPassword,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )

        SocialDivider(
            label = strings.socialDivider,
            modifier = Modifier.padding(vertical = CedarTokens.spacing.xxs),
        )

        CedarSecondaryButton(
            text = strings.googleButton,
            onClick = onGoogleSignIn,
            enabled = enabled,
        )

        if (isAppleSignInAvailable) {
            CedarSecondaryButton(
                text = strings.appleButton,
                onClick = onAppleSignIn,
                enabled = enabled,
            )
        }

        CedarTextButton(
            text = strings.signUpButton,
            onClick = onSignUp,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Linha, texto, linha — separa "entrar com senha" de "entrar com provedor". */
@Composable
private fun SocialDivider(
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CedarTokens.spacing.sm),
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
