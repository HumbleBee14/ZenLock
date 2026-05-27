import Foundation
import os.log

private let snapshotLog = Logger(subsystem: "com.humblebee.zenlock", category: "ScreenTimeSnapshot")

struct ScreenTimeSnapshot: Codable {
    struct Row: Codable, Identifiable {
        var id: String { bundleID ?? name }
        let bundleID: String?
        let name: String
        let duration: TimeInterval
    }

    let totalDuration: TimeInterval
    let rows: [Row]
    let capturedAt: Date

    static let empty = ScreenTimeSnapshot(totalDuration: 0, rows: [], capturedAt: .distantPast)
    static let defaultsKey = "zen_screen_time_snapshot_v2"

    static func save(_ snapshot: ScreenTimeSnapshot) {
        guard let defaults = UserDefaults(suiteName: Constants.appGroupID) else {
            snapshotLog.error("save: UserDefaults suite is nil")
            return
        }

        // Don't clobber a good cached snapshot with an empty one.
        // The extension is invoked multiple times concurrently; some calls fire
        // before activity data is loaded and return 0 rows. Only persist meaningful data.
        if snapshot.rows.isEmpty && snapshot.totalDuration == 0 {
            if let existing = defaults.data(forKey: defaultsKey),
               let prev = try? JSONDecoder().decode(ScreenTimeSnapshot.self, from: existing),
               (!prev.rows.isEmpty || prev.totalDuration > 0) {
                snapshotLog.notice("save: SKIPPED empty snapshot (preserving previous rows=\(prev.rows.count))")
                return
            }
        }

        guard let data = try? JSONEncoder().encode(snapshot) else {
            snapshotLog.error("save: encode failed")
            return
        }
        defaults.set(data, forKey: defaultsKey)
        snapshotLog.notice("save: OK bytes=\(data.count) rows=\(snapshot.rows.count)")

        // Cross-process notify so the main app's UserDefaults cache invalidates.
        CFNotificationCenterPostNotification(
            CFNotificationCenterGetDarwinNotifyCenter(),
            CFNotificationName("com.humblebee.zenlock.snapshot.updated" as CFString),
            nil, nil, true
        )
    }

    static func load() -> ScreenTimeSnapshot? {
        guard let defaults = UserDefaults(suiteName: Constants.appGroupID) else {
            snapshotLog.error("load: UserDefaults suite is nil")
            return nil
        }
        guard let data = defaults.data(forKey: defaultsKey) else {
            snapshotLog.notice("load: no data in UserDefaults for key \(defaultsKey, privacy: .public)")
            return nil
        }
        do {
            let snap = try JSONDecoder().decode(ScreenTimeSnapshot.self, from: data)
            snapshotLog.notice("load: OK rows=\(snap.rows.count) total=\(snap.totalDuration, format: .fixed(precision: 0))s")
            return snap
        } catch {
            snapshotLog.error("load: decode FAILED \(error.localizedDescription, privacy: .public)")
            return nil
        }
    }
}
