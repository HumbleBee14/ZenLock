import SwiftUI
import FamilyControls

struct ContentView: View {
    @State private var hasCompletedOnboarding = AppGroupStorage().bool(forKey: Constants.Keys.onboardingCompleted)
    @Environment(ScreenTimeManager.self) private var screenTimeManager

    var body: some View {
        Group {
            if hasCompletedOnboarding && screenTimeManager.isAuthorized {
                DashboardView()
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
