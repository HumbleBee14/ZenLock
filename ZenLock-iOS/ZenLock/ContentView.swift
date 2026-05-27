import SwiftUI
import DeviceActivity
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
    }
}

/// Mounts an off-screen DeviceActivityReport at the app root so the extension
/// warms up and writes the cached snapshot file before the user ever opens
/// the Screen Time tab. Re-fires on scene-active to keep the cache fresh.
private struct ScreenTimePrefetcher: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var filterEnd: Date = Date()
    @State private var reportID: UUID = UUID()

    var body: some View {
        DeviceActivityReport(
            DeviceActivityReport.Context("perApp"),
            filter: filter
        )
        .id(reportID)
        .frame(width: 1, height: 1)
        .opacity(0.01)
        .allowsHitTesting(false)
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                filterEnd = Date()
                reportID = UUID()
            }
        }
    }

    private var filter: DeviceActivityFilter {
        let start = Calendar.current.startOfDay(for: Date())
        return DeviceActivityFilter(
            segment: .daily(during: DateInterval(start: start, end: filterEnd))
        )
    }
}

private struct MainTabs: View {
    var body: some View {
        TabView {
            DashboardView()
                .tabItem { Label("Focus", systemImage: "shield.lefthalf.filled") }

            NavigationStack { ScreenTimeView() }
                .tabItem { Label("Screen Time", systemImage: "hourglass") }

            NavigationStack { AnalyticsView() }
                .tabItem { Label("Insights", systemImage: "chart.bar.fill") }
        }
        .tint(ZenTheme.primary)
    }
}
