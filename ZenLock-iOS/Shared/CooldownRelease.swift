import Foundation
import DeviceActivity
import ManagedSettings

struct PendingUnlock: Codable {
    let groupId: String
    let groupName: String
    let requestedAt: Date
    let unlocksAt: Date

    static let key = "zen_pending_unlock"

    static func load(defaults: UserDefaults? = Constants.sharedDefaults) -> PendingUnlock? {
        guard let data = defaults?.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(PendingUnlock.self, from: data)
    }

    func save(defaults: UserDefaults? = Constants.sharedDefaults) {
        guard let data = try? JSONEncoder().encode(self) else { return }
        defaults?.set(data, forKey: Self.key)
    }

    static func clear(defaults: UserDefaults? = Constants.sharedDefaults) {
        defaults?.removeObject(forKey: key)
    }
}

enum CooldownRelease {
    static let groupActivityPrefix = "zen_cooldown_release_"
    static let quickFocusActivity = "zen_quick_focus_cooldown"
    static let completedKey = "zen_completed_unlocks"
    static let registrationErrorKey = "zen_cooldown_release_error"
    static let minimumInterval: TimeInterval = 15 * 60

    static func groupActivity(_ groupId: String) -> DeviceActivityName {
        DeviceActivityName(groupActivityPrefix + groupId)
    }

    static func isReleaseActivity(_ activity: DeviceActivityName) -> Bool {
        activity.rawValue.hasPrefix(groupActivityPrefix) || activity.rawValue == quickFocusActivity
    }

    static func schedule(_ activity: DeviceActivityName, unlocksAt: Date, now: Date = Date(), calendar: Calendar = .current) {
        let center = DeviceActivityCenter()
        center.stopMonitoring([activity])
        let schedule = paddedSchedule(endingAt: unlocksAt, now: now, calendar: calendar)
        do {
            try center.startMonitoring(activity, during: schedule)
            Constants.sharedDefaults?.removeObject(forKey: registrationErrorKey)
        } catch {
            Constants.sharedDefaults?.set("\(activity.rawValue): \(error.localizedDescription)", forKey: registrationErrorKey)
        }
    }

    static func cancelPending(forGroupId groupId: String, defaults: UserDefaults? = Constants.sharedDefaults) {
        cancel(groupActivity(groupId))
        if PendingUnlock.load(defaults: defaults)?.groupId == groupId {
            PendingUnlock.clear(defaults: defaults)
        }
    }

    static func paddedSchedule(endingAt endsAt: Date, now: Date = Date(), calendar: Calendar = .current) -> DeviceActivitySchedule {
        let start = min(now, endsAt.addingTimeInterval(-minimumInterval))
        let units: Set<Calendar.Component> = [.year, .month, .day, .hour, .minute, .second]
        return DeviceActivitySchedule(
            intervalStart: calendar.dateComponents(units, from: start),
            intervalEnd: calendar.dateComponents(units, from: endsAt),
            repeats: false
        )
    }

    static func cancel(_ activity: DeviceActivityName) {
        DeviceActivityCenter().stopMonitoring([activity])
    }

    @discardableResult
    static func releaseGroupIfElapsed(now: Date = Date(), defaults: UserDefaults? = Constants.sharedDefaults) -> String? {
        guard let pending = PendingUnlock.load(defaults: defaults), now >= pending.unlocksAt else { return nil }
        let id = pending.groupId

        for name in [id, "\(id)-A", "\(id)-B"] {
            ManagedSettingsStore(named: .init(name)).clearAllSettings()
        }
        DeviceActivityCenter().stopMonitoring([
            DeviceActivityName(id),
            DeviceActivityName("\(id)-A"),
            DeviceActivityName("\(id)-B"),
            groupActivity(id)
        ])

        defaults?.set(false, forKey: Constants.Keys.activeGroupPrefix + id)
        var groups = SharedBlockGroup.load(from: defaults)
        if let index = groups.firstIndex(where: { $0.id == id }) {
            groups[index].isActive = false
            SharedBlockGroup.save(groups, to: defaults)
        }
        UsageBlockState.clear(id, defaults: defaults)

        var completed = defaults?.stringArray(forKey: completedKey) ?? []
        if !completed.contains(id) { completed.append(id) }
        defaults?.set(completed, forKey: completedKey)

        PendingUnlock.clear(defaults: defaults)
        return id
    }

    static func consumeCompletedUnlock(_ groupId: String, defaults: UserDefaults? = Constants.sharedDefaults) {
        let remaining = (defaults?.stringArray(forKey: completedKey) ?? []).filter { $0 != groupId }
        if remaining.isEmpty {
            defaults?.removeObject(forKey: completedKey)
        } else {
            defaults?.set(remaining, forKey: completedKey)
        }
    }

    static func takeCompletedUnlocks(defaults: UserDefaults? = Constants.sharedDefaults) -> [String] {
        let completed = defaults?.stringArray(forKey: completedKey) ?? []
        defaults?.removeObject(forKey: completedKey)
        return completed
    }

    private struct QuickFocusCooldown: Decodable {
        var cooldownEndsAt: Date?
    }

    @discardableResult
    static func releaseQuickFocusIfElapsed(now: Date = Date(), defaults: UserDefaults? = Constants.sharedDefaults) -> Bool {
        guard let data = defaults?.data(forKey: Constants.Keys.quickFocusSession),
              let session = try? JSONDecoder().decode(QuickFocusCooldown.self, from: data),
              let endsAt = session.cooldownEndsAt, now >= endsAt else { return false }

        ManagedSettingsStore(named: .init(Constants.quickFocusActivity)).clearAllSettings()
        DeviceActivityCenter().stopMonitoring([
            DeviceActivityName(Constants.quickFocusActivity),
            DeviceActivityName(quickFocusActivity)
        ])
        defaults?.removeObject(forKey: Constants.Keys.quickFocusSession)
        defaults?.set(now, forKey: Constants.Keys.quickFocusReleasedAt)
        return true
    }
}
