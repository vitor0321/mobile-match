package com.walcker.match.app.di

import com.walcker.match.app.notifications.PushNotificationService
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal expect fun pushNotificationServiceModule(): Module

internal val notificationsModule = module {
    includes(pushNotificationServiceModule())
}
