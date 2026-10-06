import Foundation
import DeviceActivity
import ManagedSettings
import FamilyControls
import UserNotifications

class DeviceActivityMonitorExtension: DeviceActivityMonitor {

    private let defaults = UserDefaults(suiteName: Constants.appGroupID)

    override func intervalDidStart(for activity: DeviceActivityName) {
        if CooldownRelease.isReleaseActivity(activity) {
            releaseCooldown(for: activity)
            return
        }
        guard activity.rawValue != Constants.quickFocusActivity else { return }
        ShieldExpiry.sweep(excluding: extractGroupId(from: activity), defaults: defaults)
        evaluateBlockState(for: activity, reason: .intervalStart)
        let groupId = extractGroupId(from: activity)
        if let group = loadGroup(groupId), group.blockMode == .timeBased, ScheduleEvaluator.isWithinSchedule(group) {
            WindowLog.record(groupId: groupId, defaults: defaults)
        }
    }

    override func intervalDidEnd(for activity: DeviceActivityName) {
        if CooldownRelease.isReleaseActivity(activity) {
            releaseCooldown(for: activity)
            return
        }
        if activity.rawValue == Constants.quickFocusActivity {
            ShieldExpiry.releaseQuickFocusIfStale(now: Date().addingTimeInterval(Self.endCallbackGrace), defaults: defaults)
            return
        }
        ShieldExpiry.sweep(excluding: extractGroupId(from: activity), defaults: defaults)
        evaluateBlockState(for: activity, reason: .intervalEnd)
    }

    override func eventDidReachThreshold(
        _ event: DeviceActivityEvent.Name,
        activity: DeviceActivityName
    ) {
        // A callback can be immediate when earlier usage already met the limit.
        // Dropping it does not cause DeviceActivity to send another callback.
        guard let group = loadGroup(activity.rawValue),
              group.blockMode == .usageBased,
              event.rawValue == "usage_limit_\(group.id)" else { return }
        ShieldExpiry.sweep(excluding: group.id, defaults: defaults)
        evaluateBlockState(for: activity, reason: .thresholdReached)
    }

    override func eventWillReachThresholdWarning(
        _ event: DeviceActivityEvent.Name,
        activity: DeviceActivityName
    ) {
        guard let group = loadGroup(activity.rawValue), group.isActive,
              group.blockMode == .usageBased,
              event.rawValue == "usage_limit_\(group.id)" else { return }
        let groupId = group.id
        let groupName = group.name

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

    private func releaseCooldown(for activity: DeviceActivityName) {
        if activity.rawValue == CooldownRelease.quickFocusActivity {
            CooldownRelease.releaseQuickFocusIfElapsed(defaults: defaults)
        } else {
            CooldownRelease.releaseGroupIfElapsed(defaults: defaults)
        }
    }

    // MARK: - Single-path evaluation

    private static let endCallbackGrace: TimeInterval = 5

    private enum EvalReason {
        case intervalStart, intervalEnd, thresholdReached
    }

    private func evaluateBlockState(for activity: DeviceActivityName, reason: EvalReason) {
        let groupId = extractGroupId(from: activity)
        let storeName = ManagedSettingsStore.Name(activity.rawValue)

        guard let group = loadGroup(groupId), group.isActive else {
            ManagedSettingsStore(named: storeName).clearAllSettings()
            ManagedSettingsStore(named: ManagedSettingsStore.Name(groupId)).clearAllSettings()
            return
        }

        let shouldBlock: Bool
        switch group.blockMode {
        case .timeBased:
            let probe = reason == .intervalEnd ? Date().addingTimeInterval(Self.endCallbackGrace) : Date()
            shouldBlock = ScheduleEvaluator.isWithinSchedule(group, at: probe)
        case .usageBased:
            let period = group.usagePeriod ?? .daily
            if reason == .thresholdReached {
                UsageBlockState.record(groupId, period: period, defaults: defaults)
            }
            shouldBlock = UsageBlockState.load(groupId, defaults: defaults)?.isBlocked(period: period) ?? false
        }

        if shouldBlock {
            applyShield(storeName: storeName, group: group)
        } else {
            ManagedSettingsStore(named: storeName).clearAllSettings()
            if group.blockMode == .timeBased, !ScheduleEvaluator.isWithinSchedule(group) {
                for name in [groupId, "\(groupId)-A", "\(groupId)-B"] {
                    ManagedSettingsStore(named: .init(name)).clearAllSettings()
                }
            }
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
