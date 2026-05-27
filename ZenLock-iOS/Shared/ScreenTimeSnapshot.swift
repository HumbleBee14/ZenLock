import Foundation

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

    static let storageKey = "zen_screen_time_snapshot"

    static let empty = ScreenTimeSnapshot(totalDuration: 0, rows: [], capturedAt: .distantPast)

    static func save(_ snapshot: ScreenTimeSnapshot) {
        guard let defaults = UserDefaults(suiteName: Constants.appGroupID) else { return }
        if let data = try? JSONEncoder().encode(snapshot) {
            defaults.set(data, forKey: storageKey)
        }
    }

    static func load() -> ScreenTimeSnapshot? {
        guard
            let defaults = UserDefaults(suiteName: Constants.appGroupID),
            let data = defaults.data(forKey: storageKey)
        else { return nil }
        return try? JSONDecoder().decode(ScreenTimeSnapshot.self, from: data)
    }
}
