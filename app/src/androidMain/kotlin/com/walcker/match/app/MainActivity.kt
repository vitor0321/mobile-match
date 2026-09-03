package com.walcker.match.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.walcker.match.app.notifications.EXTRA_MATCH_ID
import com.walcker.match.app.notifications.NotificationPermissionRequester
import com.walcker.match.app.notifications.NotificationPermissionRequesterHolder
import com.walcker.match.core.location.LocationPermissionRequester
import com.walcker.match.core.location.LocationPermissionRequesterHolder
import com.walcker.match.navigator.DeepLink
import com.walcker.match.navigator.DeepLinkCoordinator
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.android.ext.android.inject

internal class MainActivity :
    ComponentActivity(),
    LocationPermissionRequester,
    NotificationPermissionRequester {
    private val deepLinkCoordinator: DeepLinkCoordinator by inject()

    private var pendingPermissionContinuation: CancellableContinuation<Boolean>? = null
    private var pendingNotificationPermissionContinuation: CancellableContinuation<Boolean>? = null

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            pendingPermissionContinuation?.resume(granted) { _, _, _ -> }
            pendingPermissionContinuation = null
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            pendingNotificationPermissionContinuation?.resume(granted) { _, _, _ -> }
            pendingNotificationPermissionContinuation = null
        }

    override suspend fun requestFineLocationPermission(): Boolean =
        suspendCancellableCoroutine { continuation ->
            pendingPermissionContinuation = continuation
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    override suspend fun requestNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return suspendCancellableCoroutine { continuation ->
            pendingNotificationPermissionContinuation = continuation
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        LocationPermissionRequesterHolder.requester = this
        NotificationPermissionRequesterHolder.requester = this

        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        enableEdgeToEdge()
        setContent {
            App(
                onFirstFrameRendered = { keepSplashOnScreen = false },
            )
        }

        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val matchId = intent?.getStringExtra(EXTRA_MATCH_ID) ?: return
        deepLinkCoordinator.navigate(DeepLink.OpenMatch(matchId))
    }

    override fun onDestroy() {
        if (LocationPermissionRequesterHolder.requester === this) {
            LocationPermissionRequesterHolder.requester = null
        }
        if (NotificationPermissionRequesterHolder.requester === this) {
            NotificationPermissionRequesterHolder.requester = null
        }
        super.onDestroy()
    }
}
