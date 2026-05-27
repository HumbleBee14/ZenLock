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
        case .frictionBased:
            shieldManager.applyShield(for: shared, selection: selection)
            try scheduleManager.startMonitoring(for: shared, selection: selection)
        }
    }

    enum DeactivationError: Error, LocalizedError {
        case deepFocusLocked

        var errorDescription: String? {
            switch self {
            case .deepFocusLocked:
                return "Deep Focus is on. This group can't be turned off while its session is active."
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

    /// Re-evaluate all active groups (call from sceneDidBecomeActive). Heals from token drift
    /// and any extension callbacks that may have been missed under system load.
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
            case .frictionBased:
                if isInFrictionBypass(shared.id) {
                    shieldManager.removeShield(forGroupId: shared.id)
                } else {
                    shieldManager.applyShield(for: shared, selection: selection)
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
