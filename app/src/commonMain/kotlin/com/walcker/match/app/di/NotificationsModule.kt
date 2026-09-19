package com.walcker.match.app.di

import com.walcker.identity.api.SignOutCleanup
import com.walcker.match.app.notifications.DeviceTokenRegistrar
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

internal expect fun pushNotificationServiceModule(): Module

internal val notificationsModule =
    module {
        includes(pushNotificationServiceModule())
        single {
            DeviceTokenRegistrar(
                sessionHolder = get(),
                pushNotificationService = get(),
                firestore = get(),
            )
        } bind SignOutCleanup::class
    }
