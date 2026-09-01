package com.walcker.identity.strings

import cafe.adriel.lyricist.LyricistStrings
import com.walcker.match.core.strings.Locales

@LyricistStrings(languageTag = Locales.EN)
internal val EnIdentityStrings =
    IdentityStrings(
        login = loginStringsEn,
        nativeAuth = nativeAuthStringsEn,
        signUp = signUpStringsEn,
        profile = profileStringsEn,
        paywall = paywallStringsEn,
        forgotPassword = forgotPasswordStringsEn,
    )
