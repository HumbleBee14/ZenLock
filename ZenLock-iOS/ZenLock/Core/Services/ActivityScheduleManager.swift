import Foundation
import DeviceActivity
import FamilyControls

protocol ActivityScheduleManaging {
    func startMonitoring(for group: SharedBlockGroup, selection: FamilyActivitySelection) throws
    func ensureUsageMonitoring(for group: SharedBlockGroup, selection: FamilyActivitySelection) throws
    func stopMonitoring(forGroupId id: String)
    func stopAllMonitoring()
}

final class ActivityScheduleManager: ActivityScheduleManaging {
    private let center = DeviceActivityCenter()
    private let notifier = ScheduleNotifier()
    private let storage = AppGroupStorage()

    private static let usageRefreshInterval: TimeInterval = 86_400

    private static func usageRegisteredKey(_ id: String) -> String { "zen_usage_registered_\(id)" }

    func startMonitoring(for group: SharedBlockGroup, selection: FamilyActivitySelection) throws {
        switch group.blockMode {
        case .timeBased:
            try startTimeBasedMonitoring(for: group)
            scheduleStartBackstop(for: group)
        case .usageBased:
            try startUsageBasedMonitoring(for: group, selection: selection)
        }
    }

    func ensureUsageMonitoring(for group: SharedBlockGroup, selection: FamilyActivitySelection) throws {
        let registered = center.activities.contains(DeviceActivityName(group.id))
        guard !registered || usageRegistrationIsStale(group.id) else { return }
        try startMonitoring(for: group, selection: selection)
    }

    /// A registration can stop delivering callbacks while still appearing active,
    /// so refresh it daily. Safe only where past activity is counted on re-register.
    private func usageRegistrationIsStale(_ id: String) -> Bool {
        guard #available(iOS 17.4, *) else { return false }
        guard let last = storage.date(forKey: Self.usageRegisteredKey(id)) else { return true }
        return Date().timeIntervalSince(last) >= Self.usageRefreshInterval
    }

    func stopMonitoring(forGroupId id: String) {
        center.stopMonitoring([
            DeviceActivityName(id),
            DeviceActivityName("\(id)-A"),
            DeviceActivityName("\(id)-B")
        ])
        notifier.cancelStartNotification(groupId: id)
        storage.removeValue(forKey: Self.usageRegisteredKey(id))
    }

    private func scheduleStartBackstop(for group: SharedBlockGroup) {
        notifier.cancelStartNotification(groupId: group.id)
        guard group.notifyBeforeStart,
              let h = group.scheduleStartHour, let m = group.scheduleStartMinute else { return }
        notifier.scheduleHeadsUpNotification(
            groupId: group.id,
            groupName: group.name,
            hour: h,
            minute: m,
            repeats: group.scheduleRepeats
        )
    }

    func stopAllMonitoring() {
        center.stopMonitoring()
    }

    private func startTimeBasedMonitoring(for group: SharedBlockGroup) throws {
        guard let startHour = group.scheduleStartHour,
              let startMin = group.scheduleStartMinute,
              let endHour = group.scheduleEndHour,
              let endMin = group.scheduleEndMinute else { return }

        let crossesMidnight = startHour > endHour || (startHour == endHour && startMin > endMin)

        if crossesMidnight {
            let scheduleA = DeviceActivitySchedule(
                intervalStart: DateComponents(hour: startHour, minute: startMin),
                intervalEnd: DateComponents(hour: 23, minute: 59),
                repeats: group.scheduleRepeats
            )
            let scheduleB = DeviceActivitySchedule(
                intervalStart: DateComponents(hour: 0, minute: 0),
                intervalEnd: DateComponents(hour: endHour, minute: endMin),
                repeats: group.scheduleRepeats
            )
            try center.startMonitoring(DeviceActivityName("\(group.id)-A"), during: scheduleA)
            try center.startMonitoring(DeviceActivityName("\(group.id)-B"), during: scheduleB)
        } else {
            let schedule = DeviceActivitySchedule(
                intervalStart: DateComponents(hour: startHour, minute: startMin),
                intervalEnd: DateComponents(hour: endHour, minute: endMin),
                repeats: group.scheduleRepeats
            )
            try center.startMonitoring(DeviceActivityName(group.id), during: schedule)
        }
    }

    private func startUsageBasedMonitoring(for group: SharedBlockGroup, selection: FamilyActivitySelection) throws {
        guard let limitMinutes = group.usageLimitMinutes,
              (group.usagePeriod ?? .daily).limitOptions.contains(limitMinutes) else {
            throw ActivationError.invalidUsageLimit
        }
        guard !selection.applicationTokens.isEmpty || !selection.categoryTokens.isEmpty else {
            throw ActivationError.noAppsSelected
        }

        let schedule: DeviceActivitySchedule
        switch group.usagePeriod ?? .daily {
        case .hourly:
            schedule = DeviceActivitySchedule(
                intervalStart: DateComponents(minute: 0, second: 0),
                intervalEnd: DateComponents(minute: 59, second: 59),
                repeats: true,
                warningTime: DateComponents(minute: 5)
            )
        case .daily:
            schedule = DeviceActivitySchedule(
                intervalStart: DateComponents(hour: 0, minute: 0, second: 0),
                intervalEnd: DateComponents(hour: 23, minute: 59, second: 59),
                repeats: true,
                warningTime: DateComponents(minute: 5)
            )
        }

        let usageEvent: DeviceActivityEvent
        if #available(iOS 17.4, *) {
            usageEvent = DeviceActivityEvent(
                applications: selection.applicationTokens,
                categories: selection.categoryTokens,
                threshold: DateComponents(minute: limitMinutes),
                includesPastActivity: true
            )
        } else {
            // iOS 17.0–17.3 only counts usage after registration.
            usageEvent = DeviceActivityEvent(
                applications: selection.applicationTokens,
                categories: selection.categoryTokens,
                threshold: DateComponents(minute: limitMinutes)
            )
        }

        try center.startMonitoring(
            DeviceActivityName(group.id),
            during: schedule,
            events: [DeviceActivityEvent.Name("usage_limit_\(group.id)"): usageEvent]
        )
        storage.setDate(Date(), forKey: Self.usageRegisteredKey(group.id))
    }

}
