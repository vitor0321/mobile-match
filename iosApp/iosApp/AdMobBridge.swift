import App
import GoogleMobileAds
import UIKit

final class AdMobBridge: NSObject, BannerFactory {
    static var isMobileAdsReady = false
    private static var pendingContainers: [WeakBox] = []

    private final class WeakBox {
        weak var value: BannerContainerView?

        init(_ value: BannerContainerView) {
            self.value = value
        }
    }

    static func notifyMobileAdsReady() {
        isMobileAdsReady = true
        let queued = pendingContainers
        pendingContainers.removeAll()
        queued.forEach { $0.value?.tryLoad() }
    }

    static func enqueue(_ container: BannerContainerView) {
        pendingContainers.append(WeakBox(container))
    }

    func createBanner(
        adUnitId: String,
        rootViewController: UIViewController?,
        onLoad: @escaping () -> Void,
        onFail: @escaping (String) -> Void
    ) -> UIView {
        BannerContainerView(
            adUnitId: adUnitId,
            rootViewController: rootViewController,
            onLoad: onLoad,
            onFail: onFail
        )
    }
}

final class BannerContainerView: UIView {
    private let bannerView: BannerView
    private let delegate: AdMobBridgeDelegate
    private var hasLoaded = false
    private var isQueuedForReady = false

    init(
        adUnitId: String,
        rootViewController: UIViewController?,
        onLoad: @escaping () -> Void,
        onFail: @escaping (String) -> Void
    ) {
        let size = adSizeFor(cgSize: CGSize(width: 320, height: 50))
        bannerView = BannerView(adSize: size)
        delegate = AdMobBridgeDelegate(onLoad: onLoad, onFail: onFail)

        super.init(frame: CGRect(x: 0, y: 0, width: 320, height: 50))

        bannerView.adUnitID = adUnitId
        bannerView.rootViewController = rootViewController
        bannerView.delegate = delegate
        bannerView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(bannerView)
        NSLayoutConstraint.activate([
            bannerView.centerXAnchor.constraint(equalTo: centerXAnchor),
            bannerView.centerYAnchor.constraint(equalTo: centerYAnchor),
            bannerView.widthAnchor.constraint(equalToConstant: 320),
            bannerView.heightAnchor.constraint(equalToConstant: 50),
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError()
    }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        guard window != nil else { return }
        tryLoad()
    }

    func tryLoad() {
        guard !hasLoaded, window != nil else { return }
        if !AdMobBridge.isMobileAdsReady {
            if !isQueuedForReady {
                isQueuedForReady = true
                AdMobBridge.enqueue(self)
            }
            return
        }
        hasLoaded = true
        bannerView.load(Request())
    }
}

private final class AdMobBridgeDelegate: NSObject, BannerViewDelegate {
    private let onLoad: () -> Void
    private let onFail: (String) -> Void

    init(onLoad: @escaping () -> Void, onFail: @escaping (String) -> Void) {
        self.onLoad = onLoad
        self.onFail = onFail
    }

    func bannerViewDidReceiveAd(_ bannerView: BannerView) {
        onLoad()
    }

    func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
        let nsError = error as NSError
        onFail("code=\(nsError.code) domain=\(nsError.domain) description=\(nsError.localizedDescription)")
    }
}

