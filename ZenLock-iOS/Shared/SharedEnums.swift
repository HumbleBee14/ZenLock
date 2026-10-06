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
    /// DeviceActivity's 15-minute minimum concerns the schedule, not the usage
    /// threshold. Keep UI, saved drafts and registration consistent.
    var limitOptions: [Int] {
        switch self {
        case .hourly: return Array(1...59)
        case .daily: return Array(stride(from: 10, through: 720, by: 10))
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
