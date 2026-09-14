var failures = 0
var checks = 0
func check(_ condition: @autoclosure () -> Bool, _ message: String) {
    checks += 1
    if !condition() { failures += 1; print("FAIL: \(message)") }
}
let defaults = Constants.sharedDefaults!
let storage = AppGroupStorage()
let service = BlockingService()
let monitor = DeviceActivityMonitorExtension()
func group(_ period: UsagePeriod = .hourly, minutes: Int = 15) -> BlockGroup {
    let value = BlockGroup(name: "Test", blockMode: .usageBased, usageLimitMinutes: minutes, usagePeriod: period)
    value.decodedSelection = FamilyActivitySelection(applicationTokens: ["app"], categoryTokens: [])
    return value
}
func shielded(_ value: BlockGroup) -> Bool {
    ManagedSettingsStore(named: .init(value.id.uuidString)).shield.applications == ["app"]
}
for period in [UsagePeriod.hourly, .daily] {
    let value = group(period)
    try service.activateGroup(value)
    let activity = DeviceActivityName(value.id.uuidString)
    let event = DeviceActivityEvent.Name("usage_limit_\(value.id.uuidString)")
    monitor.intervalDidStart(for: activity)
    monitor.eventDidReachThreshold(event, activity: activity)
    check(shielded(value), "\(period): immediate valid threshold must shield (past usage)")
    monitor.eventDidReachThreshold(event, activity: activity)
    check(shielded(value), "\(period): duplicate threshold remains shielded")
    // Simulate crossing the calendar boundary, not an early callback in the same period.
    UsageBlockState.record(value.id.uuidString, period: period,
                           at: Date().addingTimeInterval(-172800))
    monitor.intervalDidEnd(for: activity)
    check(!shielded(value), "\(period): interval end clears shield")
    monitor.intervalDidStart(for: activity)
    check(!shielded(value), "\(period): new interval starts unblocked")
    monitor.eventDidReachThreshold(.init("unrelated"), activity: activity)
    check(!shielded(value), "\(period): unrelated threshold must be ignored")
    monitor.eventDidReachThreshold(event, activity: activity)
    check(shielded(value), "\(period): later threshold shields")
    _ = service.deactivateGroup(value)
    monitor.eventDidReachThreshold(event, activity: activity)
    check(!shielded(value), "\(period): late callback cannot reactivate stopped group")
}
struct RegistrationFailure: Error {}
DeviceActivityCenter.failure = RegistrationFailure()
let failed = group()
do { try service.activateGroup(failed); check(false, "registration error propagates") } catch {}
check(!failed.isActive, "registration failure rolls back model active flag")
check(storage.loadGroups().first(where: { $0.id == failed.id.uuidString })?.isActive == false,
      "registration failure rolls back extension active flag")
DeviceActivityCenter.failure = nil
for period in [UsagePeriod.hourly, .daily] {
    for minutes in [0, 5, 10, 14] {
        let old = group(period, minutes: minutes)
        let draft = GroupDraft(from: old)
        check(draft.usageLimitMinutes >= 15, "legacy \(minutes) minute \(period) draft meets minimum")
        var invalid = draft
        invalid.usageLimitMinutes = minutes
        invalid.apply(to: old)
        check(old.usageLimitMinutes! >= 15, "save enforces minimum independently of slider")
    }
}

