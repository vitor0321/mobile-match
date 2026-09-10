package com.walcker.identity.strings.di

import com.walcker.identity.strings.IdentityStringsHolder
import org.koin.dsl.module

internal val identityStringsModule =
    module {
        single { IdentityStringsHolder() }
    }
