package com.walcker.match.app.notifications

import com.walcker.match.navigator.DeepLink
import com.walcker.match.navigator.DeepLinkCoordinator
import org.koin.mp.KoinPlatform

public class IosDeepLinkBridge {
    public fun openMatch(matchId: String) {
        println("DEEPLINK_DEBUG IosDeepLinkBridge.openMatch matchId=$matchId")
        try {
            val coordinator = KoinPlatform.getKoin().get<DeepLinkCoordinator>()
            println("DEEPLINK_DEBUG resolved coordinator=$coordinator")
            coordinator.navigate(DeepLink.OpenMatch(matchId))
            println("DEEPLINK_DEBUG navigate() called")
        } catch (e: Throwable) {
            println("DEEPLINK_DEBUG EXCEPTION in openMatch: $e")
        }
    }

    public companion object {
        private var instance: IosDeepLinkBridge? = null

        public fun getInstance(): IosDeepLinkBridge {
            if (instance == null) {
                instance = IosDeepLinkBridge()
            }
            return instance!!
        }
    }
}
