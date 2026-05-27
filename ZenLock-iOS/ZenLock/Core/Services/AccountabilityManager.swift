import Foundation
import UserNotifications

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

    /// Request unlock with cool-down nudges. Returns unlock-at date.
    @discardableResult
    func requestUnlock(group: BlockGroup) -> Date {
        let partner = self.partner
        let cool = partner?.coolDownMinutes ?? 5
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

        scheduleNudges(group: group, partnerName: partner?.name, cool: cool)
        return unlocksAt
    }

    func cancelPendingUnlock() {
        defaults?.removeObject(forKey: Self.pendingUnlockKey)
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: ["unlock_nudge_1", "unlock_nudge_mid", "unlock_ready"])
    }

    private func scheduleNudges(group: BlockGroup, partnerName: String?, cool: Int) {
        let center = UNUserNotificationCenter.current()
        let partnerHint = partnerName.map { " (\($0) is your accountability partner)" } ?? ""

        let nudge1 = UNMutableNotificationContent()
        nudge1.title = "Are you sure?"
        nudge1.body = "You asked to unlock \(group.name)\(partnerHint). The shield comes down in \(cool) minutes — you can still close this and stay focused."
        nudge1.sound = .default

        let nudge1Req = UNNotificationRequest(
            identifier: "unlock_nudge_1",
            content: nudge1,
            trigger: UNTimeIntervalNotificationTrigger(timeInterval: 30, repeats: false)
        )
        center.add(nudge1Req)

        if cool >= 3 {
            let mid = UNMutableNotificationContent()
            mid.title = "Halfway through the cool-down"
            mid.body = "Last chance to back out and keep your focus streak intact."
            mid.sound = .default
            let midReq = UNNotificationRequest(
                identifier: "unlock_nudge_mid",
                content: mid,
                trigger: UNTimeIntervalNotificationTrigger(timeInterval: TimeInterval(cool * 30), repeats: false)
            )
            center.add(midReq)
        }

        let ready = UNMutableNotificationContent()
        ready.title = "🔓 \(group.name) is unlockable"
        ready.body = "Tap to manage in ZenLock\(partnerHint)."
        ready.sound = .default
        let readyReq = UNNotificationRequest(
            identifier: "unlock_ready",
            content: ready,
            trigger: UNTimeIntervalNotificationTrigger(timeInterval: TimeInterval(cool * 60), repeats: false)
        )
        center.add(readyReq)
    }
}
