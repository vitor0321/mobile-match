package com.walcker.match.core.di

import com.walcker.match.core.dispatchers.Dispatcher
import com.walcker.match.core.navigation.NavigatorHolder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val coreModule =
    module {
        single { NavigatorHolder() }
        single<CoroutineDispatcher>(named(Dispatcher.IO)) { Dispatchers.IO }
        single<CoroutineDispatcher>(named(Dispatcher.DEFAULT)) { Dispatchers.Default }
        single<CoroutineDispatcher>(named(Dispatcher.MAIN)) { Dispatchers.Main }
    }

public val coreModules: List<Module> = listOf(coreModule, platformCoreModule)
