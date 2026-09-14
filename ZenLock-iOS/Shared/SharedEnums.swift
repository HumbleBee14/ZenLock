import Foundation

enum BlockMode: String, Codable, CaseIterable, Sendable {
    case timeBased
    case usageBased
}

enum UsagePeriod: String, Codable, Sendable {
    case hourly
    case daily
}

extension UsagePeriod {
    /// Product limits; DeviceActivity's 15-minute minimum concerns the schedule,
    /// not the usage threshold. Keep UI, saved drafts and registration consistent.
    var limitOptions: [Int] {
        switch self {
        case .hourly: return Array(stride(from: 15, through: 50, by: 5))
        case .daily: return [15, 30, 45, 60] + Array(stride(from: 90, through: 720, by: 30))
        }
    }

    func normalizedLimit(_ minutes: Int) -> Int {
        // Clamp before subtraction to avoid overflow with malformed saved values.
        let clamped = min(max(minutes, limitOptions[0]), limitOptions.last!)
        return limitOptions.min { abs($0 - clamped) < abs($1 - clamped) }!
    }
}

enum UserTier: String, Codable, Sendable {
    case free
    case trial
    case premium
}

enum PremiumFeature: String, Codable, CaseIterable, Sendable {
    case unlimitedGroups
    case advancedAnalytics
    case customThemes
    case widget
    case deepFocus
}
