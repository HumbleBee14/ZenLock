import Foundation

enum ScheduleEvaluator {

    /// Check if date is within group's schedule, accounting for day-of-week and cross-midnight wraps.
    static func isWithinSchedule(_ group: SharedBlockGroup, at date: Date = Date(), calendar: Calendar = .current) -> Bool {
        guard group.blockMode == .timeBased else { return true }

        guard let startHour = group.scheduleStartHour,
              let startMin = group.scheduleStartMinute,
              let endHour = group.scheduleEndHour,
              let endMin = group.scheduleEndMinute else {
            return false
        }

        let comps = calendar.dateComponents([.hour, .minute, .weekday], from: date)
        guard let hour = comps.hour, let minute = comps.minute, let weekday = comps.weekday else { return false }

        let nowMinutes = hour * 60 + minute
        let startMinutes = startHour * 60 + startMin
        let endMinutes = endHour * 60 + endMin
        let crossesMidnight = startMinutes > endMinutes

        let inWindow = crossesMidnight
            ? (nowMinutes >= startMinutes || nowMinutes < endMinutes)
            : (nowMinutes >= startMinutes && nowMinutes < endMinutes)
        guard inWindow else { return false }

        if let allowedDays = group.scheduleDaysOfWeek, !allowedDays.isEmpty {
            let inPostMidnightTail = crossesMidnight && nowMinutes < endMinutes
            let windowStartWeekday = inPostMidnightTail ? ((weekday - 2 + 7) % 7) + 1 : weekday
            return allowedDays.contains(windowStartWeekday)
        }
        return true
    }
}
