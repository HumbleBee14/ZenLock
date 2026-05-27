import SwiftUI
import SwiftData
import FamilyControls

@main
struct ZenLockApp: App {
    @Environment(\.scenePhase) private var scenePhase
    @State private var screenTimeManager = ScreenTimeManager()
    @State private var blockingService = BlockingService()
    @State private var router = DeepLinkRouter()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(screenTimeManager)
                .environment(blockingService)
                .environment(router)
                .onOpenURL { url in router.handle(url) }
        }
        .modelContainer(for: [BlockGroup.self, FocusSession.self])
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                screenTimeManager.refreshStatus()
                reevaluateActiveGroups()
            }
        }
    }

    private func reevaluateActiveGroups() {
        guard let container = try? ModelContainer(for: BlockGroup.self, FocusSession.self) else { return }
        let context = ModelContext(container)
        let descriptor = FetchDescriptor<BlockGroup>()
        guard let groups = try? context.fetch(descriptor) else { return }
        blockingService.evaluateActiveGroups(groups)
    }
}
