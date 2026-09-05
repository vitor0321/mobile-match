package com.walcker.identity.features.ui.di

import com.walcker.identity.IdentityDestinationImpl
import com.walcker.identity.features.ui.forgotpassword.ForgotPasswordStepModel
import com.walcker.identity.features.ui.login.LoginStepModel
import com.walcker.identity.features.ui.signup.SignUpStepModel
import com.walcker.match.navigator.IdentityDestination
import org.koin.dsl.module

internal val identityUiModule =
    module {
        factory {
            LoginStepModel(
                signUseCase = get(),
                navigatorHolder = get(),
                stringsHolder = get(),
                loginCoordinator = get(),
                analytics = get(),
                crashReporter = get(),
            )
        }
        factory {
            SignUpStepModel(
                signUseCase = get(),
                navigatorHolder = get(),
                stringsHolder = get(),
                analytics = get(),
                crashReporter = get(),
            )
        }
        factory {
            ForgotPasswordStepModel(
                signUseCase = get(),
                navigatorHolder = get(),
                stringsHolder = get(),
                analytics = get(),
                crashReporter = get(),
            )
        }
        single<IdentityDestination> { IdentityDestinationImpl() }
    }
