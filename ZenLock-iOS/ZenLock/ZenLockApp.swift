import SwiftUI
import SwiftData
import FamilyControls

@main
struct ZenLockApp: App {
    @State private var screenTimeManager = ScreenTimeManager()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(screenTimeManager)
                .onAppear { screenTimeManager.refreshStatus() }
        }
        .modelContainer(for: [BlockGroup.self, FocusSession.self])
    }
}
