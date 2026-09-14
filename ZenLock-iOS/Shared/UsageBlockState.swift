import Foundation

/// Persists a reached threshold until its calendar period ends. Callback order
/// must not grant another allowance within a period that already reached its cap.
struct UsageBlockState: Codable {
    let period: UsagePeriod
    let start: Date
    let end: Date

    static func key(_ groupId: String) -> String { "zen_usage_block_\(groupId)" }

    static func load(_ groupId: String, defaults: UserDefaults? = Constants.sharedDefaults) -> Self? {
        guard let data = defaults?.data(forKey: key(groupId)) else { return nil }
        return try? JSONDecoder().decode(Self.self, from: data)
    }

    static func record(_ groupId: String, period: UsagePeriod, at date: Date = Date(),
                       calendar: Calendar = .current, defaults: UserDefaults? = Constants.sharedDefaults) {
        // Registration ends at :59:59. Match that instant so intervalDidEnd
        // can release the shield without waiting for the next start callback.
        guard let interval = calendar.dateInterval(of: period == .hourly ? .hour : .day, for: date),
              let data = try? JSONEncoder().encode(Self(period: period, start: interval.start, end: interval.end.addingTimeInterval(-1))) else { return }
        defaults?.set(data, forKey: key(groupId))
    }

    func isBlocked(period: UsagePeriod, at date: Date = Date()) -> Bool {
        self.period == period && date >= start && date < end
    }

    static func clear(_ groupId: String, defaults: UserDefaults? = Constants.sharedDefaults) {
        defaults?.removeObject(forKey: key(groupId))
    }
}
