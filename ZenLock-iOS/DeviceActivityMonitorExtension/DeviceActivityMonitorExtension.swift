import Foundation
import DeviceActivity
import ManagedSettings
import FamilyControls
import UserNotifications

class DeviceActivityMonitorExtension: DeviceActivityMonitor {

    private let defaults = UserDefaults(suiteName: Constants.appGroupID)

    override func intervalDidStart(for activity: DeviceActivityName) {
        evaluateBlockState(for: activity, reason: .intervalStart)
    }

    override func intervalDidEnd(for activity: DeviceActivityName) {
        evaluateBlockState(for: activity, reason: .intervalEnd)
    }

    override func eventDidReachThreshold(
        _ event: DeviceActivityEvent.Name,
        activity: DeviceActivityName
    ) {
        evaluateBlockState(for: activity, reason: .thresholdReached)
    }

    override func eventWillReachThresholdWarning(
        _ event: DeviceActivityEvent.Name,
        activity: DeviceActivityName
    ) {
        let groupId = extractGroupId(from: activity)
        let groupName = loadGroup(groupId)?.name ?? "ZenLock"

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

    // MARK: - Single-path evaluation

    private enum EvalReason {
        case intervalStart, intervalEnd, thresholdReached
    }

    private func evaluateBlockState(for activity: DeviceActivityName, reason: EvalReason) {
        let groupId = extractGroupId(from: activity)
        let storeName = ManagedSettingsStore.Name(activity.rawValue)

        if reason == .thresholdReached, isPrematureThreshold(for: groupId) {
            return
        }

        guard let group = loadGroup(groupId), group.isActive else {
            ManagedSettingsStore(named: storeName).clearAllSettings()
            return
        }

        let shouldBlock: Bool
        switch group.blockMode {
        case .timeBased:
            shouldBlock = (reason != .intervalEnd) && ScheduleEvaluator.isWithinSchedule(group)
        case .usageBased:
            shouldBlock = reason == .thresholdReached
        case .frictionBased:
            shouldBlock = true
        }

        if shouldBlock {
            applyShield(storeName: storeName, group: group)
        } else {
            ManagedSettingsStore(named: storeName).clearAllSettings()
        }
    }

    private func applyShield(storeName: ManagedSettingsStore.Name, group: SharedBlockGroup) {
        var store = ManagedSettingsStore(named: storeName)
        store.clearAllSettings()
        store = ManagedSettingsStore(named: storeName)

        guard let selectionData = defaults?.data(forKey: Constants.Keys.selectionPrefix + group.id),
              let selection = try? JSONDecoder().decode(FamilyActivitySelection.self, from: selectionData) else {
            return
        }

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

    private func loadGroup(_ groupId: String) -> SharedBlockGroup? {
        guard let data = defaults?.data(forKey: Constants.Keys.blockGroups),
              let groups = try? JSONDecoder().decode([SharedBlockGroup].self, from: data) else {
            return nil
        }
        return groups.first(where: { $0.id == groupId })
    }
}
