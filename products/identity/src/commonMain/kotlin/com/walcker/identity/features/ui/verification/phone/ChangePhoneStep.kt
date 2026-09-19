package com.walcker.identity.features.ui.verification.phone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.walcker.identity.strings.WithIdentityStrings
import com.walcker.match.navigator.BottomBarVisibilityCoordinator
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

internal class ChangePhoneStep : Screen {
    override val key: String get() = "change-phone"

    @Composable
    override fun Content() {
        WithIdentityStrings {
            val navigator = LocalNavigator.currentOrThrow
            val bottomBarVisibility: BottomBarVisibilityCoordinator = koinInject()
            val stepModel =
                koinScreenModel<PhoneVerificationStepModel>(
                    parameters = { parametersOf(PhoneVerificationMode.Change) },
                )
            val state by stepModel.state.collectAsState()

            DisposableEffect(bottomBarVisibility) {
                bottomBarVisibility.setVisible(false)
                onDispose { bottomBarVisibility.setVisible(true) }
            }

            LaunchedEffect(state.isPhoneChanged) {
                if (state.isPhoneChanged) navigator.pop()
            }

            PhoneVerificationRoute(
                stepModel = stepModel,
                mode = PhoneVerificationMode.Change,
                onBack = { navigator.pop() },
            )
        }
    }
}
