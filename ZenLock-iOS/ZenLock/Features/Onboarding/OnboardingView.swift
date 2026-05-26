import SwiftUI

struct OnboardingView: View {
    @State private var currentPage = 0
    @State private var screenTimeManager = ScreenTimeManager()
    @State private var isAuthorizing = false
    @State private var authError: String?
    var onComplete: () -> Void

    var body: some View {
        ZStack {
            ZenTheme.background.ignoresSafeArea()

            VStack(spacing: 0) {
                TabView(selection: $currentPage) {
                    welcomePage.tag(0)
                    featuresPage.tag(1)
                    authorizationPage.tag(2)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .animation(ZenTheme.smooth, value: currentPage)

                pageIndicator
                    .padding(.bottom, ZenTheme.Spacing.xl)
            }
        }
    }

    private var welcomePage: some View {
        VStack(spacing: ZenTheme.Spacing.xl) {
            Spacer()

            ZStack {
                Circle()
                    .fill(ZenTheme.primaryGradient)
                    .frame(width: 120, height: 120)
                Image(systemName: "shield.checkered")
                    .font(.system(size: 52))
                    .foregroundStyle(.white)
            }

            VStack(spacing: ZenTheme.Spacing.md) {
                Text("ZenLock")
                    .font(ZenTheme.largeTitle)
                    .foregroundStyle(ZenTheme.text)
                Text("Take control of your screen time.\nStay focused. Stay present.")
                    .font(ZenTheme.body)
                    .foregroundStyle(ZenTheme.textSecondary)
                    .multilineTextAlignment(.center)
            }

            Spacer()

            ZenButton(title: "Get Started", icon: "arrow.right") {
                withAnimation { currentPage = 1 }
            }
            .padding(.horizontal, ZenTheme.Spacing.xl)
        }
        .padding(ZenTheme.Spacing.lg)
    }

    private var featuresPage: some View {
        VStack(spacing: ZenTheme.Spacing.xl) {
            Spacer()

            VStack(spacing: ZenTheme.Spacing.lg) {
                featureRow(icon: "shield.fill", title: "App Blocking", subtitle: "Block distracting apps with system-level enforcement", color: ZenTheme.primary)
                featureRow(icon: "clock.fill", title: "Scheduled Focus", subtitle: "Set automatic blocking schedules", color: ZenTheme.accent)
                featureRow(icon: "hand.raised.fill", title: "Mindful Friction", subtitle: "Breathing exercises before opening apps", color: ZenTheme.warning)
                featureRow(icon: "chart.bar.fill", title: "Usage Limits", subtitle: "Set daily or hourly usage thresholds", color: ZenTheme.success)
            }

            Spacer()

            ZenButton(title: "Continue", icon: "arrow.right") {
                withAnimation { currentPage = 2 }
            }
            .padding(.horizontal, ZenTheme.Spacing.xl)
        }
        .padding(ZenTheme.Spacing.lg)
    }

    private var authorizationPage: some View {
        VStack(spacing: ZenTheme.Spacing.xl) {
            Spacer()

            ZStack {
                Circle()
                    .fill(ZenTheme.accent.opacity(0.15))
                    .frame(width: 120, height: 120)
                Image(systemName: "lock.shield")
                    .font(.system(size: 52))
                    .foregroundStyle(ZenTheme.accent)
            }

            VStack(spacing: ZenTheme.Spacing.md) {
                Text("Screen Time Access")
                    .font(ZenTheme.title)
                    .foregroundStyle(ZenTheme.text)
                Text("ZenLock needs Screen Time permission to block apps. This data never leaves your device.")
                    .font(ZenTheme.body)
                    .foregroundStyle(ZenTheme.textSecondary)
                    .multilineTextAlignment(.center)
            }

            if let error = authError {
                Text(error)
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.error)
                    .multilineTextAlignment(.center)
            }

            Spacer()

            ZenButton(title: isAuthorizing ? "Requesting..." : "Authorize", icon: "checkmark.shield") {
                authorize()
            }
            .disabled(isAuthorizing)
            .padding(.horizontal, ZenTheme.Spacing.xl)
        }
        .padding(ZenTheme.Spacing.lg)
    }

    private func featureRow(icon: String, title: String, subtitle: String, color: Color) -> some View {
        HStack(spacing: ZenTheme.Spacing.md) {
            GroupIcon(systemName: icon, color: color, size: 48)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)
                Text(subtitle)
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
            }
            Spacer()
        }
        .padding(.horizontal, ZenTheme.Spacing.md)
    }

    private var pageIndicator: some View {
        HStack(spacing: ZenTheme.Spacing.sm) {
            ForEach(0..<3) { index in
                Capsule()
                    .fill(index == currentPage ? ZenTheme.primary : ZenTheme.surfaceLight)
                    .frame(width: index == currentPage ? 24 : 8, height: 8)
                    .animation(ZenTheme.springy, value: currentPage)
            }
        }
    }

    private func authorize() {
        isAuthorizing = true
        authError = nil
        Task {
            do {
                try await screenTimeManager.requestAuthorization()
                _ = await NotificationManager.shared.requestPermission()
                AppGroupStorage().setBool(true, forKey: Constants.Keys.onboardingCompleted)
                await MainActor.run { onComplete() }
            } catch {
                await MainActor.run {
                    authError = "Authorization failed. Please try again."
                    isAuthorizing = false
                }
            }
        }
    }
}
