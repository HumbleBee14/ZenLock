import Foundation
import ManagedSettings
import FamilyControls

/// Global web filter independent of BlockGroups (always-on adult/domain blocking).
final class WebFilterManager {
    static var globalStoreName: ManagedSettingsStore.Name { .init("zen_global_web") }
    static let selectionKey = "zen_global_web_selection"
    static let adultFilterKey = "zen_global_adult_filter"

    private let defaults: UserDefaults?

    init(defaults: UserDefaults? = Constants.sharedDefaults) {
        self.defaults = defaults
    }

    var adultFilterEnabled: Bool {
        get { defaults?.bool(forKey: Self.adultFilterKey) ?? false }
        set { defaults?.set(newValue, forKey: Self.adultFilterKey) }
    }

    func loadSelection() -> FamilyActivitySelection {
        guard let data = defaults?.data(forKey: Self.selectionKey),
              let selection = try? JSONDecoder().decode(FamilyActivitySelection.self, from: data) else {
            return FamilyActivitySelection()
        }
        return selection
    }

    func saveSelection(_ selection: FamilyActivitySelection) {
        guard let data = try? JSONEncoder().encode(selection) else { return }
        defaults?.set(data, forKey: Self.selectionKey)
    }

    func apply(selection: FamilyActivitySelection, blockAdultContent: Bool) {
        var store = ManagedSettingsStore(named: Self.globalStoreName)
        store.clearAllSettings()
        store = ManagedSettingsStore(named: Self.globalStoreName)

        if !selection.webDomainTokens.isEmpty {
            store.shield.webDomains = selection.webDomainTokens
        }
        if blockAdultContent {
            store.webContent.blockedByFilter = .auto()
        }

        saveSelection(selection)
        adultFilterEnabled = blockAdultContent
    }

    func clear() {
        let store = ManagedSettingsStore(named: Self.globalStoreName)
        store.clearAllSettings()
        defaults?.removeObject(forKey: Self.selectionKey)
        adultFilterEnabled = false
    }
}
