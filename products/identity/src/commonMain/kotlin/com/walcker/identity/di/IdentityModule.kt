package com.walcker.identity.di

import com.walcker.identity.features.data.di.identityDataModule
import com.walcker.identity.features.data.platform.identityPlatformModule
import com.walcker.identity.features.ui.di.identityUiModule
import com.walcker.identity.strings.di.identityStringsModule
import org.koin.core.module.Module

public val identityModule: List<Module> =
    listOf(
        identityStringsModule,
        identityPlatformModule,
        identityDataModule,
        identityUiModule,
    )
