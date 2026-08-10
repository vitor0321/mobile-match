package com.walcker.match.core.ads

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Renders a real AdMob banner ad on Android using [AdView].
 *
 * Requires `MobileAds.initialize(...)` to be called once in the Android app before use.
 *
 * @param adUnitId AdMob banner unit ID. Use the test ID during development:
 *   `ca-app-pub-3940256099942544/6300978111`
 */
@Composable
public fun AdMobBannerAndroid(
    adUnitId: String,
    onVisibilityChanged: (Boolean) -> Unit = {},
) {
    val currentOnVisibilityChanged = rememberUpdatedState(onVisibilityChanged)

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { context: Context ->
            AdView(context).apply {
                currentOnVisibilityChanged.value(false)
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        currentOnVisibilityChanged.value(false)
                        Log.e("AdMobBanner", "Failed to load ad: ${error.message} (code=${error.code})")
                    }

                    override fun onAdLoaded() {
                        currentOnVisibilityChanged.value(true)
                        Log.d("AdMobBanner", "Ad loaded successfully")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}