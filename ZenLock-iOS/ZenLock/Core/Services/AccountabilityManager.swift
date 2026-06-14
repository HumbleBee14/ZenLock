import Foundation

struct AccountabilityPartner: Codable, Equatable {
    var name: String
    var coolDownMinutes: Int
}

/// Single-device accountability using partner-based friction to encourage mindful unlocks.
final class AccountabilityManager {
    static let partnerKey = "zen_accountability_partner"
    static let pendingUnlockKey = "zen_pending_unlock"

    struct PendingUnlock: Codable {
        let groupId: String
        let groupName: String
        let requestedAt: Date
        let unlocksAt: Date
    }

    private let defaults: UserDefaults?

    init(defaults: UserDefaults? = Constants.sharedDefaults) {
        self.defaults = defaults
    }

    // MARK: - Partner

    var partner: AccountabilityPartner? {
        get {
            guard let data = defaults?.data(forKey: Self.partnerKey) else { return nil }
            return try? JSONDecoder().decode(AccountabilityPartner.self, from: data)
        }
        set {
            guard let value = newValue else {
                defaults?.removeObject(forKey: Self.partnerKey)
                return
            }
            if let data = try? JSONEncoder().encode(value) {
                defaults?.set(data, forKey: Self.partnerKey)
            }
        }
    }

    // MARK: - Unlock flow

    var pendingUnlock: PendingUnlock? {
        guard let data = defaults?.data(forKey: Self.pendingUnlockKey) else { return nil }
        return try? JSONDecoder().decode(PendingUnlock.self, from: data)
    }

    /// Begin the cool-down. Returns the unlock-at date.
    @discardableResult
    func requestUnlock(group: BlockGroup) -> Date {
        let cool = CooldownService.minutes
        let now = Date()
        let unlocksAt = now.addingTimeInterval(TimeInterval(cool * 60))

        let pending = PendingUnlock(
            groupId: group.id.uuidString,
            groupName: group.name,
            requestedAt: now,
            unlocksAt: unlocksAt
        )
        if let data = try? JSONEncoder().encode(pending) {
            defaults?.set(data, forKey: Self.pendingUnlockKey)
        }
        return unlocksAt
    }

    func cancelPendingUnlock() {
        defaults?.removeObject(forKey: Self.pendingUnlockKey)
    }
}
