import App
import FirebaseAuth
import FirebaseCore
import FirebaseMessaging
import Foundation
import GoogleSignIn
import SwiftUI
import UIKit
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil,
    ) -> Bool {
        FirebaseApp.configure()
        signOutStaleKeychainSessionOnFreshInstall()
        configureGoogleSignIn()
        configurePushNotifications(application)
        return true
    }

    private func signOutStaleKeychainSessionOnFreshInstall() {
        let hasLaunchedKey = "com.walcker.match.hasLaunchedBefore"
        guard !UserDefaults.standard.bool(forKey: hasLaunchedKey) else { return }
        try? Auth.auth().signOut()
        UserDefaults.standard.set(true, forKey: hasLaunchedKey)
    }

    private func configurePushNotifications(_ application: UIApplication) {
        // Set Firebase Messaging delegate
        Messaging.messaging().delegate = self

        // Set UNUserNotificationCenter delegate for handling notifications
        UNUserNotificationCenter.current().delegate = self

        // Register for APNs regardless of the notification permission: the silent
        // push Firebase Phone Auth uses to prove the device does not need one.
        // Gating this on `granted` sent everyone who declined notifications to the
        // reCAPTCHA web fallback.
        application.registerForRemoteNotifications()

        // Request notification permissions (user-visible notifications only)
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Auth.auth().setAPNSToken(deviceToken, type: .unknown)
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        _ = Auth.auth().canHandleNotification(userInfo)
        completionHandler(.noData)
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey : Any] = [:],
    ) -> Bool {
        if Auth.auth().canHandle(url) {
            return true
        }
        return GIDSignIn.sharedInstance.handle(url)
    }

    // MARK: - MessagingDelegate

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        if let fcmToken = fcmToken {
            // Call Kotlin callback to store token
            IosPushNotificationService.companion.getInstance().onTokenReceived(token: fcmToken)
        }
    }

    // MARK: - UNUserNotificationCenterDelegate

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // Show the system banner/sound/badge even while the app is in foreground.
        completionHandler([.banner, .sound, .badge])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if let matchId = response.notification.request.content.userInfo["matchId"] as? String {
            IosDeepLinkBridge.companion.getInstance().openMatch(matchId: matchId)
        }
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

private func matchIdFromMatchLink(_ url: URL) -> String? {
    let components = URLComponents(url: url, resolvingAgainstBaseURL: true)
    if let id = components?.queryItems?.first(where: { $0.name == "id" })?.value, !id.isEmpty {
        return id
    }
    let lastComponent = url.lastPathComponent
    return lastComponent.isEmpty ? nil : lastComponent
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    if Auth.auth().canHandle(url) {
                        return
                    }
                    print("DEEPLINK_DEBUG onOpenURL received url=\(url.absoluteString)")
                    guard let matchId = matchIdFromMatchLink(url) else {
                        print("DEEPLINK_DEBUG onOpenURL could not extract matchId")
                        return
                    }
                    print("DEEPLINK_DEBUG onOpenURL matchId=\(matchId), calling openMatch")
                    IosDeepLinkBridge.companion.getInstance().openMatch(matchId: matchId)
                }
        }
    }
}
