import SwiftUI
import DeviceActivity

struct ScreenTimeView: View {
    @AppStorage(Constants.Keys.dailyGoalMinutes, store: Constants.sharedDefaults)
    private var dailyGoalMinutes: Int = 180

    @Environment(\.scenePhase) private var scenePhase
    @State private var filterEnd: Date = Date()
    @State private var reportID: UUID = UUID()
    @State private var hasCachedData: Bool = ScreenTimeSnapshot.load() != nil
    @State private var loaderVisible: Bool = ScreenTimeSnapshot.load() == nil

    var body: some View {
        ZStack {
            ZenTheme.background.ignoresSafeArea()

            DeviceActivityReport(
                DeviceActivityReport.Context("perApp"),
                filter: todayFilter
            )
            .id(reportID)

            if loaderVisible && !hasCachedData {
                VStack(spacing: 12) {
                    ProgressView().tint(ZenTheme.primary)
                    Text("Fetching today's usage…")
                        .font(.callout)
                        .foregroundStyle(ZenTheme.textSecondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(ZenTheme.background)
            }
        }
        .navigationTitle("Today's Screen Time")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            checkCacheAndLoad()
        }
        .onChange(of: dailyGoalMinutes) { _, _ in
            forceReload()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { checkCacheAndLoad() }
        }
        .onReceive(NotificationCenter.default.publisher(for: .screenTimeSnapshotUpdated)) { _ in
            if ScreenTimeSnapshot.load() != nil {
                hasCachedData = true
                loaderVisible = false
            }
        }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    forceReload()
                } label: {
                    Image(systemName: "arrow.clockwise")
                }
                .tint(ZenTheme.primary)
            }
        }
        .task {
            DarwinNotificationBridge.shared.start()
        }
    }

    private func checkCacheAndLoad() {
        if ScreenTimeSnapshot.load() != nil {
            hasCachedData = true
            loaderVisible = false
        } else {
            loaderVisible = true
        }

        if !hasCachedData {
            Task {
                try? await Task.sleep(nanoseconds: 5_000_000_000)
                await MainActor.run {
                    if ScreenTimeSnapshot.load() != nil {
                        hasCachedData = true
                    }
                    loaderVisible = false
                }
            }
        }
    }

    private func forceReload() {
        filterEnd = Date()
        reportID = UUID()
    }

    private var todayFilter: DeviceActivityFilter {
        let start = Calendar.current.startOfDay(for: Date())
        return DeviceActivityFilter(
            segment: .daily(during: DateInterval(start: start, end: filterEnd))
        )
    }
}

extension Notification.Name {
    static let screenTimeSnapshotUpdated = Notification.Name("zen.screenTimeSnapshotUpdated")
}

final class DarwinNotificationBridge: @unchecked Sendable {
    static let shared = DarwinNotificationBridge()
    private var started = false

    func start() {
        guard !started else { return }
        started = true
        let name = "com.humblebee.zenlock.snapshot.updated" as CFString
        let center = CFNotificationCenterGetDarwinNotifyCenter()
        let observer = Unmanaged.passUnretained(self).toOpaque()
        CFNotificationCenterAddObserver(
            center, observer,
            { _, _, _, _, _ in
                NotificationCenter.default.post(name: .screenTimeSnapshotUpdated, object: nil)
            },
            name, nil, .deliverImmediately
        )
    }
}
