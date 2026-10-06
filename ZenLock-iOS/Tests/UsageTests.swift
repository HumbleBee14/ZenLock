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
func group(_ period: UsagePeriod = .hourly, minutes: Int = 20) -> BlockGroup {
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
    let minimum = period.limitOptions[0]
    for minutes in [Int.min, -5, 0, 5, 14, 15] {
        let old = group(period, minutes: minutes)
        let draft = GroupDraft(from: old)
        check(period.limitOptions.contains(draft.usageLimitMinutes), "legacy \(minutes) minute \(period) draft snaps to an allowed step")
        var invalid = draft
        invalid.usageLimitMinutes = minutes
        invalid.apply(to: old)
        check(old.usageLimitMinutes! >= minimum && period.limitOptions.contains(old.usageLimitMinutes!), "save enforces allowed steps independently of slider")
    }
}
check(UsagePeriod.hourly.limitOptions == Array(1...59), "hourly limits move in 1-minute steps")
check(UsagePeriod.daily.limitOptions.first == 10 && UsagePeriod.daily.limitOptions.last == 720, "daily limits span 10 minutes to 12 hours")
check(UsagePeriod.daily.limitOptions.allSatisfy { $0 % 10 == 0 }, "daily limits move in 10-minute steps")
check(UsagePeriod.daily.normalizedLimit(15) == 10 || UsagePeriod.daily.normalizedLimit(15) == 20, "legacy 15-minute daily limit snaps to a 10-minute step")

