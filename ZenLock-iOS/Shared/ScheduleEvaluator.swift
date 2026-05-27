import Foundation

enum ScheduleEvaluator {

    /// Returns true if `date` falls inside the group's schedule window AND the day-of-week filter,
    /// taking cross-midnight wrap into account.
    /// For non-time-based modes, returns true unconditionally (the mode owns its own gating).
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
                // For cross-midnight schedules, the "tail" portion belongs to the previous day's session.
                // Allow it if yesterday's weekday is permitted and we're inside the post-midnight tail.
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
