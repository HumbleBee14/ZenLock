import Foundation
import SwiftData

@Model
final class FocusSession {
    var id: UUID
    var groupId: UUID?
    var groupName: String
    var startedAt: Date
    var endedAt: Date?
    var targetDuration: TimeInterval
    var wasCompleted: Bool

    var actualDuration: TimeInterval {
        guard let endedAt else { return Date().timeIntervalSince(startedAt) }
        return endedAt.timeIntervalSince(startedAt)
    }

    init(
        id: UUID = UUID(),
        groupId: UUID? = nil,
        groupName: String,
        startedAt: Date = Date(),
        endedAt: Date? = nil,
        targetDuration: TimeInterval,
        wasCompleted: Bool = false
    ) {
        self.id = id
        self.groupId = groupId
        self.groupName = groupName
        self.startedAt = startedAt
        self.endedAt = endedAt
        self.targetDuration = targetDuration
        self.wasCompleted = wasCompleted
    }
}