for period in [UsagePeriod.hourly, .daily] {
    let maxLimit = period == .hourly ? 50 : 720
    for minutes in [Int.min, -1, 0, 14, 15, 16, maxLimit, maxLimit + 1, Int.max] {
        let normalized = period.normalizedLimit(minutes)
        check(period.limitOptions.contains(normalized), "normalization handles \(period) boundary \(minutes)")
        check(normalized >= 15 && normalized <= maxLimit, "normalized limit is in range")
    }
    for minutes in [15, maxLimit] {
        let valid = group(period, minutes: minutes)
        try service.activateGroup(valid)
        let activity = DeviceActivityName(valid.id.uuidString)
        let events = DeviceActivityCenter.registrations[activity]!
        check(events.values.first?.threshold.minute == minutes, "registered threshold matches selected bound")
        check(events.values.first?.includesPastActivity == true, "registration counts existing period usage")
        let schedule = DeviceActivityCenter.schedules[activity]!
        check(schedule.repeats, "usage schedule repeats")
        check(schedule.intervalStart.second == 0, "usage start explicitly specifies zero seconds")
        check(schedule.warningTime?.minute == 1, "usage schedule requests a one-minute warning")
        check(schedule.intervalEnd.second == 59, "last minute remains monitored through second 59")
        check(schedule.intervalStart.minute == 0 && schedule.intervalEnd.minute == 59, "usage schedule covers expected minutes")
        check(schedule.intervalStart.hour == (period == .hourly ? nil : 0), "hourly and daily start components")
        check(schedule.intervalEnd.hour == (period == .hourly ? nil : 23), "hourly and daily end components")
        _ = service.deactivateGroup(valid)
    }
    for minutes in [0, 14, maxLimit + 1, Int.max] {
        let invalid = group(period, minutes: minutes)
        do { try service.activateGroup(invalid); check(false, "invalid runtime limit must throw") } catch {}
        check(!invalid.isActive, "invalid runtime limit is not active")
        check(DeviceActivityCenter.registrations[.init(invalid.id.uuidString)] == nil, "invalid runtime limit is not registered")
    }
}
for data in [Data?.none, Data("bad JSON".utf8), try! JSONEncoder().encode(FamilyActivitySelection())] {
    let invalid = group()
    invalid.selectionData = data
    do { try service.activateGroup(invalid); check(false, "missing/invalid/empty selection must throw") } catch {}
    check(!invalid.isActive, "invalid selection is not active")
}
let category = group()
category.decodedSelection = FamilyActivitySelection(applicationTokens: [], categoryTokens: ["social"])
try service.activateGroup(category)
monitor.eventDidReachThreshold(.init("usage_limit_\(category.id.uuidString)"), activity: .init(category.id.uuidString))
if case .specific(let tokens) = ManagedSettingsStore(named: .init(category.id.uuidString)).shield.applicationCategories {
    check(tokens == ["social"], "category-only selection is shielded")
} else { check(false, "category-only selection is shielded") }
let removed = group()
try service.activateGroup(removed)
service.removeGroupFromAppGroups(removed.id.uuidString)
monitor.eventDidReachThreshold(.init("usage_limit_\(removed.id.uuidString)"), activity: .init(removed.id.uuidString))
check(!shielded(removed), "deleted group ignores delayed threshold")
let timed = group()
timed.blockMode = .timeBased
timed.isActive = true
service.syncGroupToAppGroups(timed)
monitor.eventDidReachThreshold(.init("usage_limit_\(timed.id.uuidString)"), activity: .init(timed.id.uuidString))
check(!shielded(timed), "usage event cannot shield a group changed to time-based")

let strictLegacy = group(minutes: 5)
strictLegacy.isActive = true
strictLegacy.deepFocusEnabled = true
var cosmetic = GroupDraft(from: strictLegacy)
cosmetic.name = "Renamed"
check(cosmetic.canPreserveMonitoring(for: strictLegacy), "legacy strict cosmetic save preserves existing enforcement")
let ordinary = group()
ordinary.isActive = true
var edited = GroupDraft(from: ordinary)
edited.name = "Renamed"
check(edited.canPreserveMonitoring(for: ordinary), "usage cosmetic edit preserves monitoring")
edited.usageLimitMinutes = 20
check(!edited.canPreserveMonitoring(for: ordinary), "changed threshold requires registration")
edited = GroupDraft(from: ordinary)
edited.usagePeriod = .daily
check(!edited.canPreserveMonitoring(for: ordinary), "changed period requires registration")
edited = GroupDraft(from: ordinary)
edited.selection.applicationTokens = ["other"]
check(!edited.canPreserveMonitoring(for: ordinary), "changed app selection requires registration")
edited = GroupDraft(from: ordinary)
edited.blockMode = .timeBased
check(!edited.canPreserveMonitoring(for: ordinary), "changed mode requires registration")
ordinary.isActive = false
check(!GroupDraft(from: ordinary).canPreserveMonitoring(for: ordinary), "inactive groups do not preserve monitoring")

let retryGroup = group()
try service.activateGroup(retryGroup)
let editor = TestEditor(retryGroup)
editor.draft.usageLimitMinutes = 20
DeviceActivityCenter.failure = RegistrationFailure()
editor.submit()
check(!retryGroup.isActive && !editor.dismissed, "failed edit remains open and inactive")
check(editor.toast != nil, "failed edit reports activation error")
DeviceActivityCenter.failure = nil
editor.submit()
check(retryGroup.isActive && editor.dismissed, "corrected failed edit retries activation")
check(DeviceActivityCenter.registrations[.init(retryGroup.id.uuidString)] != nil, "retry actually registers monitoring")
let lockedEditor = TestEditor(strictLegacy)
service.syncGroupToAppGroups(strictLegacy)
ShieldManager().applyShield(for: strictLegacy.toShared(), selection: strictLegacy.decodedSelection!)
lockedEditor.draft.name = "Still locked"
lockedEditor.submit()
check(strictLegacy.isActive && shielded(strictLegacy), "cosmetic save cannot disable a legacy strict session")
check(strictLegacy.usageLimitMinutes == 5, "strict save does not change enforced legacy limit")
check(strictLegacy.name == "Still locked", "strict cosmetic edit is saved")

