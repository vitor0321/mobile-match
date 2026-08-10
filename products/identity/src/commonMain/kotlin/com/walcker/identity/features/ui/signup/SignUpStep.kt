package com.walcker.identity.features.ui.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.walcker.match.cedar.PasswordOutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.identity.strings.LocalIdentityStrings
import com.walcker.identity.strings.WithIdentityStrings
import com.walcker.match.cedar.CedarTopBar

internal class SignUpStep : Screen {
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
                    onConfirmPasswordChanged = { onEvent(SignUpInternalRoute.OnConfirmPasswordChanged(it)) },
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
            PasswordOutlinedTextField(
                value = state.confirmPassword,
                onValueChange = onConfirmPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.confirmPasswordLabel) },
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
            TextButton(
                onClick = onLogin,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(strings.loginButton)
            }
        }
    }
}