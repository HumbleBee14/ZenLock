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

    func toggleGroup(_ group: BlockGroup, context: ModelContext) {
        if group.isActive {
            Task { await deactivate(group, context: context) }
        } else {
            activate(group, context: context)
        }
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
        // Strict Mode is checked first — a locked session never reaches auth.
        let shared = group.toShared()
        if shared.deepFocusEnabled,
           (shared.blockMode != .timeBased || ScheduleEvaluator.isWithinSchedule(shared)) {
            toast = ZenToastData(
                message: "Strict Mode is on — this session can't be stopped until its schedule ends.",
                kind: .warning
            )
            return
        }

        // Require the device owner (Face ID / Touch ID / passcode) to stop.
        let ok = await BiometricGate.authenticate(reason: "Stop “\(group.name)” focus session")
        guard ok else {
            toast = ZenToastData(message: "Authentication required to stop.", kind: .warning)
            return
        }

        let recorder = SessionRecorder(context: context)
        switch blockingService.deactivateGroup(group) {
        case .success:
            recorder.end(group: group, completed: true)
            try? context.save()
        case .failure(let error):
            errorMessage = error.errorDescription
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
        }
    }
}
