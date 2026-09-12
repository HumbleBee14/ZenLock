import Foundation
import SwiftData

final class SessionRecorder {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    func beginQuickFocus(endsAt: Date) {
        endQuickFocus()
        let now = Date()
        let session = FocusSession(
            groupId: nil,
            groupName: SessionLedger.quickFocusName,
            startedAt: now,
            targetDuration: endsAt.timeIntervalSince(now)
        )
        context.insert(session)
        try? context.save()
    }

    func extendQuickFocus(endsAt: Date) {
        guard let open = openQuickFocus() else { return }
        open.targetDuration = endsAt.timeIntervalSince(open.startedAt)
        try? context.save()
    }

    func endQuickFocus(now: Date = Date()) {
        guard let open = openQuickFocus() else { return }
        let plannedEnd = open.startedAt.addingTimeInterval(open.targetDuration)
        open.endedAt = min(now, plannedEnd)
        open.wasCompleted = now >= plannedEnd
        try? context.save()
    }

    private func openQuickFocus() -> FocusSession? {
        var descriptor = FetchDescriptor<FocusSession>(
            predicate: #Predicate { $0.groupId == nil && $0.endedAt == nil },
            sortBy: [SortDescriptor(\.startedAt, order: .reverse)]
        )
        descriptor.fetchLimit = 1
        return (try? context.fetch(descriptor))?.first
    }
}
