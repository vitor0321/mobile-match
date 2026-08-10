package com.walcker.identity.features.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.walcker.match.cedar.PasswordOutlinedTextField
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.identity.features.data.remote.isAppleSignInAvailable
import com.walcker.identity.strings.LocalIdentityStrings
import com.walcker.identity.strings.WithIdentityStrings
import com.walcker.match.cedar.CedarTopBar

internal class LoginStep : Screen {
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

    Scaffold(
        topBar = {
            CedarTopBar(
                title = strings.title,
                subtitle = strings.subtitle,
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.emailLabel) },
                singleLine = true,
                enabled = !state.isLoading,
            )
            PasswordOutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.passwordLabel) },
                singleLine = true,
                enabled = !state.isLoading,
            )
            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            ) {
                Text(if (state.isLoading) strings.submitLoadingButton else strings.submitButton)
            }
            OutlinedButton(
                onClick = onGoogleSignIn,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            ) {
                Text(strings.googleButton)
            }
            if (isAppleSignInAvailable) {
                OutlinedButton(
                    onClick = onAppleSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                ) {
                    Text(strings.appleButton)
                }
            }
            TextButton(
                onClick = onSignUp,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(strings.signUpButton)
            }
            TextButton(
                onClick = onForgotPassword,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(strings.forgotPasswordButton)
            }
        }
    }
}