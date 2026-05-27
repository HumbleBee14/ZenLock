import Foundation
import SwiftData

final class SessionRecorder {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    /// Begin a new session record. Returns the inserted session so callers can persist its id
    /// alongside the group (e.g. so deactivation can find and finalize it).
    @discardableResult
    func begin(group: BlockGroup, targetDuration: TimeInterval) -> FocusSession {
        let session = FocusSession(
            groupId: group.id,
            groupName: group.name,
            startedAt: Date(),
            targetDuration: targetDuration
        )
        context.insert(session)
        try? context.save()
        return session
    }

    /// Finalize the most recent open session for the given group.
    func end(group: BlockGroup, completed: Bool) {
        let groupId = group.id
        var descriptor = FetchDescriptor<FocusSession>(
            predicate: #Predicate { $0.groupId == groupId && $0.endedAt == nil },
            sortBy: [SortDescriptor(\.startedAt, order: .reverse)]
        )
        descriptor.fetchLimit = 1
        guard let open = (try? context.fetch(descriptor))?.first else { return }
        open.endedAt = Date()
        open.wasCompleted = completed
        try? context.save()
    }
}
