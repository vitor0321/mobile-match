package com.walcker.match.app.di

import com.walcker.identity.di.identityModule
import com.walcker.match.core.di.coreModules
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.KoinAppDeclaration

internal fun initKoin(
    isDebug: Boolean = false,
    config: KoinAppDeclaration? = null,
): KoinApplication = startKoin {
    config?.invoke(this)
    modules(coreModules + identityModule)
}
