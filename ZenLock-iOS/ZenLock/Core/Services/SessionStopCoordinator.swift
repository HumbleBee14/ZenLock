import Foundation
import SwiftData

/// Single source of truth for stopping a focus session. Every entry point
/// (dashboard toggle, session editor) routes through here so the rules —
/// Strict Mode lock, Face ID gate, and the global cool-down — are applied
/// identically and the cool-down state stays in sync across screens.
@MainActor
struct SessionStopCoordinator {
    enum Result {
        case blockedStrict
        case authFailed
        case cooldownStarted(startedAt: Date, endsAt: Date)
    }

    private let blockingService: BlockingService
    private let accountability: AccountabilityManager

    init(
        blockingService: BlockingService = BlockingService(),
        accountability: AccountabilityManager = AccountabilityManager()
    ) {
        self.blockingService = blockingService
        self.accountability = accountability
    }

    /// True while this group is in its cool-down (apps still blocked, timer running).
    func pendingUnlock(for group: BlockGroup) -> AccountabilityManager.PendingUnlock? {
        guard let pending = accountability.pendingUnlock,
              pending.groupId == group.id.uuidString else { return nil }
        return pending
    }

    /// Begin stopping: enforce Strict Mode, require Face ID, then start the cool-down.
    func requestStop(_ group: BlockGroup) async -> Result {
        if group.toShared().isStrictLocked { return .blockedStrict }

        let ok = await BiometricGate.authenticate(reason: "Stop “\(group.name)”")
        guard ok else { return .authFailed }

        let endsAt = accountability.requestUnlock(group: group)
        return .cooldownStarted(startedAt: Date(), endsAt: endsAt)
    }

    /// "Keep focusing" — abandon the pending stop.
    func cancelStop(_ group: BlockGroup) {
        accountability.cancelPendingUnlock()
    }

    /// Called on a timer; when the cool-down has elapsed, actually unlock.
    /// Returns true if the session was just finalized.
    @discardableResult
    func finalizeIfElapsed(_ group: BlockGroup, context: ModelContext) -> Bool {
        guard let pending = pendingUnlock(for: group), Date() >= pending.unlocksAt else { return false }
        accountability.cancelPendingUnlock()
        SessionRecorder(context: context).end(group: group, completed: true)
        _ = blockingService.deactivateGroup(group)
        try? context.save()
        return true
    }
}
