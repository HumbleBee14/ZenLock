import Foundation

enum ReviewPrompter {
    private static let firstLaunchKey = "zen_review_first_launch"
    private static let askCountKey = "zen_review_ask_count"
    private static let askAfterDays: [Double] = [7, 28, 56]

    static func consumeAskIfDue(now: Date = Date()) -> Bool {
        guard let defaults = Constants.sharedDefaults else { return false }
        guard let first = defaults.object(forKey: firstLaunchKey) as? Date else {
            defaults.set(now, forKey: firstLaunchKey)
            return false
        }
        let count = defaults.integer(forKey: askCountKey)
        guard count < askAfterDays.count else { return false }
        guard now.timeIntervalSince(first) >= askAfterDays[count] * 86_400 else { return false }
        defaults.set(count + 1, forKey: askCountKey)
        return true
    }

    static func isBlockingNow(groups: [BlockGroup], now: Date = Date()) -> Bool {
        if let quick = ActiveSession.load(), quick.endsAt > now { return true }
        return groups.contains { group in
            guard group.isActive, group.blockMode == .timeBased else { return false }
            return ScheduleEvaluator.isWithinSchedule(group.toShared())
        }
    }
}
