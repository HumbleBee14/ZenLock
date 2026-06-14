import Foundation
import FamilyControls

@Observable
final class BlockingService {
    private let shieldManager: ShieldManaging
    private let scheduleManager: ActivityScheduleManaging
    private let storage: AppGroupStorage

    init(
        shieldManager: ShieldManaging = ShieldManager(),
        scheduleManager: ActivityScheduleManaging = ActivityScheduleManager(),
        storage: AppGroupStorage = AppGroupStorage()
    ) {
        self.shieldManager = shieldManager
        self.scheduleManager = scheduleManager
        self.storage = storage
    }

    func activateGroup(_ group: BlockGroup) throws {
        group.isActive = true
        group.updatedAt = Date()
        let shared = group.toShared()

        syncGroupToAppGroups(group)

        guard let selection = group.decodedSelection else { return }

        switch shared.blockMode {
        case .timeBased:
            try scheduleManager.startMonitoring(for: shared, selection: selection)
            if ScheduleEvaluator.isWithinSchedule(shared) {
                shieldManager.applyShield(for: shared, selection: selection)
            }
        case .usageBased:
            try scheduleManager.startMonitoring(for: shared, selection: selection)
        }
    }

    /// Outcome of arming a group on save, so the UI can show the right toast.
    enum ArmOutcome {
        /// Blocking is in effect right now (within window, or usage-based armed).
        case activeNow
        /// Registered to fire automatically; `startsAt` is the next start time today (nil = recurring/unknown).
        case armed(startsAt: Date?)
        /// A non-repeating time window that has already fully passed today — left off.
        case windowPassed
    }

    /// Called on create/edit save. Registers monitoring so iOS auto-activates
    /// the group at its scheduled time without the user reopening the app.
    /// For a non-repeating window already fully in the past today, leaves the
    /// group inactive (the user can still turn it on manually).
    @discardableResult
    func armOrActivate(_ group: BlockGroup) throws -> ArmOutcome {
        let shared = group.toShared()

        // Non-repeating time window fully in the past today → don't arm.
        if shared.blockMode == .timeBased,
           !shared.scheduleRepeats,
           isWindowFullyPastToday(shared) {
            group.isActive = false
            group.updatedAt = Date()
            syncGroupToAppGroups(group)
            return .windowPassed
        }

        try activateGroup(group)

        if shared.blockMode == .timeBased {
            if ScheduleEvaluator.isWithinSchedule(shared) {
                return .activeNow
            }
            return .armed(startsAt: nextStartDate(shared))
        }
        return .activeNow
    }

    private func isWindowFullyPastToday(_ group: SharedBlockGroup, now: Date = Date(), calendar: Calendar = .current) -> Bool {
        guard let sH = group.scheduleStartHour, let sM = group.scheduleStartMinute,
              let eH = group.scheduleEndHour, let eM = group.scheduleEndMinute else { return false }
        // Cross-midnight windows are never "fully past" within the same day.
        let startMinutes = sH * 60 + sM
        let endMinutes = eH * 60 + eM
        if startMinutes > endMinutes { return false }
        let comps = calendar.dateComponents([.hour, .minute], from: now)
        let nowMinutes = (comps.hour ?? 0) * 60 + (comps.minute ?? 0)
        return nowMinutes >= endMinutes
    }

    private func nextStartDate(_ group: SharedBlockGroup, now: Date = Date(), calendar: Calendar = .current) -> Date? {
        guard let sH = group.scheduleStartHour, let sM = group.scheduleStartMinute else { return nil }
        return calendar.date(bySettingHour: sH, minute: sM, second: 0, of: now)
    }

    enum DeactivationError: Error, LocalizedError {
        case deepFocusLocked

        var errorDescription: String? {
            switch self {
            case .deepFocusLocked:
                return "Strict Mode is on. This group can't be turned off while its session is active."
            }
        }
    }

    @discardableResult
    func deactivateGroup(_ group: BlockGroup) -> Result<Void, DeactivationError> {
        let shared = group.toShared()
        if shared.deepFocusEnabled && ScheduleEvaluator.isWithinSchedule(shared) && shared.blockMode == .timeBased {
            return .failure(.deepFocusLocked)
        }
        if shared.deepFocusEnabled && shared.blockMode != .timeBased {
            return .failure(.deepFocusLocked)
        }

        group.isActive = false
        group.updatedAt = Date()

        shieldManager.removeShield(forGroupId: shared.id)
        scheduleManager.stopMonitoring(forGroupId: shared.id)

        storage.setGroupActive(shared.id, false)
        storage.resetOpenCount(shared.id)
        syncGroupToAppGroups(group)
        return .success(())
    }

    /// Re-evaluate active groups and sync shield state on app foreground.
    func evaluateActiveGroups(_ groups: [BlockGroup]) {
        for group in groups where group.isActive {
            guard let selection = group.decodedSelection else {
                continue
            }
            let shared = group.toShared()

            switch shared.blockMode {
            case .timeBased:
                if ScheduleEvaluator.isWithinSchedule(shared) {
                    shieldManager.applyShield(for: shared, selection: selection)
                } else {
                    shieldManager.removeShield(forGroupId: shared.id)
                }
            case .usageBased:
                break
            }
        }
    }

    private func isInFrictionBypass(_ groupId: String) -> Bool {
        guard let until = Constants.sharedDefaults?.object(forKey: "friction_bypass_until_\(groupId)") as? Date else { return false }
        return Date() < until
    }

    func syncGroupToAppGroups(_ group: BlockGroup) {
        let shared = group.toShared()
        storage.setGroupActive(shared.id, shared.isActive)

        if let selectionData = group.selectionData {
            storage.saveSelection(selectionData, forGroupId: shared.id)
        }

        var groups = storage.loadGroups()
        if let index = groups.firstIndex(where: { $0.id == shared.id }) {
            groups[index] = shared
        } else {
            groups.append(shared)
        }
        storage.saveGroups(groups)
    }

    func removeGroupFromAppGroups(_ groupId: String) {
        shieldManager.removeShield(forGroupId: groupId)
        scheduleManager.stopMonitoring(forGroupId: groupId)
        storage.setGroupActive(groupId, false)

        var groups = storage.loadGroups()
        groups.removeAll { $0.id == groupId }
        storage.saveGroups(groups)
    }
}
