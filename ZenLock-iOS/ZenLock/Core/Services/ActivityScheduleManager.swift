import Foundation
import DeviceActivity
import FamilyControls

protocol ActivityScheduleManaging {
    func startMonitoring(for group: SharedBlockGroup, selection: FamilyActivitySelection) throws
    func stopMonitoring(forGroupId id: String)
    func stopAllMonitoring()
}

final class ActivityScheduleManager: ActivityScheduleManaging {
    private let center = DeviceActivityCenter()
    private let storage = AppGroupStorage()

    func startMonitoring(for group: SharedBlockGroup, selection: FamilyActivitySelection) throws {
        switch group.blockMode {
        case .timeBased:
            try startTimeBasedMonitoring(for: group)
        case .usageBased:
            try startUsageBasedMonitoring(for: group, selection: selection)
        }
        storage.setScheduleStartTime(Date(), forGroupId: group.id)
    }

    func stopMonitoring(forGroupId id: String) {
        center.stopMonitoring([
            DeviceActivityName(id),
            DeviceActivityName("\(id)-A"),
            DeviceActivityName("\(id)-B")
        ])
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
        guard let limitMinutes = group.usageLimitMinutes else { return }

        let schedule: DeviceActivitySchedule
        switch group.usagePeriod ?? .daily {
        case .hourly:
            schedule = DeviceActivitySchedule(
                intervalStart: DateComponents(minute: 0),
                intervalEnd: DateComponents(minute: 59),
                repeats: true
            )
        case .daily:
            schedule = DeviceActivitySchedule(
                intervalStart: DateComponents(hour: 0, minute: 0),
                intervalEnd: DateComponents(hour: 23, minute: 59),
                repeats: true
            )
        }

        let usageEvent = DeviceActivityEvent(
            applications: selection.applicationTokens,
            categories: selection.categoryTokens,
            threshold: DateComponents(minute: limitMinutes)
        )

        try center.startMonitoring(
            DeviceActivityName(group.id),
            during: schedule,
            events: [DeviceActivityEvent.Name("usage_limit_\(group.id)"): usageEvent]
        )
    }

}
