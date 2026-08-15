import App
import FirebaseCore
import FirebaseMessaging
import Foundation
import GoogleSignIn
import GoogleMobileAds
import SwiftUI
import UIKit
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil,
    ) -> Bool {
        FirebaseApp.configure()
        configureGoogleSignIn()
        configurePushNotifications(application)
        BannerFactoryHolder.shared.factory = AdMobBridge()
        MobileAds.shared.start { _ in
            AdMobBridge.notifyMobileAdsReady()
        }
        return true
    }

    private func configurePushNotifications(_ application: UIApplication) {
        // Set Firebase Messaging delegate
        Messaging.messaging().delegate = self

        // Set UNUserNotificationCenter delegate for handling notifications
        UNUserNotificationCenter.current().delegate = self

        // Request notification permissions
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
            DispatchQueue.main.async {
                if granted {
                    application.registerForRemoteNotifications()
                }
            }
        }
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey : Any] = [:],
    ) -> Bool {
        GIDSignIn.sharedInstance.handle(url)
    }

    // MARK: - MessagingDelegate

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        if let fcmToken = fcmToken {
            // Call Kotlin callback to store token
            IosPushNotificationService.getInstance().onTokenReceived(token: fcmToken)
        }
    }

    // MARK: - UNUserNotificationCenterDelegate

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // Handle notification when app is in foreground
        // In ETAPA3, we'll process and show local notification
        completionHandler([.banner, .sound, .badge])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        // Handle notification tap
        // In ETAPA3, we'll deep link to match details
        completionHandler()
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
