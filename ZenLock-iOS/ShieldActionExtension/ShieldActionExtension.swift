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

            requestUnlock()
            completionHandler(.close)

        default:
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
        return nil
    }

    private func loadGroups() -> [SharedBlockGroup] {
        guard let data = defaults?.data(forKey: Constants.Keys.blockGroups),
              let groups = try? JSONDecoder().decode([SharedBlockGroup].self, from: data) else {
            return []
        }
        return groups
    }

    private func requestUnlock() {
        let content = UNMutableNotificationContent()
        content.title = "ZenLock"
        content.body = "Open ZenLock to unlock."
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: "unlock_request",
            content: content,
            trigger: UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        )
        UNUserNotificationCenter.current().add(request)
    }
}
