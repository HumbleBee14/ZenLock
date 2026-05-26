import Foundation

enum BlockMode: String, Codable, CaseIterable, Sendable {
    case timeBased
    case usageBased
    case frictionBased
}

enum UsagePeriod: String, Codable, Sendable {
    case hourly
    case daily
}

enum FrictionType: String, Codable, Sendable {
    case breathing
    case delay
    case question
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
    case accountability
    case widget
    case deepFocus
}
