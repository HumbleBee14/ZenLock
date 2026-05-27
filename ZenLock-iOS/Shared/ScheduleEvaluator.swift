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
        guard let hour = comps.hour, let minute = comps.minute else { return false }

        if let allowedDays = group.scheduleDaysOfWeek, !allowedDays.isEmpty {
            if let weekday = comps.weekday, !allowedDays.contains(weekday) {
                let crossesMidnight = startHour > endHour || (startHour == endHour && startMin > endMin)
                if crossesMidnight && isInPostMidnightTail(hour: hour, minute: minute, endHour: endHour, endMin: endMin) {
                    let prevWeekday = ((weekday - 2 + 7) % 7) + 1
                    if !allowedDays.contains(prevWeekday) { return false }
                } else {
                    return false
                }
            }
        }

        let nowMinutes = hour * 60 + minute
        let startMinutes = startHour * 60 + startMin
        let endMinutes = endHour * 60 + endMin

        let crossesMidnight = startMinutes > endMinutes

        if crossesMidnight {
            return nowMinutes >= startMinutes || nowMinutes < endMinutes
        } else {
            return nowMinutes >= startMinutes && nowMinutes < endMinutes
        }
    }

    private static func isInPostMidnightTail(hour: Int, minute: Int, endHour: Int, endMin: Int) -> Bool {
        let now = hour * 60 + minute
        let end = endHour * 60 + endMin
        return now < end
    }
}
