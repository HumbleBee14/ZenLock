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

    static let empty = ScreenTimeSnapshot(totalDuration: 0, rows: [], capturedAt: .distantPast)

    private static var fileURL: URL? {
        FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: Constants.appGroupID)?
            .appendingPathComponent("screen_time_snapshot.json")
    }

    static func save(_ snapshot: ScreenTimeSnapshot) {
        guard let url = fileURL,
              let data = try? JSONEncoder().encode(snapshot) else { return }
        try? data.write(to: url, options: .atomic)
    }

    static func load() -> ScreenTimeSnapshot? {
        guard let url = fileURL,
              let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode(ScreenTimeSnapshot.self, from: data)
    }
}