let recovering = group()
try service.activateGroup(recovering)
let recoveringActivity = DeviceActivityName(recovering.id.uuidString)
monitor.eventDidReachThreshold(.init("usage_limit_\(recovering.id.uuidString)"), activity: recoveringActivity)
monitor.intervalDidStart(for: recoveringActivity)
check(shielded(recovering), "duplicate or delayed interval start preserves reached threshold")
DeviceActivityCenter().stopMonitoring([recoveringActivity])
service.evaluateActiveGroups([recovering])
check(DeviceActivityCenter.registrations[recoveringActivity] != nil, "foreground restores missing usage registration")
check(shielded(recovering), "recovery preserves current-period shield")

monitor.intervalDidEnd(for: recoveringActivity)
check(shielded(recovering), "early end callback cannot erase a current-period threshold")
UsageBlockState.record(recovering.id.uuidString, period: .hourly, at: Date().addingTimeInterval(-7200))
service.evaluateActiveGroups([recovering])
check(!shielded(recovering), "foreground removes expired usage shield if boundary callback was missed")
let startBefore = DeviceActivityCenter.startCalls
service.evaluateActiveGroups([recovering])
check(DeviceActivityCenter.startCalls == startBefore,
      "foreground does not restart a healthy monitor")
DeviceActivityCenter().stopMonitoring([recoveringActivity])
DeviceActivityCenter.failure = RegistrationFailure()
UsageBlockState.record(recovering.id.uuidString, period: .hourly)
service.evaluateActiveGroups([recovering])
check(shielded(recovering), "recovery failure preserves known current-period shield")
check(storage.get(String.self, forKey: "usage_monitor_error_\(recovering.id.uuidString)")?.isEmpty == false,
      "recovery failure is available for diagnostics")
DeviceActivityCenter.failure = nil
service.evaluateActiveGroups([recovering])
check(defaults.object(forKey: "usage_monitor_error_\(recovering.id.uuidString)") == nil,
      "successful recovery clears diagnostic error")
_ = service.deactivateGroup(recovering)
check(UsageBlockState.load(recovering.id.uuidString) == nil, "stop clears persisted threshold")
var calendar = Calendar(identifier: .gregorian)
calendar.timeZone = TimeZone(identifier: "America/Los_Angeles")!
let iso = ISO8601DateFormatter()
for (dateString, dayHours) in [("2026-03-08T12:00:00Z", 23), ("2026-11-01T12:00:00Z", 25)] {
    let date = iso.date(from: dateString)!
    for period in [UsagePeriod.hourly, .daily] {
        UsageBlockState.record("boundary", period: period, at: date, calendar: calendar)
        let state = UsageBlockState.load("boundary")!
        check(state.isBlocked(period: period, at: state.start), "threshold includes period start")
        check(state.isBlocked(period: period, at: state.end.addingTimeInterval(-1)), "threshold persists until boundary")
        check(!state.isBlocked(period: period, at: state.end), "threshold expires exactly at boundary")
        let calendarEnd = calendar.dateInterval(of: period == .hourly ? .hour : .day, for: date)!.end
        check(!state.isBlocked(period: period, at: calendarEnd.addingTimeInterval(-1)),
              "scheduled end callback at :59:59 can release the shield")
        check(!state.isBlocked(period: period, at: state.start.addingTimeInterval(-1)), "clock before stored period is not blocked")
        if period == .daily {
            check(state.end.timeIntervalSince(state.start) == Double(dayHours * 3600 - 1), "DST schedule duration uses calendar")
        }
        check(!state.isBlocked(period: period == .daily ? .hourly : .daily, at: date), "different usage period ignores stale state")
    }
}

let warningGroup = group()
try service.activateGroup(warningGroup)
let warningActivity = DeviceActivityName(warningGroup.id.uuidString)
let warningEvent = DeviceActivityEvent.Name("usage_limit_\(warningGroup.id.uuidString)")
UNUserNotificationCenter.requests = []
monitor.eventWillReachThresholdWarning(warningEvent, activity: warningActivity)
check(UNUserNotificationCenter.requests.count == 1, "valid warning posts one notification")
check(!shielded(warningGroup), "warning does not prematurely block apps")
monitor.eventWillReachThresholdWarning(.init("unrelated"), activity: warningActivity)
check(UNUserNotificationCenter.requests.count == 1, "unrelated warning is ignored")
_ = service.deactivateGroup(warningGroup)
monitor.eventWillReachThresholdWarning(warningEvent, activity: warningActivity)
check(UNUserNotificationCenter.requests.count == 1, "stopped group warning is ignored")
service.removeGroupFromAppGroups(warningGroup.id.uuidString)
monitor.eventWillReachThresholdWarning(warningEvent, activity: warningActivity)
check(UNUserNotificationCenter.requests.count == 1, "deleted group warning is ignored")
print("\(checks) checks, \(failures) failures")
defaults.removePersistentDomain(forName: Constants.appGroupID)
exit(failures == 0 ? 0 : 1)
