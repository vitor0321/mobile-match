package com.walcker.identity.features.ui.forgotpassword

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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.identity.strings.LocalIdentityStrings
import com.walcker.identity.strings.WithIdentityStrings
import com.walcker.match.cedar.CedarTopBar

internal class ForgotPasswordStep : Screen {
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

@Composable
internal fun ForgotPasswordScreen(
    state: ForgotPasswordState,
    onEmailChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    val strings = LocalIdentityStrings.current.forgotPassword

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
            if (state.isSuccess) {
                Text(
                    text = strings.successMessage,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
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
                onClick = onBack,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(strings.backToLoginButton)
            }
        }
    }
}
