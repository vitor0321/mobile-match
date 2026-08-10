import App
import SwiftUI
import UIKit

struct ComposeView: UIViewControllerRepresentable {
    let onFirstFrameRendered: (() -> Void)?

    func makeUIViewController(context: Context) -> UIViewController {
        return MainViewControllerKt.MainViewController(
            onFirstFrameRendered: onFirstFrameRendered,
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var isSplashVisible = true
    @State private var hasRenderedFirstFrame = false
    @State private var splashStartedAt = Date()

    private let minimumSplashDuration: TimeInterval = 0.65

    var body: some View {
        ZStack {
            ComposeView(onFirstFrameRendered: handleFirstFrameRendered)
                .ignoresSafeArea()

            if isSplashVisible {
                MatchSplashView()
                    .transition(.opacity)
                    .ignoresSafeArea()
                    .zIndex(1)
            }
        }
        .onAppear {
            splashStartedAt = Date()
        }
    }

    private func handleFirstFrameRendered() {
        guard !hasRenderedFirstFrame else { return }
        hasRenderedFirstFrame = true

        let elapsed = Date().timeIntervalSince(splashStartedAt)
        let remaining = max(0, minimumSplashDuration - elapsed)

        DispatchQueue.main.asyncAfter(deadline: .now() + remaining) {
            withAnimation(.easeOut(duration: 0.2)) {
                isSplashVisible = false
            }
        }
    }
}

private struct MatchSplashView: View {
    private let backgroundColor = Color(red: 79 / 255, green: 129 / 255, blue: 76 / 255)

    var body: some View {
        ZStack {
            backgroundColor

            Image("SplashIcon")
                .resizable()
                .scaledToFit()
                .frame(width: 132, height: 132)
                .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        }
    }
}
