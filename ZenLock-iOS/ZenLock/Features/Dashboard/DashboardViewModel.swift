import SwiftUI
import FamilyControls
import SwiftData

@Observable
final class DashboardViewModel {
    var groups: [BlockGroup] = []
    var showCreateGroup = false
    var showSettings = false
    var errorMessage: String?

    private let blockingService: BlockingService

    init(blockingService: BlockingService = BlockingService()) {
        self.blockingService = blockingService
    }

    func loadGroups(context: ModelContext) {
        let descriptor = FetchDescriptor<BlockGroup>(sortBy: [SortDescriptor(\.createdAt, order: .reverse)])
        groups = (try? context.fetch(descriptor)) ?? []
        updateWidgetSnapshot(context: context)
    }

    private func updateWidgetSnapshot(context: ModelContext) {
        let sessions = (try? context.fetch(FetchDescriptor<FocusSession>())) ?? []
        let summary = StreakCalculator.summary(from: sessions)
        Constants.sharedDefaults?.set(summary.focusScore, forKey: "zen_widget_focus_score")
    }

    func toggleGroup(_ group: BlockGroup, context: ModelContext) {
        let recorder = SessionRecorder(context: context)
        do {
            if group.isActive {
                switch blockingService.deactivateGroup(group) {
                case .success:
                    recorder.end(group: group, completed: true)
                case .failure(let error):
                    errorMessage = error.errorDescription
                    return
                }
            } else {
                try blockingService.activateGroup(group)
                recorder.begin(group: group, targetDuration: estimatedDuration(for: group))
            }
            try context.save()
        } catch {
            group.isActive = false
            errorMessage = error.localizedDescription
        }
    }

    func deleteGroup(_ group: BlockGroup, context: ModelContext) {
        blockingService.removeGroupFromAppGroups(group.id.uuidString)
        context.delete(group)
        try? context.save()
        loadGroups(context: context)
    }

    var activeGroupCount: Int { groups.filter(\.isActive).count }
    var totalGroupCount: Int { groups.count }

    private func estimatedDuration(for group: BlockGroup) -> TimeInterval {
        switch group.blockMode {
        case .timeBased:
            guard let sH = group.scheduleStartHour, let sM = group.scheduleStartMinute,
                  let eH = group.scheduleEndHour, let eM = group.scheduleEndMinute else { return 0 }
            let startMin = sH * 60 + sM
            let endMin = eH * 60 + eM
            let diff = endMin > startMin ? endMin - startMin : (24 * 60 - startMin) + endMin
            return TimeInterval(diff * 60)
        case .usageBased:
            return TimeInterval((group.usageLimitMinutes ?? 0) * 60)
        case .frictionBased:
            return 0
        }
    }
}
