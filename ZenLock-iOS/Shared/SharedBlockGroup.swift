import Foundation

struct SharedBlockGroup: Codable, Identifiable, Sendable {
    let id: String
    var name: String
    var blockMode: BlockMode
    var isActive: Bool

    var selectionData: Data?

    var usageLimitMinutes: Int?
    var usagePeriod: UsagePeriod?

    var frictionDelaySeconds: Int?
    var frictionType: FrictionType?
    var progressiveDelay: Bool

    var scheduleStartHour: Int?
    var scheduleStartMinute: Int?
    var scheduleEndHour: Int?
    var scheduleEndMinute: Int?
    var scheduleDaysOfWeek: [Int]?
    var scheduleRepeats: Bool

    var webFilterEnabled: Bool
    var blockAdultContent: Bool
    var deepFocusEnabled: Bool
    var customShieldMessage: String?

    init(
        id: String = UUID().uuidString,
        name: String,
        blockMode: BlockMode = .timeBased,
        isActive: Bool = false,
        selectionData: Data? = nil,
        usageLimitMinutes: Int? = nil,
        usagePeriod: UsagePeriod? = nil,
        frictionDelaySeconds: Int? = nil,
        frictionType: FrictionType? = nil,
        progressiveDelay: Bool = false,
        scheduleStartHour: Int? = nil,
        scheduleStartMinute: Int? = nil,
        scheduleEndHour: Int? = nil,
        scheduleEndMinute: Int? = nil,
        scheduleDaysOfWeek: [Int]? = nil,
        scheduleRepeats: Bool = false,
        webFilterEnabled: Bool = false,
        blockAdultContent: Bool = false,
        deepFocusEnabled: Bool = false,
        customShieldMessage: String? = nil
    ) {
        self.id = id
        self.name = name
        self.blockMode = blockMode
        self.isActive = isActive
        self.selectionData = selectionData
        self.usageLimitMinutes = usageLimitMinutes
        self.usagePeriod = usagePeriod
        self.frictionDelaySeconds = frictionDelaySeconds
        self.frictionType = frictionType
        self.progressiveDelay = progressiveDelay
        self.scheduleStartHour = scheduleStartHour
        self.scheduleStartMinute = scheduleStartMinute
        self.scheduleEndHour = scheduleEndHour
        self.scheduleEndMinute = scheduleEndMinute
        self.scheduleDaysOfWeek = scheduleDaysOfWeek
        self.scheduleRepeats = scheduleRepeats
        self.webFilterEnabled = webFilterEnabled
        self.blockAdultContent = blockAdultContent
        self.deepFocusEnabled = deepFocusEnabled
        self.customShieldMessage = customShieldMessage
    }
}

// MARK: - UserDefaults Persistence

extension SharedBlockGroup {
    static func save(_ groups: [SharedBlockGroup], to defaults: UserDefaults? = Constants.sharedDefaults) {
        guard let data = try? JSONEncoder().encode(groups) else { return }
        defaults?.set(data, forKey: Constants.Keys.blockGroups)
    }

    static func load(from defaults: UserDefaults? = Constants.sharedDefaults) -> [SharedBlockGroup] {
        guard let data = defaults?.data(forKey: Constants.Keys.blockGroups),
              let groups = try? JSONDecoder().decode([SharedBlockGroup].self, from: data) else {
            return []
        }
        return groups
    }
}
