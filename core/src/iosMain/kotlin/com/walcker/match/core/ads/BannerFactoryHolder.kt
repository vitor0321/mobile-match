package com.walcker.match.core.ads

import platform.UIKit.UIView
import platform.UIKit.UIViewController

public interface BannerFactory {
    public fun createBanner(
        adUnitId: String,
        rootViewController: UIViewController?,
        onLoad: () -> Unit,
        onFail: (errorDescription: String) -> Unit,
    ): UIView
}

public object BannerFactoryHolder {
    public var factory: BannerFactory? = null
}

