import Foundation

enum BlockMode: String, Codable, CaseIterable, Sendable {
    case timeBased
    case usageBased
}

enum UsagePeriod: String, Codable, Sendable {
    case hourly
    case daily
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
