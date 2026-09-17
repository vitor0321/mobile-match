package com.walcker.identity.features.ui.verification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.walcker.identity.api.VerificationStage
import com.walcker.identity.features.ui.verification.email.EmailVerificationEvents
import com.walcker.identity.features.ui.verification.email.EmailVerificationInternalRoute
import com.walcker.identity.features.ui.verification.email.EmailVerificationScreen
import com.walcker.identity.features.ui.verification.email.EmailVerificationStepModel
import com.walcker.identity.features.ui.verification.phone.PhoneVerificationMode
import com.walcker.identity.features.ui.verification.phone.PhoneVerificationRoute
import com.walcker.identity.features.ui.verification.phone.PhoneVerificationStepModel
import com.walcker.identity.strings.WithIdentityStrings
import com.walcker.match.cedar.tokens.CedarTokens
import org.koin.core.parameter.parametersOf

internal class VerificationStep : Screen {
    override val key: String get() = "verification"

    @Composable
    override fun Content() {
        WithIdentityStrings {
            val routerModel = koinScreenModel<VerificationStepModel>()
            val stage by routerModel.state.collectAsState()

            LifecycleResumeEffect(routerModel) {
                routerModel.onResumed()
                onPauseOrDispose { }
            }

            when (stage) {
                VerificationStage.Email -> EmailVerificationRoute()
                VerificationStage.Phone ->
                    PhoneVerificationRoute(
                        stepModel =
                            koinScreenModel<PhoneVerificationStepModel>(
                                parameters = { parametersOf(PhoneVerificationMode.Verify) },
                            ),
                        mode = PhoneVerificationMode.Verify,
                    )
                VerificationStage.Done, null ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(CedarTokens.colors.canvas),
                    )
            }
        }
    }

    @Composable
    private fun EmailVerificationRoute() {
        val stepModel = koinScreenModel<EmailVerificationStepModel>()
        val state by stepModel.state.collectAsState()

        EmailVerificationEvents(stepModel = stepModel) { onEvent ->
            EmailVerificationScreen(
                state = state,
                onConfirmed = { onEvent(EmailVerificationInternalRoute.OnConfirmedClicked) },
                onResend = { onEvent(EmailVerificationInternalRoute.OnResendClicked) },
                onLogout = { onEvent(EmailVerificationInternalRoute.OnLogoutClicked) },
            )
        }
    }
}
