import SwiftUI
import FamilyControls
import SwiftData

@MainActor
@Observable
final class DashboardViewModel {
    var groups: [BlockGroup] = []
    var showCreateGroup = false
    var showSettings = false
    var errorMessage: String?

    private let blockingService: BlockingService
    private let stopCoordinator = SessionStopCoordinator()

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

    var toast: ZenToastData?
    var stopConfirmGroup: BlockGroup?
    var deleteConfirmGroup: BlockGroup?

    func toggleGroup(_ group: BlockGroup, context: ModelContext) {
        if group.isActive {
            stopConfirmGroup = group
        } else {
            activate(group, context: context)
        }
    }

    func confirmStop(context: ModelContext) {
        guard let group = stopConfirmGroup else { return }
        stopConfirmGroup = nil
        Task { await deactivate(group, context: context) }
    }

    private func activate(_ group: BlockGroup, context: ModelContext) {
        let recorder = SessionRecorder(context: context)
        do {
            let outcome = try blockingService.armOrActivate(group)
            if case .activeNow = outcome {
                recorder.begin(group: group, targetDuration: estimatedDuration(for: group))
            }
            try context.save()
            toast = ScheduleToastFactory.make(for: outcome, group: group)
        } catch {
            group.isActive = false
            errorMessage = error.localizedDescription
        }
    }

    @MainActor
    private func deactivate(_ group: BlockGroup, context: ModelContext) async {
        switch await stopCoordinator.requestStop(group) {
        case .blockedStrict:
            toast = ZenToastData(
                message: "Strict Mode is on — this session can't be stopped until its schedule ends.",
                kind: .warning
            )
        case .authFailed:
            toast = ZenToastData(message: "Authentication required to stop.", kind: .warning)
        case .cooldownStarted:
            toast = ZenToastData(message: "Cooling down — apps unlock when the timer ends.", kind: .info)
        }
    }

    /// Called by the dashboard timer to finalize any elapsed cool-downs.
    func finalizeElapsedCooldowns(context: ModelContext) {
        for group in groups where stopCoordinator.pendingUnlock(for: group) != nil {
            stopCoordinator.finalizeIfElapsed(group, context: context)
        }
    }

    func pendingUnlock(for group: BlockGroup) -> AccountabilityManager.PendingUnlock? {
        stopCoordinator.pendingUnlock(for: group)
    }

    func cancelStop(_ group: BlockGroup) {
        stopCoordinator.cancelStop(group)
    }

    func deleteGroup(_ group: BlockGroup, context: ModelContext) {
        // Deleting an active Strict Mode session would tear down its shield and
        // is therefore just another way to stop it early — block it, same as the
        // stop and deactivate paths.
        if group.toShared().isStrictLocked {
            toast = ZenToastData(
                message: "Strict Mode is on — this session can't be deleted until it ends.",
                kind: .warning
            )
            return
        }
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
        }
    }
}
