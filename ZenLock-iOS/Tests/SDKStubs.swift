import Foundation

struct FamilyActivitySelection: Codable {
    var applicationTokens: Set<String> = []
    var categoryTokens: Set<String> = []
}
struct DeviceActivityName: Hashable {
    let rawValue: String
    init(_ value: String) { rawValue = value }
}
struct DeviceActivitySchedule {
    let intervalStart: DateComponents
    let intervalEnd: DateComponents
    let repeats: Bool
    var warningTime: DateComponents? = nil
}
struct DeviceActivityEvent {
    struct Name: Hashable {
        let rawValue: String
        init(_ value: String) { rawValue = value }
    }
    let applications: Set<String>
    let categories: Set<String>
    let threshold: DateComponents
    var includesPastActivity: Bool = false
}
struct DeviceActivityCenter {
    static var registrations: [DeviceActivityName: [DeviceActivityEvent.Name: DeviceActivityEvent]] = [:]
    static var schedules: [DeviceActivityName: DeviceActivitySchedule] = [:]
    static var startCalls = 0
    static var failure: Error?
    var activities: [DeviceActivityName] { Array(Self.registrations.keys) }
    func startMonitoring(_ name: DeviceActivityName, during schedule: DeviceActivitySchedule,
                         events: [DeviceActivityEvent.Name: DeviceActivityEvent] = [:]) throws {
        Self.startCalls += 1
        if let error = Self.failure { throw error }
        Self.registrations[name] = events
        Self.schedules[name] = schedule
    }
    func stopMonitoring(_ names: [DeviceActivityName]? = nil) {
        for name in names ?? activities {
            Self.registrations.removeValue(forKey: name)
            Self.schedules.removeValue(forKey: name)
        }
    }
}
class DeviceActivityMonitor {
    func intervalDidStart(for activity: DeviceActivityName) {}
    func intervalDidEnd(for activity: DeviceActivityName) {}
    func eventDidReachThreshold(_ event: DeviceActivityEvent.Name, activity: DeviceActivityName) {}
    func eventWillReachThresholdWarning(_ event: DeviceActivityEvent.Name, activity: DeviceActivityName) {}
}
final class ManagedSettingsStore {
    struct Name: Hashable {
        let rawValue: String
        init(_ value: String) { rawValue = value }
    }
    enum Policy { case specific(Set<String>) }
    final class Shield {
        var applications: Set<String>?
        var applicationCategories: Policy?
    }
    static var shields: [Name: Shield] = [:]
    let name: Name
    init(named name: Name) { self.name = name }
    var shield: Shield {
        if let value = Self.shields[name] { return value }
        let value = Shield()
        Self.shields[name] = value
        return value
    }
    func clearAllSettings() { Self.shields[name] = Shield() }
}
final class UNMutableNotificationContent {
    var title = ""
    var body = ""
    var sound: Sound?
    enum Sound { case `default` }
}
struct UNNotificationRequest {
    let identifier: String
    let content: UNMutableNotificationContent
    let trigger: String?
}
struct UNUserNotificationCenter {
    static var requests: [UNNotificationRequest] = []
    static func current() -> Self { Self() }
    func add(_ request: UNNotificationRequest) { Self.requests.append(request) }
}
struct ScheduleNotifier {
    func cancelStartNotification(groupId: String) {}
    func scheduleHeadsUpNotification(groupId: String, groupName: String, hour: Int, minute: Int, repeats: Bool) {}
}

struct TestModelContext { func save() throws {} }
struct ZenToastData {
    enum Kind { case warning }
    let message: String
    let kind: Kind
}
enum SessionLedger { static func reconcile(context: TestModelContext) {} }
