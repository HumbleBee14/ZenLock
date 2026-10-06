import Foundation
import DeviceActivity
import ManagedSettings

enum ShieldExpiry {
    static func isStale(groupId: String, now: Date = Date(), defaults: UserDefaults? = Constants.sharedDefaults) -> Bool {
        if let pending = PendingUnlock.load(defaults: defaults), pending.groupId == groupId {
            return now >= pending.unlocksAt
        }
        guard let group = SharedBlockGroup.load(from: defaults).first(where: { $0.id == groupId }),
              group.isActive else { return true }
        switch group.blockMode {
        case .timeBased:
            return !ScheduleEvaluator.isWithinSchedule(group, at: now)
        case .usageBased:
            let period = group.usagePeriod ?? .daily
            guard let state = UsageBlockState.load(groupId, defaults: defaults) else { return true }
            return !state.isBlocked(period: period, at: now)
        }
    }

    @discardableResult
    static func releaseIfStale(groupId: String, now: Date = Date(), defaults: UserDefaults? = Constants.sharedDefaults) -> Bool {
        guard isStale(groupId: groupId, now: now, defaults: defaults) else { return false }
        if CooldownRelease.releaseGroupIfElapsed(now: now, defaults: defaults) == groupId {
            return true
        }
        guard hasShield(groupId: groupId) else { return false }
        clearStores(groupId: groupId)
        if let state = UsageBlockState.load(groupId, defaults: defaults),
           let group = SharedBlockGroup.load(from: defaults).first(where: { $0.id == groupId }),
           !state.isBlocked(period: group.usagePeriod ?? .daily, at: now) {
            UsageBlockState.clear(groupId, defaults: defaults)
        }
        return true
    }

    static func quickFocusIsStale(now: Date = Date(), defaults: UserDefaults? = Constants.sharedDefaults) -> Bool {
        guard let data = defaults?.data(forKey: Constants.Keys.quickFocusSession),
              let session = try? JSONDecoder().decode(QuickFocusState.self, from: data) else { return true }
        if let cooldown = session.cooldownEndsAt, now >= cooldown { return true }
        return now >= session.endsAt
    }

    @discardableResult
    static func releaseQuickFocusIfStale(now: Date = Date(), defaults: UserDefaults? = Constants.sharedDefaults) -> Bool {
        guard quickFocusIsStale(now: now, defaults: defaults) else { return false }
        if CooldownRelease.releaseQuickFocusIfElapsed(now: now, defaults: defaults) { return true }
        ManagedSettingsStore(named: .init(Constants.quickFocusActivity)).clearAllSettings()
        DeviceActivityCenter().stopMonitoring([
            DeviceActivityName(Constants.quickFocusActivity),
            DeviceActivityName(CooldownRelease.quickFocusActivity)
        ])
        defaults?.removeObject(forKey: Constants.Keys.quickFocusSession)
        return true
    }

    static func sweep(excluding groupId: String? = nil, now: Date = Date(), defaults: UserDefaults? = Constants.sharedDefaults) {
        for group in SharedBlockGroup.load(from: defaults) where group.id != groupId {
            releaseIfStale(groupId: group.id, now: now, defaults: defaults)
        }
        if defaults?.data(forKey: Constants.Keys.quickFocusSession) != nil {
            releaseQuickFocusIfStale(now: now, defaults: defaults)
        }
    }

    private static func hasShield(groupId: String) -> Bool {
        [groupId, "\(groupId)-A", "\(groupId)-B"].contains { name in
            let shield = ManagedSettingsStore(named: .init(name)).shield
            return shield.applications != nil || shield.applicationCategories != nil
        }
    }

    private static func clearStores(groupId: String) {
        for name in [groupId, "\(groupId)-A", "\(groupId)-B"] {
            ManagedSettingsStore(named: .init(name)).clearAllSettings()
        }
    }

    private struct QuickFocusState: Decodable {
        var endsAt: Date
        var cooldownEndsAt: Date?
    }
}
