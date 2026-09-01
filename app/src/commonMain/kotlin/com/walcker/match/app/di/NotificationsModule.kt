package com.walcker.match.app.di

import org.koin.core.module.Module
import org.koin.dsl.module

internal expect fun pushNotificationServiceModule(): Module

internal val notificationsModule =
    module {
        includes(pushNotificationServiceModule())
    }
