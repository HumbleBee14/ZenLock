import Foundation

enum StreakCalculator {
    struct Summary {
        let currentStreak: Int
        let bestStreak: Int
        let focusScore: Int          // 0-100, last 7 days
        let totalFocusMinutes: Int   // last 7 days
        let completedSessions: Int   // last 7 days
    }

    /// Computes a streak/score summary from a list of sessions. A "focus day" is any day with
    /// at least one completed session (or any session ≥ 15 minutes).
    static func summary(from sessions: [FocusSession], calendar: Calendar = .current, today: Date = Date()) -> Summary {
        let focusDays = Set(sessions
            .filter { $0.wasCompleted || $0.actualDuration >= 15 * 60 }
            .compactMap { calendar.dateInterval(of: .day, for: $0.startedAt)?.start })

        var currentStreak = 0
        var cursor = calendar.startOfDay(for: today)
        while focusDays.contains(cursor) {
            currentStreak += 1
            guard let prev = calendar.date(byAdding: .day, value: -1, to: cursor) else { break }
            cursor = prev
        }

        // best streak: walk all focus days sorted, count consecutive runs
        let sorted = focusDays.sorted()
        var best = 0
        var run = 0
        var previous: Date?
        for day in sorted {
            if let prev = previous, calendar.date(byAdding: .day, value: 1, to: prev) == day {
                run += 1
            } else {
                run = 1
            }
            best = max(best, run)
            previous = day
        }

        let weekStart = calendar.date(byAdding: .day, value: -6, to: calendar.startOfDay(for: today)) ?? today
        let lastWeek = sessions.filter { $0.startedAt >= weekStart }

        let totalMinutes = lastWeek.reduce(0) { $0 + Int($1.actualDuration / 60) }
        let completed = lastWeek.filter(\.wasCompleted).count
        let attempted = lastWeek.count

        let completionRate = attempted > 0 ? Double(completed) / Double(attempted) : 0
        // 70% weight on completion, 30% on raw minutes (capped at 600 = 10h/week target)
        let minuteScore = min(1.0, Double(totalMinutes) / 600.0)
        let score = Int(((completionRate * 0.7) + (minuteScore * 0.3)) * 100)

        return Summary(
            currentStreak: currentStreak,
            bestStreak: best,
            focusScore: score,
            totalFocusMinutes: totalMinutes,
            completedSessions: completed
        )
    }
}
