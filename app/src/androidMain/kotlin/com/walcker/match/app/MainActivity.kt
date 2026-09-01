package com.walcker.match.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.walcker.match.core.location.LocationPermissionRequester
import com.walcker.match.core.location.LocationPermissionRequesterHolder
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

internal class MainActivity :
    ComponentActivity(),
    LocationPermissionRequester {
    private var pendingPermissionContinuation: CancellableContinuation<Boolean>? = null

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            pendingPermissionContinuation?.resume(granted) { _, _, _ -> }
            pendingPermissionContinuation = null
        }

    override suspend fun requestFineLocationPermission(): Boolean =
        suspendCancellableCoroutine { continuation ->
            pendingPermissionContinuation = continuation
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        LocationPermissionRequesterHolder.requester = this

        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        enableEdgeToEdge()
        setContent {
            App(
                onFirstFrameRendered = { keepSplashOnScreen = false },
            )
        }
    }

    override fun onDestroy() {
        if (LocationPermissionRequesterHolder.requester === this) {
            LocationPermissionRequesterHolder.requester = null
        }
        super.onDestroy()
    }
}
