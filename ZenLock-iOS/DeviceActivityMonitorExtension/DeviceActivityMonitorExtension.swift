import Foundation
import DeviceActivity
import ManagedSettings
import FamilyControls
import UserNotifications

class DeviceActivityMonitorExtension: DeviceActivityMonitor {

    private let defaults = UserDefaults(suiteName: "group.com.grepguru.zenlock")

    override func intervalDidStart(for activity: DeviceActivityName) {
        evaluateBlockState(for: activity)
    }

    override func intervalDidEnd(for activity: DeviceActivityName) {
        evaluateBlockState(for: activity)
    }

    override func eventDidReachThreshold(
        _ event: DeviceActivityEvent.Name,
        activity: DeviceActivityName
    ) {
        evaluateBlockState(for: activity, thresholdReached: true)
    }

    override func eventWillReachThresholdWarning(
        _ event: DeviceActivityEvent.Name,
        activity: DeviceActivityName
    ) {
        let groupId = extractGroupId(from: activity)
        let groupName = loadGroupName(for: groupId)

        let content = UNMutableNotificationContent()
        content.title = "⏳ Almost at your limit"
        content.body = "\(groupName) is approaching its time limit."
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: "threshold_warning_\(groupId)",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request)
    }

    private func evaluateBlockState(
        for activity: DeviceActivityName,
        thresholdReached: Bool = false
    ) {
        let groupId = extractGroupId(from: activity)

        if thresholdReached, isPrematureThreshold(for: groupId) {
            return
        }

        let isActive = defaults?.bool(forKey: "zen_active_\(groupId)") ?? false

        let storeName = ManagedSettingsStore.Name(activity.rawValue)
        if isActive {
            applyShield(storeName: storeName, groupId: groupId)
        } else {
            ManagedSettingsStore(named: storeName).clearAllSettings()
        }
    }

    private func applyShield(storeName: ManagedSettingsStore.Name, groupId: String) {
        var store = ManagedSettingsStore(named: storeName)
        store.clearAllSettings()
        store = ManagedSettingsStore(named: storeName)

        guard let selectionData = defaults?.data(forKey: "zen_selection_\(groupId)"),
              let selection = try? JSONDecoder().decode(FamilyActivitySelection.self, from: selectionData) else {
            return
        }

        if !selection.applicationTokens.isEmpty {
            store.shield.applications = selection.applicationTokens
        }
        if !selection.categoryTokens.isEmpty {
            store.shield.applicationCategories = .specific(selection.categoryTokens)
        }
    }

    private func isPrematureThreshold(for groupId: String) -> Bool {
        guard let startTime = defaults?.object(forKey: "schedule_start_\(groupId)") as? Date else {
            return false
        }
        return Date().timeIntervalSince(startTime) < 60
    }

    private func extractGroupId(from activity: DeviceActivityName) -> String {
        let raw = activity.rawValue
        if raw.hasSuffix("-A") || raw.hasSuffix("-B") {
            return String(raw.dropLast(2))
        }
        return raw
    }

    private func loadGroupName(for groupId: String) -> String {
        guard let data = defaults?.data(forKey: "zen_block_groups"),
              let groups = try? JSONDecoder().decode([SharedBlockGroup].self, from: data) else {
            return "ZenLock"
        }
        return groups.first(where: { $0.id == groupId })?.name ?? "ZenLock"
    }
}
