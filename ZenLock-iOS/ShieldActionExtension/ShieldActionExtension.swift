import ManagedSettings
import FamilyControls
import UserNotifications

class ShieldActionExtension: ShieldActionDelegate {

    private let defaults = UserDefaults(suiteName: Constants.appGroupID)

    override func handle(
        action: ShieldAction,
        for application: ApplicationToken,
        completionHandler: @escaping (ShieldActionResponse) -> Void
    ) {
        let group = resolveGroup { selection in selection.applicationTokens.contains(application) }
        handle(action: action, group: group, completionHandler: completionHandler)
    }

    override func handle(
        action: ShieldAction,
        for webDomain: WebDomainToken,
        completionHandler: @escaping (ShieldActionResponse) -> Void
    ) {
        let group = resolveGroup { selection in selection.webDomainTokens.contains(webDomain) }
        handle(action: action, group: group, completionHandler: completionHandler)
    }

    override func handle(
        action: ShieldAction,
        for category: ActivityCategoryToken,
        completionHandler: @escaping (ShieldActionResponse) -> Void
    ) {
        let group = resolveGroup { selection in selection.categoryTokens.contains(category) }
        handle(action: action, group: group, completionHandler: completionHandler)
    }

    // MARK: - Core handler

    private func handle(
        action: ShieldAction,
        group: SharedBlockGroup?,
        completionHandler: @escaping (ShieldActionResponse) -> Void
    ) {
        switch action {
        case .primaryButtonPressed:
            completionHandler(.close)

        case .secondaryButtonPressed:
            if let group, group.deepFocusEnabled {
                completionHandler(.close)
                return
            }

            if let group, group.blockMode == .frictionBased {
                bumpOpenCount(for: group.id)
                grantFrictionBypass(for: group.id, seconds: 60)
                completionHandler(.close)
            } else {
                requestUnlock(groupName: group?.name)
                completionHandler(.defer)
            }

        @unknown default:
            completionHandler(.close)
        }
    }

    // MARK: - Helpers

    private func resolveGroup(matching predicate: (FamilyActivitySelection) -> Bool) -> SharedBlockGroup? {
        let groups = loadGroups().filter(\.isActive)
        for group in groups {
            guard let data = defaults?.data(forKey: Constants.Keys.selectionPrefix + group.id),
                  let selection = try? JSONDecoder().decode(FamilyActivitySelection.self, from: data) else { continue }
            if predicate(selection) { return group }
        }
        return groups.first
    }

    private func loadGroups() -> [SharedBlockGroup] {
        guard let data = defaults?.data(forKey: Constants.Keys.blockGroups),
              let groups = try? JSONDecoder().decode([SharedBlockGroup].self, from: data) else {
            return []
        }
        return groups
    }

    private func bumpOpenCount(for groupId: String) {
        let key = Constants.Keys.openCountPrefix + groupId
        let count = (defaults?.integer(forKey: key) ?? 0) + 1
        defaults?.set(count, forKey: key)
    }

    /// Temporarily lift shield; re-applied on next monitor tick or foreground.
    private func grantFrictionBypass(for groupId: String, seconds: Int) {
        let expiry = Date().addingTimeInterval(TimeInterval(seconds))
        defaults?.set(expiry, forKey: "friction_bypass_until_\(groupId)")
        ManagedSettingsStore(named: .init(groupId)).clearAllSettings()
    }

    private func requestUnlock(groupName: String?) {
        defaults?.set(true, forKey: "zen_unlock_requested")
        if let groupName {
            defaults?.set(groupName, forKey: "zen_unlock_requested_group")
        }

        let content = UNMutableNotificationContent()
        content.title = "🔓 Unlock Requested"
        content.body = groupName.map { "Open ZenLock to manage \($0)." } ?? "Open ZenLock to manage your blocking session."
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: "unlock_request_\(UUID().uuidString)",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request)
    }
}
