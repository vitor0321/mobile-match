package com.walcker.match.core.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIApplication
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

@OptIn(ExperimentalForeignApi::class)
@Composable
public fun AdMobBannerIos(
    adUnitId: String,
    onVisibilityChanged: (Boolean) -> Unit = {},
) {
    val currentOnVisibilityChanged = rememberUpdatedState(onVisibilityChanged)

    val bannerView = remember(adUnitId) {
        currentOnVisibilityChanged.value(false)
        val factory = BannerFactoryHolder.factory
        factory?.createBanner(
            adUnitId = adUnitId,
            rootViewController = findKeyRootViewController(),
            onLoad = {
                currentOnVisibilityChanged.value(true)
            },
            onFail = { description ->
                currentOnVisibilityChanged.value(false)
            },
        ) ?: UIView()
    }

    UIKitView(
        factory = { bannerView },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        properties = UIKitInteropProperties(isInteractive = true, isNativeAccessibilityEnabled = false),
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun findKeyRootViewController(): UIViewController? {
    val scenes = UIApplication.sharedApplication.connectedScenes
    for (scene in scenes) {
        val windowScene = scene as? UIWindowScene ?: continue
        for (window in windowScene.windows) {
            val uiWindow = window as? UIWindow ?: continue
            if (uiWindow.isKeyWindow()) return uiWindow.rootViewController
        }
    }
    return (scenes.firstOrNull() as? UIWindowScene)
        ?.windows
        ?.firstOrNull()
        ?.let { it as? UIWindow }
        ?.rootViewController
}