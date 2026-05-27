import SwiftUI
import FamilyControls

struct ContentView: View {
    @State private var hasCompletedOnboarding = AppGroupStorage().bool(forKey: Constants.Keys.onboardingCompleted)
    @Environment(ScreenTimeManager.self) private var screenTimeManager

    var body: some View {
        Group {
            if hasCompletedOnboarding && screenTimeManager.isAuthorized {
                MainTabs()
            } else {
                OnboardingView {
                    withAnimation(ZenTheme.smooth) {
                        hasCompletedOnboarding = true
                    }
                }
            }
        }
        .preferredColorScheme(.dark)
    }
}

private struct MainTabs: View {
    var body: some View {
        TabView {
            DashboardView()
                .tabItem { Label("Focus", systemImage: "shield.lefthalf.filled") }

            NavigationStack { AnalyticsView() }
                .tabItem { Label("Insights", systemImage: "chart.bar.fill") }
        }
        .tint(ZenTheme.primary)
    }
}
