import Foundation

protocol EntitlementProviding {
    var tier: UserTier { get }
    func canUse(_ feature: PremiumFeature) -> Bool
}

final class EntitlementManager: EntitlementProviding {
    var tier: UserTier { .free }
    func canUse(_ feature: PremiumFeature) -> Bool { true }
}
