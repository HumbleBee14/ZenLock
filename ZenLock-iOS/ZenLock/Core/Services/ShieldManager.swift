import Foundation
import ManagedSettings
import FamilyControls

protocol ShieldManaging {
    func applyShield(for group: SharedBlockGroup, selection: FamilyActivitySelection)
    func removeShield(forGroupId id: String)
    func removeAllShields(groupIds: [String])
}

final class ShieldManager: ShieldManaging {
    func applyShield(for group: SharedBlockGroup, selection: FamilyActivitySelection) {
        let storeName = ManagedSettingsStore.Name(group.id)
        var store = ManagedSettingsStore(named: storeName)
        store.clearAllSettings()
        store = ManagedSettingsStore(named: storeName)

        if !selection.applicationTokens.isEmpty {
            store.shield.applications = selection.applicationTokens
        }
        if !selection.categoryTokens.isEmpty {
            store.shield.applicationCategories = .specific(selection.categoryTokens)
        }
        if group.webFilterEnabled, !selection.webDomainTokens.isEmpty {
            store.shield.webDomains = selection.webDomainTokens
        }
        if group.blockAdultContent {
            store.webContent.blockedByFilter = .auto()
        }
    }

    func removeShield(forGroupId id: String) {
        let store = ManagedSettingsStore(named: .init(id))
        store.clearAllSettings()
    }

    func removeAllShields(groupIds: [String]) {
        groupIds.forEach { removeShield(forGroupId: $0) }
    }
}
