package com.walcker.identity.features.ui.di

import com.walcker.identity.IdentityDestinationImpl
import com.walcker.identity.features.ui.login.LoginStepModel
import com.walcker.identity.features.ui.paywall.PaywallStepModel
import com.walcker.identity.features.ui.profile.ProfileStepModel
import com.walcker.identity.features.ui.signup.SignUpStepModel
import com.walcker.identity.features.ui.forgotpassword.ForgotPasswordStepModel
import com.walcker.match.navigator.IdentityDestination
import org.koin.dsl.module

internal val identityUiModule = module {
    factory {
        LoginStepModel(
            signUseCase = get(),
            navigatorHolder = get(),
            stringsHolder = get(),
        )
    }
    factory {
        SignUpStepModel(
            signUseCase = get(),
            navigatorHolder = get(),
            stringsHolder = get(),
        )
    }
    factory {
        ForgotPasswordStepModel(
            signUseCase = get(),
            navigatorHolder = get(),
            stringsHolder = get(),
        )
    }
    factory {
        ProfileStepModel(
            signUseCase = get(),
            deleteAccountUseCase = get(),
            profileAccountUseCase = get(),
            navigatorHolder = get(),
            identityDestination = get(),
            stringsHolder = get(),
        )
    }
    factory {
        PaywallStepModel(
            billingRepository = get(),
            sessionHolder = get(),
            proStateHolder = get(),
            stringsHolder = get(),
        )
    }
    single<IdentityDestination> { IdentityDestinationImpl() }
}
