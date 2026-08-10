package com.walcker.identity.fake

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import com.walcker.match.navigator.IdentityDestination

internal class FakeIdentityDestination : IdentityDestination {
    var loginCallCount: Int = 0
        private set

    var signUpCallCount: Int = 0
        private set

    var paywallCallCount: Int = 0
        private set

    var profileCallCount: Int = 0
        private set

    override fun login(): Screen {
        loginCallCount += 1
        return TestIdentityScreen("login")
    }

    override fun signUp(): Screen {
        signUpCallCount += 1
        return TestIdentityScreen("signup")
    }

    override fun paywall(): Screen {
        paywallCallCount += 1
        return TestIdentityScreen("paywall")
    }

    override fun profile(): Screen {
        profileCallCount += 1
        return TestIdentityScreen("profile")
    }
}

private data class TestIdentityScreen(
    private val name: String,
) : Screen {
    @Composable
    override fun Content() = Unit
}

