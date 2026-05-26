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

        shieldManager.applyShield(for: shared, selection: selection)

        if shared.blockMode == .timeBased || shared.blockMode == .usageBased {
            try scheduleManager.startMonitoring(for: shared, selection: selection)
        }
    }

    func deactivateGroup(_ group: BlockGroup) {
        group.isActive = false
        group.updatedAt = Date()
        let shared = group.toShared()

        shieldManager.removeShield(forGroupId: shared.id)
        scheduleManager.stopMonitoring(forGroupId: shared.id)

        storage.setGroupActive(shared.id, false)
        syncGroupToAppGroups(group)
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
