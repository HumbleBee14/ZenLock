import Foundation
import ManagedSettings

enum ShieldTokenMatch {
    static func quickFocusShields(appToken: ApplicationToken?, categoryToken: ActivityCategoryToken?) -> Bool {
        let shield = ManagedSettingsStore(named: .init(Constants.quickFocusActivity)).shield
        if let appToken, shield.applications?.contains(appToken) == true { return true }
        if let categoryToken, case .specific(let tokens, _)? = shield.applicationCategories, tokens.contains(categoryToken) {
            return true
        }
        return false
    }
}
