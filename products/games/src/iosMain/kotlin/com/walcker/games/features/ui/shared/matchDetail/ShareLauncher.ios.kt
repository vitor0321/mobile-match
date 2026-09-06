package com.walcker.games.features.ui.shared.matchDetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

@Composable
internal actual fun rememberShareLauncher(): (subject: String, text: String) -> Unit {
    return remember {
        { _: String, text: String ->
            val activityController =
                UIActivityViewController(
                    activityItems = listOf(text),
                    applicationActivities = null,
                )
            currentRootViewController()?.presentViewController(
                activityController,
                animated = true,
                completion = null,
            )
        }
    }
}

private fun currentRootViewController(): UIViewController? {
    val app = UIApplication.sharedApplication
    @Suppress("DEPRECATION")
    val window = app.keyWindow ?: app.windows.firstOrNull() as? UIWindow
    return window?.rootViewController
}
