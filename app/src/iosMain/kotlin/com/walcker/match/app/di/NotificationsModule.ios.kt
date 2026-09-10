package com.walcker.match.app.di

import com.walcker.match.app.notifications.IosPushNotificationService
import com.walcker.match.app.notifications.PushNotificationService
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun pushNotificationServiceModule(): Module =
    module {
        single<PushNotificationService> {
            IosPushNotificationService.getInstance()
        }
    }
