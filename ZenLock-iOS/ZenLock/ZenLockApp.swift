import SwiftUI
import SwiftData
import FamilyControls

@main
struct ZenLockApp: App {
    @Environment(\.scenePhase) private var scenePhase
    @State private var screenTimeManager = ScreenTimeManager()
    @State private var blockingService = BlockingService()
    @State private var router = DeepLinkRouter()
    @State private var notificationDelegate = ZenNotificationDelegate()
    @AppStorage(AppThemeStorage.key) private var themeRaw: String = AppTheme.system.rawValue

    private let container: ModelContainer = Self.makeContainer()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(screenTimeManager)
                .environment(blockingService)
                .environment(router)
                .preferredColorScheme(AppTheme(rawValue: themeRaw)?.colorScheme)
                .onOpenURL { url in router.handle(url) }
        }
        .modelContainer(container)
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                screenTimeManager.refreshStatus()
                reevaluateActiveGroups()
            }
        }
    }

    private func reevaluateActiveGroups() {
        let context = ModelContext(container)
        let descriptor = FetchDescriptor<BlockGroup>()
        guard let groups = try? context.fetch(descriptor) else { return }
        blockingService.evaluateActiveGroups(groups)
    }

    /// Open the SwiftData store; if it fails (typically a schema mismatch during dev),
    /// nuke the underlying files and retry once. Loses local groups but keeps the app launchable.
    private static func makeContainer() -> ModelContainer {
        let schema = Schema([BlockGroup.self, FocusSession.self])
        let config = ModelConfiguration(schema: schema)
        do {
            return try ModelContainer(for: schema, configurations: config)
        } catch {
            wipeStore(at: config.url)
            return (try? ModelContainer(for: schema, configurations: config))
                ?? (try! ModelContainer(for: schema))
        }
    }

    private static func wipeStore(at url: URL) {
        let fm = FileManager.default
        for suffix in ["", "-shm", "-wal"] {
            let path = URL(fileURLWithPath: url.path + suffix)
            try? fm.removeItem(at: path)
        }
    }
}
