package com.walcker.match.app.notifications

import com.walcker.match.navigator.DeepLink
import com.walcker.match.navigator.DeepLinkCoordinator
import org.koin.mp.KoinPlatform

public class IosDeepLinkBridge {
    public fun openMatch(matchId: String) {
        KoinPlatform.getKoin().get<DeepLinkCoordinator>().navigate(DeepLink.OpenMatch(matchId))
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
