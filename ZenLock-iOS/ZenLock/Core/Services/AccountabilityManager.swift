import Foundation

final class AccountabilityManager {

    private let defaults: UserDefaults?

    init(defaults: UserDefaults? = Constants.sharedDefaults) {
        self.defaults = defaults
    }

    var pendingUnlock: PendingUnlock? {
        PendingUnlock.load(defaults: defaults)
    }

    @discardableResult
    func requestUnlock(group: BlockGroup) -> Date {
        let cool = CooldownService.minutes
        let now = Date()
        let unlocksAt = now.addingTimeInterval(TimeInterval(cool * 60))
        let id = group.id.uuidString

        PendingUnlock(
            groupId: id,
            groupName: group.name,
            requestedAt: now,
            unlocksAt: unlocksAt
        ).save(defaults: defaults)
        CooldownRelease.schedule(CooldownRelease.groupActivity(id), unlocksAt: unlocksAt, now: now)
        return unlocksAt
    }

    func cancelPendingUnlock() {
        if let pending = pendingUnlock {
            CooldownRelease.cancel(CooldownRelease.groupActivity(pending.groupId))
        }
        PendingUnlock.clear(defaults: defaults)
    }
}
