import Foundation
import SwiftData

enum SessionLedger {
    static let quickFocusName = "Quick Focus"
    private static let backfillDays = 60

    struct Window {
        let start: Date
        let end: Date
        var duration: TimeInterval { end.timeIntervalSince(start) }
    }

    static func reconcile(context: ModelContext, now: Date = Date(), calendar: Calendar = .current) {
        let groups = (try? context.fetch(FetchDescriptor<BlockGroup>())) ?? []
        var sessions = (try? context.fetch(FetchDescriptor<FocusSession>(sortBy: [SortDescriptor(\.startedAt)]))) ?? []
        let groupsByID = Dictionary(groups.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })

        for session in sessions where session.endedAt == nil {
            closeIfDue(session, group: session.groupId.flatMap { groupsByID[$0] }, now: now, calendar: calendar)
        }

        for group in groups where group.isActive && group.blockMode == .timeBased {
            backfill(group: group, sessions: &sessions, context: context, now: now, calendar: calendar)
        }

        try? context.save()
    }

    private static func closeIfDue(_ session: FocusSession, group: BlockGroup?, now: Date, calendar: Calendar) {
        guard let group else {
            if session.groupId == nil {
                closeQuickFocus(session, now: now)
            } else {
                close(session, at: min(now, session.startedAt.addingTimeInterval(session.targetDuration)), now: now)
            }
            return
        }

        guard group.blockMode == .timeBased,
              let window = window(for: group, containing: session.startedAt, calendar: calendar) else {
            close(session, at: min(now, session.startedAt.addingTimeInterval(max(session.targetDuration, 0))), now: now)
            return
        }

        if !group.isActive {
            let stoppedAt = max(session.startedAt, min(window.end, group.updatedAt))
            session.endedAt = stoppedAt
            session.wasCompleted = stoppedAt >= window.end
        } else if now >= window.end {
            session.endedAt = window.end
            session.wasCompleted = true
        }
    }

    private static func closeQuickFocus(_ session: FocusSession, now: Date) {
        let plannedEnd = session.startedAt.addingTimeInterval(session.targetDuration)
        if now >= plannedEnd {
            session.endedAt = plannedEnd
            session.wasCompleted = true
        } else if ActiveSession.load() == nil {
            session.endedAt = now
            session.wasCompleted = false
        }
    }

    private static func close(_ session: FocusSession, at end: Date, now: Date) {
        let plannedEnd = session.startedAt.addingTimeInterval(session.targetDuration)
        session.endedAt = max(session.startedAt, end)
        session.wasCompleted = session.targetDuration > 0 && end >= plannedEnd
    }

    private static func backfill(group: BlockGroup, sessions: inout [FocusSession], context: ModelContext, now: Date, calendar: Calendar) {
        let horizon = calendar.date(byAdding: .day, value: -backfillDays, to: now) ?? now
        let activeSince = max(group.updatedAt, horizon)
        guard let firstDay = calendar.date(byAdding: .day, value: -1, to: calendar.startOfDay(for: activeSince)) else { return }

        var day = firstDay
        let today = calendar.startOfDay(for: now)
        while day <= today {
            defer { day = calendar.date(byAdding: .day, value: 1, to: day) ?? today.addingTimeInterval(1) }

            guard let window = window(for: group, startingOn: day, calendar: calendar) else { return }
            guard window.end > activeSince, window.start <= now else { continue }
            if let days = group.scheduleDaysOfWeek, !days.isEmpty,
               let weekday = calendar.dateComponents([.weekday], from: day).weekday,
               !days.contains(weekday) {
                continue
            }

            let alreadyRecorded = sessions.contains { session in
                session.groupId == group.id && session.startedAt >= window.start && session.startedAt < window.end
            }
            if !alreadyRecorded {
                let startedAt = max(window.start, activeSince)
                let finished = now >= window.end
                let session = FocusSession(
                    groupId: group.id,
                    groupName: group.name,
                    startedAt: startedAt,
                    endedAt: finished ? window.end : nil,
                    targetDuration: window.duration,
                    wasCompleted: finished
                )
                context.insert(session)
                sessions.append(session)
            }

            if !group.scheduleRepeats { return }
        }
    }

    static func window(for group: BlockGroup, containing date: Date, calendar: Calendar = .current) -> Window? {
        let day = calendar.startOfDay(for: date)
        let candidates = [day, calendar.date(byAdding: .day, value: -1, to: day)].compactMap { $0 }
        for candidate in candidates {
            if let window = window(for: group, startingOn: candidate, calendar: calendar),
               window.start <= date, date < window.end {
                return window
            }
        }
        return nil
    }

    static func window(for group: BlockGroup, startingOn day: Date, calendar: Calendar = .current) -> Window? {
        guard let sH = group.scheduleStartHour, let sM = group.scheduleStartMinute,
              let eH = group.scheduleEndHour, let eM = group.scheduleEndMinute,
              let start = calendar.date(bySettingHour: sH, minute: sM, second: 0, of: day) else { return nil }
        var minutes = (eH * 60 + eM) - (sH * 60 + sM)
        if minutes <= 0 { minutes += 24 * 60 }
        return Window(start: start, end: start.addingTimeInterval(TimeInterval(minutes * 60)))
    }
}