for period in [UsagePeriod.hourly, .daily] {
    let maxLimit = period == .hourly ? 59 : 720
    let minLimit = period.limitOptions[0]
    for minutes in [Int.min, -1, 0, 1, 9, 10, 11, 14, 15, 16, maxLimit, maxLimit + 1, Int.max] {
        let normalized = period.normalizedLimit(minutes)
        check(period.limitOptions.contains(normalized), "normalization handles \(period) boundary \(minutes)")
        check(normalized >= minLimit && normalized <= maxLimit, "normalized limit is in range")
    }
    for minutes in [minLimit, maxLimit] {
        let valid = group(period, minutes: minutes)
        try service.activateGroup(valid)
        let activity = DeviceActivityName(valid.id.uuidString)
        let events = DeviceActivityCenter.registrations[activity]!
        check(events.values.first?.threshold.minute == minutes, "registered threshold matches selected bound")
        check(events.values.first?.includesPastActivity == true, "registration counts existing period usage")
        let schedule = DeviceActivityCenter.schedules[activity]!
        check(schedule.repeats, "usage schedule repeats")
        check(schedule.intervalStart.second == 0, "usage start explicitly specifies zero seconds")
        check(schedule.warningTime?.minute == 5, "usage schedule requests a five-minute warning")
        check(schedule.intervalEnd.second == 59, "last minute remains monitored through second 59")
        check(schedule.intervalStart.minute == 0 && schedule.intervalEnd.minute == 59, "usage schedule covers expected minutes")
        check(schedule.intervalStart.hour == (period == .hourly ? nil : 0), "hourly and daily start components")
        check(schedule.intervalEnd.hour == (period == .hourly ? nil : 23), "hourly and daily end components")
        _ = service.deactivateGroup(valid)
    }
    for minutes in (period == .hourly ? [0, 60, Int.max] : [0, 5, 15, 725, Int.max]) {
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
edited.usageLimitMinutes = 25
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
editor.draft.usageLimitMinutes = 25
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

for period in [UsagePeriod.hourly, .daily] {
    let pastUsage = group(period)
    let outcome = try service.armOrActivate(pastUsage)
    let message = ScheduleToastFactory.make(for: outcome, group: pastUsage).message
    check(message.contains(period == .hourly ? "this hour" : "today"), "activation copy identifies the current usage period")
    check(message.contains("already"), "activation copy explains already-reached limits")
    _ = service.deactivateGroup(pastUsage)
    try service.armOrActivate(pastUsage)
    let activity = DeviceActivityName(pastUsage.id.uuidString)
    check(DeviceActivityCenter.registrations[activity]?.values.first?.includesPastActivity == true,
          "re-enabling still requests prior usage in the current period")
    monitor.eventDidReachThreshold(.init("usage_limit_\(pastUsage.id.uuidString)"), activity: activity)
    check(shielded(pastUsage), "re-enabled group honors immediate past-usage threshold")
}
let stale = group()
try service.activateGroup(stale)
let staleActivity = DeviceActivityName(stale.id.uuidString)
var startsBefore = DeviceActivityCenter.startCalls
service.evaluateActiveGroups([stale])
check(DeviceActivityCenter.startCalls == startsBefore, "a fresh usage registration is not refreshed")
storage.setDate(Date().addingTimeInterval(-90_000), forKey: "zen_usage_registered_\(stale.id.uuidString)")
service.evaluateActiveGroups([stale])
check(DeviceActivityCenter.startCalls == startsBefore + 1, "a day-old usage registration is refreshed")
check(DeviceActivityCenter.registrations[staleActivity]?.values.first?.includesPastActivity == true,
      "refreshed registration still counts existing period usage")
monitor.eventDidReachThreshold(.init("usage_limit_\(stale.id.uuidString)"), activity: staleActivity)
check(shielded(stale), "refreshed registration still blocks on threshold")
startsBefore = DeviceActivityCenter.startCalls
service.evaluateActiveGroups([stale])
check(DeviceActivityCenter.startCalls == startsBefore, "refresh resets the staleness clock")
check(shielded(stale), "refresh does not drop the current-period shield")
_ = service.deactivateGroup(stale)
check(storage.date(forKey: "zen_usage_registered_\(stale.id.uuidString)") == nil, "stop clears the registration stamp")

let accountability = AccountabilityManager()
CooldownService.minutes = 1
let cooling = group()
try service.activateGroup(cooling)
monitor.eventDidReachThreshold(.init("usage_limit_\(cooling.id.uuidString)"), activity: .init(cooling.id.uuidString))
check(shielded(cooling), "cooling group starts shielded")
let unlocksAt = accountability.requestUnlock(group: cooling)
let releaseActivity = CooldownRelease.groupActivity(cooling.id.uuidString)
let releaseSchedule = DeviceActivityCenter.schedules[releaseActivity]
check(releaseSchedule != nil, "stop request registers a release activity for the extension")
if let releaseSchedule {
    let start = Calendar.current.date(from: releaseSchedule.intervalStart)!
    let end = Calendar.current.date(from: releaseSchedule.intervalEnd)!
    check(end.timeIntervalSince(start) >= 15 * 60 - 1, "release activity satisfies the 15-minute minimum")
    check(abs(end.timeIntervalSince(unlocksAt)) < 1, "release activity ends when the cool-down ends")
}
monitor.intervalDidStart(for: releaseActivity)
check(shielded(cooling), "release callback before the cool-down ends keeps apps blocked")
check(CooldownRelease.releaseGroupIfElapsed() == nil, "foreground does not release an unfinished cool-down")
accountability.cancelPendingUnlock()
check(DeviceActivityCenter.schedules[releaseActivity] == nil, "keep focusing cancels the release activity")
check(shielded(cooling), "keep focusing keeps apps blocked")
_ = accountability.requestUnlock(group: cooling)
PendingUnlock(groupId: cooling.id.uuidString, groupName: cooling.name,
              requestedAt: Date().addingTimeInterval(-120), unlocksAt: Date().addingTimeInterval(-60)).save()
monitor.intervalDidEnd(for: releaseActivity)
check(!shielded(cooling), "extension releases the shield when the cool-down ends")
check(!storage.isGroupActive(cooling.id.uuidString), "extension marks the group inactive for other extensions")
check(storage.loadGroups().first(where: { $0.id == cooling.id.uuidString })?.isActive == false, "shared group record is inactive after release")
check(PendingUnlock.load() == nil, "release consumes the pending unlock")
check(DeviceActivityCenter.registrations[.init(cooling.id.uuidString)] == nil, "release stops the group's monitoring")
monitor.eventDidReachThreshold(.init("usage_limit_\(cooling.id.uuidString)"), activity: .init(cooling.id.uuidString))
check(!shielded(cooling), "late threshold cannot re-shield a released group")
check(cooling.isActive, "model still believes the group is active until the app reconciles")
service.applyReleasedCooldowns([cooling])
check(!cooling.isActive, "foreground reconciliation turns the released group off")
service.evaluateActiveGroups([cooling])
check(!shielded(cooling), "foreground re-evaluation does not re-shield a released group")
check(CooldownRelease.takeCompletedUnlocks().isEmpty, "completed unlocks are consumed once")

let missed = group()
try service.activateGroup(missed)
monitor.eventDidReachThreshold(.init("usage_limit_\(missed.id.uuidString)"), activity: .init(missed.id.uuidString))
_ = accountability.requestUnlock(group: missed)
PendingUnlock(groupId: missed.id.uuidString, groupName: missed.name,
              requestedAt: Date().addingTimeInterval(-120), unlocksAt: Date().addingTimeInterval(-60)).save()
service.applyReleasedCooldowns([missed])
check(!missed.isActive && !shielded(missed), "foreground releases an elapsed cool-down the extension never delivered")
service.evaluateActiveGroups([missed])
check(!shielded(missed), "re-evaluation after a missed release keeps apps unlocked")

let quickStore = ManagedSettingsStore(named: .init(Constants.quickFocusActivity))
quickStore.shield.applications = ["app"]
struct QuickSession: Codable { var endsAt: Date; var appCount: Int; var catCount: Int; var cooldownEndsAt: Date? }
var quick = QuickSession(endsAt: Date().addingTimeInterval(3600), appCount: 1, catCount: 0, cooldownEndsAt: Date().addingTimeInterval(60))
defaults.set(try! JSONEncoder().encode(quick), forKey: Constants.Keys.quickFocusSession)
monitor.intervalDidEnd(for: .init(CooldownRelease.quickFocusActivity))
check(ManagedSettingsStore(named: .init(Constants.quickFocusActivity)).shield.applications == ["app"], "quick focus stays blocked before its cool-down ends")
quick.cooldownEndsAt = Date().addingTimeInterval(-1)
defaults.set(try! JSONEncoder().encode(quick), forKey: Constants.Keys.quickFocusSession)
monitor.intervalDidEnd(for: .init(CooldownRelease.quickFocusActivity))
check(ManagedSettingsStore(named: .init(Constants.quickFocusActivity)).shield.applications == nil, "quick focus unlocks when its cool-down ends")
check(defaults.data(forKey: Constants.Keys.quickFocusSession) == nil, "quick focus session is cleared on release")
check(defaults.object(forKey: Constants.Keys.quickFocusReleasedAt) is Date, "quick focus release time is recorded for history")

let expiredUsage = group()
try service.activateGroup(expiredUsage)
let expiredActivity = DeviceActivityName(expiredUsage.id.uuidString)
monitor.eventDidReachThreshold(.init("usage_limit_\(expiredUsage.id.uuidString)"), activity: expiredActivity)
check(!ShieldExpiry.isStale(groupId: expiredUsage.id.uuidString), "a live usage block is not stale")
check(!ShieldExpiry.releaseIfStale(groupId: expiredUsage.id.uuidString), "shield tap keeps a live usage block")
check(shielded(expiredUsage), "live usage block stays shielded after a tap")
UsageBlockState.record(expiredUsage.id.uuidString, period: .hourly, at: Date().addingTimeInterval(-7200))
check(ShieldExpiry.isStale(groupId: expiredUsage.id.uuidString), "an expired usage period is stale")
check(ShieldExpiry.releaseIfStale(groupId: expiredUsage.id.uuidString), "shield tap releases an expired usage block")
check(!shielded(expiredUsage), "expired usage shield is cleared on tap")
check(storage.isGroupActive(expiredUsage.id.uuidString), "releasing an expired period keeps the session armed for next hour")
monitor.eventDidReachThreshold(.init("usage_limit_\(expiredUsage.id.uuidString)"), activity: expiredActivity)
check(shielded(expiredUsage), "next threshold blocks again after a tap release")

let orphan = group()
try service.activateGroup(orphan)
ShieldManager().applyShield(for: orphan.toShared(), selection: orphan.decodedSelection!)
orphan.isActive = false
service.syncGroupToAppGroups(orphan)
check(ShieldExpiry.isStale(groupId: orphan.id.uuidString), "a shield for an inactive session is stale")
check(ShieldExpiry.releaseIfStale(groupId: orphan.id.uuidString), "shield tap releases an inactive session's shield")
check(!shielded(orphan), "inactive session shield is cleared")
check(ShieldExpiry.isStale(groupId: "missing-group"), "a shield for an unknown session is stale")

let tapped = group()
try service.activateGroup(tapped)
monitor.eventDidReachThreshold(.init("usage_limit_\(tapped.id.uuidString)"), activity: .init(tapped.id.uuidString))
_ = accountability.requestUnlock(group: tapped)
check(!ShieldExpiry.isStale(groupId: tapped.id.uuidString), "a running cool-down keeps the shield")
PendingUnlock(groupId: tapped.id.uuidString, groupName: tapped.name,
              requestedAt: Date().addingTimeInterval(-120), unlocksAt: Date().addingTimeInterval(-60)).save()
check(ShieldExpiry.isStale(groupId: tapped.id.uuidString), "an elapsed cool-down is stale")
check(ShieldExpiry.releaseIfStale(groupId: tapped.id.uuidString), "shield tap releases an elapsed cool-down")
check(!shielded(tapped) && !storage.isGroupActive(tapped.id.uuidString), "tap release after cool-down turns the session off")
check(CooldownRelease.takeCompletedUnlocks() == [tapped.id.uuidString], "tap release is reported back to the app")

let window = BlockGroup(name: "Night", blockMode: .timeBased)
window.decodedSelection = FamilyActivitySelection(applicationTokens: ["app"], categoryTokens: [])
let hourNow = Calendar.current.component(.hour, from: Date())
window.scheduleStartHour = (hourNow + 2) % 24
window.scheduleStartMinute = 0
window.scheduleEndHour = (hourNow + 3) % 24
window.scheduleEndMinute = 0
window.scheduleRepeats = true
window.scheduleDaysOfWeek = Array(1...7)
window.isActive = true
service.syncGroupToAppGroups(window)
ShieldManager().applyShield(for: window.toShared(), selection: window.decodedSelection!)
check(ShieldExpiry.isStale(groupId: window.id.uuidString), "a time window shield outside its window is stale")
check(ShieldExpiry.releaseIfStale(groupId: window.id.uuidString), "shield tap releases a finished time window")
check(!shielded(window) && storage.isGroupActive(window.id.uuidString), "finished window is unshielded but stays scheduled")
window.scheduleStartHour = (hourNow + 23) % 24
window.scheduleEndHour = (hourNow + 1) % 24
service.syncGroupToAppGroups(window)
ShieldManager().applyShield(for: window.toShared(), selection: window.decodedSelection!)
check(!ShieldExpiry.isStale(groupId: window.id.uuidString), "a live time window keeps its shield")
check(shielded(window), "live window shield untouched by stale check")

let quickStoreName = ManagedSettingsStore.Name(Constants.quickFocusActivity)
ManagedSettingsStore(named: quickStoreName).shield.applications = ["app"]
defaults.removeObject(forKey: Constants.Keys.quickFocusSession)
check(ShieldExpiry.quickFocusIsStale(), "a quick focus shield without a session is stale")
check(ShieldExpiry.releaseQuickFocusIfStale(), "shield tap clears an orphaned quick focus shield")
check(ManagedSettingsStore(named: quickStoreName).shield.applications == nil, "orphaned quick focus shield cleared")
ManagedSettingsStore(named: quickStoreName).shield.applications = ["app"]
quick = QuickSession(endsAt: Date().addingTimeInterval(600), appCount: 1, catCount: 0, cooldownEndsAt: nil)
defaults.set(try! JSONEncoder().encode(quick), forKey: Constants.Keys.quickFocusSession)
check(!ShieldExpiry.quickFocusIsStale(), "a running quick focus keeps its shield")
check(!ShieldExpiry.releaseQuickFocusIfStale(), "shield tap keeps a running quick focus")
quick.endsAt = Date().addingTimeInterval(-5)
defaults.set(try! JSONEncoder().encode(quick), forKey: Constants.Keys.quickFocusSession)
check(ShieldExpiry.quickFocusIsStale(), "an ended quick focus is stale")
check(ShieldExpiry.releaseQuickFocusIfStale(), "shield tap releases an ended quick focus")
check(ManagedSettingsStore(named: quickStoreName).shield.applications == nil && defaults.data(forKey: Constants.Keys.quickFocusSession) == nil,
      "ended quick focus is cleared with its session")

let sweeper = group()
try service.activateGroup(sweeper)
monitor.eventDidReachThreshold(.init("usage_limit_\(sweeper.id.uuidString)"), activity: .init(sweeper.id.uuidString))
UsageBlockState.record(sweeper.id.uuidString, period: .hourly, at: Date().addingTimeInterval(-7200))
let neighbour = group()
try service.activateGroup(neighbour)
monitor.intervalDidStart(for: .init(neighbour.id.uuidString))
check(!shielded(sweeper), "any monitor callback sweeps other sessions' expired shields")
monitor.eventDidReachThreshold(.init("usage_limit_\(neighbour.id.uuidString)"), activity: .init(neighbour.id.uuidString))
check(shielded(neighbour), "sweep does not disturb a fresh threshold on the triggering session")

let idle = group()
try service.activateGroup(idle)
check(!ShieldExpiry.releaseIfStale(groupId: idle.id.uuidString), "an unshielded armed session reports nothing to release")
defaults.set(["other-group", idle.id.uuidString], forKey: CooldownRelease.completedKey)
CooldownRelease.consumeCompletedUnlock(idle.id.uuidString)
check(CooldownRelease.takeCompletedUnlocks() == ["other-group"], "consuming one completed unlock keeps the others")

func timedGroup(startOffset: Int, endOffset: Int) -> BlockGroup {
    let value = BlockGroup(name: "Timed", blockMode: .timeBased)
    value.decodedSelection = FamilyActivitySelection(applicationTokens: ["app"], categoryTokens: [])
    let hour = Calendar.current.component(.hour, from: Date())
    value.scheduleStartHour = (hour + startOffset + 24) % 24
    value.scheduleStartMinute = 0
    value.scheduleEndHour = (hour + endOffset + 24) % 24
    value.scheduleEndMinute = 0
    value.scheduleRepeats = true
    value.scheduleDaysOfWeek = Array(1...7)
    return value
}
let liveWindow = timedGroup(startOffset: -1, endOffset: 2)
try service.activateGroup(liveWindow)
check(shielded(liveWindow), "time window inside its hours shields on activation")
_ = accountability.requestUnlock(group: liveWindow)
let liveRelease = CooldownRelease.groupActivity(liveWindow.id.uuidString)
check(DeviceActivityCenter.schedules[liveRelease] != nil, "time window stop registers release activity")
service.evaluateActiveGroups([liveWindow])
check(DeviceActivityCenter.schedules[liveRelease] != nil, "foreground re-registration keeps the cool-down release activity alive")
check(shielded(liveWindow), "foreground keeps apps blocked during an unfinished cool-down")
monitor.intervalDidEnd(for: .init(liveWindow.id.uuidString))
check(shielded(liveWindow), "an end callback triggered by re-registration does not unshield a live window")
accountability.cancelPendingUnlock()
_ = service.deactivateGroup(liveWindow)
check(DeviceActivityCenter.schedules[liveRelease] == nil, "turning a session off removes its release activity")

let endedWindow = timedGroup(startOffset: -3, endOffset: -1)
endedWindow.isActive = true
service.syncGroupToAppGroups(endedWindow)
ShieldManager().applyShield(for: endedWindow.toShared(), selection: endedWindow.decodedSelection!)
monitor.intervalDidEnd(for: .init(endedWindow.id.uuidString))
check(!shielded(endedWindow), "a genuine end callback clears a finished window")

let deletedCooling = group()
try service.activateGroup(deletedCooling)
_ = accountability.requestUnlock(group: deletedCooling)
service.removeGroupFromAppGroups(deletedCooling.id.uuidString)
check(PendingUnlock.load() == nil, "deleting a cooling session clears its pending stop")
check(DeviceActivityCenter.schedules[CooldownRelease.groupActivity(deletedCooling.id.uuidString)] == nil, "deleting a cooling session removes its release activity")

ManagedSettingsStore(named: quickStoreName).shield.applications = ["app"]
quick = QuickSession(endsAt: Date().addingTimeInterval(1800), appCount: 1, catCount: 0, cooldownEndsAt: nil)
defaults.set(try! JSONEncoder().encode(quick), forKey: Constants.Keys.quickFocusSession)
monitor.intervalDidEnd(for: .init(Constants.quickFocusActivity))
check(ManagedSettingsStore(named: quickStoreName).shield.applications == ["app"], "an end callback from extending Quick Focus keeps the extended session blocked")
quick.endsAt = Date().addingTimeInterval(30)
defaults.set(try! JSONEncoder().encode(quick), forKey: Constants.Keys.quickFocusSession)
monitor.intervalDidEnd(for: .init(Constants.quickFocusActivity))
check(ManagedSettingsStore(named: quickStoreName).shield.applications == ["app"], "an end callback 30 seconds early does not unlock Quick Focus")
quick.endsAt = Date().addingTimeInterval(2)
defaults.set(try! JSONEncoder().encode(quick), forKey: Constants.Keys.quickFocusSession)
monitor.intervalDidEnd(for: .init(Constants.quickFocusActivity))
check(ManagedSettingsStore(named: quickStoreName).shield.applications == nil, "an end callback within seconds of the deadline releases Quick Focus")

let overnight = timedGroup(startOffset: -3, endOffset: -1)
overnight.isActive = true
service.syncGroupToAppGroups(overnight)
for suffix in ["-A", "-B"] {
    ManagedSettingsStore(named: .init(overnight.id.uuidString + suffix)).shield.applications = ["app"]
}
monitor.intervalDidEnd(for: .init(overnight.id.uuidString + "-B"))
let leftover = ["", "-A", "-B"].contains { ManagedSettingsStore(named: .init(overnight.id.uuidString + $0)).shield.applications != nil }
check(!leftover, "the final overnight end callback clears every store for the window")

let shortQuick = CooldownRelease.paddedSchedule(endingAt: Date().addingTimeInterval(600))
let shortStart = Calendar.current.date(from: shortQuick.intervalStart)!
let shortEnd = Calendar.current.date(from: shortQuick.intervalEnd)!
check(shortEnd.timeIntervalSince(shortStart) >= 15 * 60 - 1, "a 10-minute Quick Focus registers a valid 15-minute interval")
check(abs(shortEnd.timeIntervalSinceNow - 600) < 2, "padded schedule still ends at the session deadline")

print("\(checks) checks, \(failures) failures")
defaults.removePersistentDomain(forName: Constants.appGroupID)
exit(failures == 0 ? 0 : 1)
