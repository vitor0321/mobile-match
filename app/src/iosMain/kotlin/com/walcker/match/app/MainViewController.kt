package com.walcker.match.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.walcker.identity.features.data.billing.PurchasesBootstrap
import com.walcker.match.app.di.initKoin
import com.walcker.match.cedar.ads.LocalAdBannerContent
import com.walcker.match.core.ads.AdMobBannerIos
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

private fun adMobBannerUnitId(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("ADMOB_BANNER_UNIT_ID") as? String
        ?: "ca-app-pub-8514371864627144/6507442485"

private fun revenueCatIosSdkKey(): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey("RevenueCatIosSdkKey") as? String ?: ""

@Suppress("unused")
public fun MainViewController(
    onFirstFrameRendered: (() -> Unit)? = null,
): UIViewController {
    return ComposeUIViewController(
        configure = {
            PurchasesBootstrap.configure(revenueCatIosSdkKey())
            initKoin()
        },
    ) {
        CompositionLocalProvider(
            LocalAdBannerContent provides { onVisibilityChanged ->
                AdMobBannerIos(
                    adUnitId = adMobBannerUnitId(),
                    onVisibilityChanged = onVisibilityChanged,
                )
            },
        ) {
            App(onFirstFrameRendered = onFirstFrameRendered)
        }
    }
}