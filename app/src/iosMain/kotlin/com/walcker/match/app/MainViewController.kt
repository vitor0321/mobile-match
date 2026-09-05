package com.walcker.match.app

import androidx.compose.ui.window.ComposeUIViewController
import com.walcker.identity.features.data.billing.PurchasesBootstrap
import com.walcker.match.app.di.initKoin
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

private fun revenueCatIosSdkKey(): String = NSBundle.mainBundle.objectForInfoDictionaryKey("RevenueCatIosSdkKey") as? String ?: ""

@Suppress("unused", "ktlint:standard:function-naming")
public fun MainViewController(
    onFirstFrameRendered: (() -> Unit)? = null,
): UIViewController =
    ComposeUIViewController(
        configure = {
            PurchasesBootstrap.configure(revenueCatIosSdkKey())
            initKoin()
        },
    ) {
        App(onFirstFrameRendered = onFirstFrameRendered)
    }
