package com.walcker.identity

import com.walcker.identity.features.ui.login.LoginStep
import com.walcker.identity.features.ui.signup.SignUpStep
import com.walcker.match.navigator.IdentityDestination

internal class IdentityDestinationImpl : IdentityDestination {
    override fun login() = LoginStep()

    override fun signUp() = SignUpStep()
}
