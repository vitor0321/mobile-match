package com.walcker.match.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.walcker.match.cedar.ads.LocalAdBannerContent
import com.walcker.match.core.ads.AdMobBannerAndroid

private fun adMobBannerUnitId(): String =
    BuildConfig.ADMOB_BANNER_UNIT_ID.ifBlank { "ca-app-pub-8514371864627144/7820524155" }

internal class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(
                LocalAdBannerContent provides { onVisibilityChanged ->
                    AdMobBannerAndroid(
                        adUnitId = adMobBannerUnitId(),
                        onVisibilityChanged = onVisibilityChanged,
                    )
                }
            ) {
                App(
                    onFirstFrameRendered = { keepSplashOnScreen = false },
                )
            }
        }
    }
}
