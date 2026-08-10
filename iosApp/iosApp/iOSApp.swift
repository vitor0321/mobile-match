import App
import FirebaseCore
import Foundation
import GoogleSignIn
import GoogleMobileAds
import SwiftUI
import UIKit

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil,
    ) -> Bool {
        FirebaseApp.configure()
        configureGoogleSignIn()
        BannerFactoryHolder.shared.factory = AdMobBridge()
        MobileAds.shared.start { _ in
            AdMobBridge.notifyMobileAdsReady()
        }
        return true
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey : Any] = [:],
    ) -> Bool {
        GIDSignIn.sharedInstance.handle(url)
    }

    private func configureGoogleSignIn() {
        guard let clientID = resolveGoogleClientID() else {
            assertionFailure("Missing Google Sign-In client ID. Check GoogleService-Info.plist.")
            return
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
    }

    private func resolveGoogleClientID() -> String? {
        if let clientID = FirebaseApp.app()?.options.clientID, !clientID.isEmpty {
            return clientID
        }

        guard let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
              let plist = NSDictionary(contentsOfFile: path),
              let clientID = plist["CLIENT_ID"] as? String,
              !clientID.isEmpty else {
            return nil
        }

        return clientID
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
