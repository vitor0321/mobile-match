package com.walcker.match.app.di

import android.content.Context
import com.walcker.match.app.notifications.AndroidPushNotificationService
import com.walcker.match.app.notifications.PushNotificationService
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal actual fun pushNotificationServiceModule(): Module = module {
    single<PushNotificationService> {
        AndroidPushNotificationService(context = get<Context>())
    }
}
