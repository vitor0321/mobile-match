package com.walcker.identity.strings

import cafe.adriel.lyricist.LyricistStrings
import com.walcker.match.core.strings.Locales

@LyricistStrings(languageTag = Locales.PT, default = true)
internal val PtBrIdentityStrings = IdentityStrings(
    login = loginStringsPt,
    nativeAuth = nativeAuthStringsPt,
    signUp = signUpStringsPt,
    profile = profileStringsPt,
    paywall = paywallStringsPt,
    forgotPassword = forgotPasswordStringsPt,
)

