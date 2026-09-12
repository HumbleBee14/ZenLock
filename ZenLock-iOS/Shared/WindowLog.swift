import Foundation

enum WindowLog {
    private static let maxEntries = 120

    static func key(_ groupId: String) -> String { "zen_window_log_\(groupId)" }

    static func record(groupId: String, at date: Date = Date(), defaults: UserDefaults? = Constants.sharedDefaults) {
        var starts = self.starts(groupId: groupId, defaults: defaults)
        if let last = starts.last, abs(last.timeIntervalSince(date)) < 300 { return }
        starts.append(date)
        if starts.count > maxEntries { starts.removeFirst(starts.count - maxEntries) }
        defaults?.set(starts, forKey: key(groupId))
    }

    static func starts(groupId: String, defaults: UserDefaults? = Constants.sharedDefaults) -> [Date] {
        (defaults?.array(forKey: key(groupId)) as? [Date]) ?? []
    }

    static func clear(groupId: String, defaults: UserDefaults? = Constants.sharedDefaults) {
        defaults?.removeObject(forKey: key(groupId))
    }
}
