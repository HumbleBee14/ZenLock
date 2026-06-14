import Foundation

struct SharedBlockGroup: Codable, Identifiable, Sendable {
    let id: String
    var name: String
    var icon: String
    var colorHex: String
    var blockMode: BlockMode
    var isActive: Bool

    var selectionData: Data?

    var usageLimitMinutes: Int?
    var usagePeriod: UsagePeriod?

    var scheduleStartHour: Int?
    var scheduleStartMinute: Int?
    var scheduleEndHour: Int?
    var scheduleEndMinute: Int?
    var scheduleDaysOfWeek: [Int]?
    var scheduleRepeats: Bool
    var notifyBeforeStart: Bool

    var deepFocusEnabled: Bool
    var customShieldMessage: String?

    var maxOpensPerDay: Int?
    var maxMinutesPerOpen: Int?

    init(
        id: String = UUID().uuidString,
        name: String,
        icon: String = "lock.shield",
        colorHex: String = "#7C3AED",
        blockMode: BlockMode = .timeBased,
        isActive: Bool = false,
        selectionData: Data? = nil,
        usageLimitMinutes: Int? = nil,
        usagePeriod: UsagePeriod? = nil,
        scheduleStartHour: Int? = nil,
        scheduleStartMinute: Int? = nil,
        scheduleEndHour: Int? = nil,
        scheduleEndMinute: Int? = nil,
        scheduleDaysOfWeek: [Int]? = nil,
        scheduleRepeats: Bool = false,
        notifyBeforeStart: Bool = false,
        deepFocusEnabled: Bool = false,
        customShieldMessage: String? = nil,
        maxOpensPerDay: Int? = nil,
        maxMinutesPerOpen: Int? = nil
    ) {
        self.id = id
        self.name = name
        self.icon = icon
        self.colorHex = colorHex
        self.blockMode = blockMode
        self.isActive = isActive
        self.selectionData = selectionData
        self.usageLimitMinutes = usageLimitMinutes
        self.usagePeriod = usagePeriod
        self.scheduleStartHour = scheduleStartHour
        self.scheduleStartMinute = scheduleStartMinute
        self.scheduleEndHour = scheduleEndHour
        self.scheduleEndMinute = scheduleEndMinute
        self.scheduleDaysOfWeek = scheduleDaysOfWeek
        self.scheduleRepeats = scheduleRepeats
        self.notifyBeforeStart = notifyBeforeStart
        self.deepFocusEnabled = deepFocusEnabled
        self.customShieldMessage = customShieldMessage
        self.maxOpensPerDay = maxOpensPerDay
        self.maxMinutesPerOpen = maxMinutesPerOpen
    }
}

// MARK: - Forward-compatible decoding

extension SharedBlockGroup {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        self.id = try c.decode(String.self, forKey: .id)
        self.name = try c.decode(String.self, forKey: .name)
        self.icon = try c.decodeIfPresent(String.self, forKey: .icon) ?? "lock.shield"
        self.colorHex = try c.decodeIfPresent(String.self, forKey: .colorHex) ?? "#7C3AED"
        self.blockMode = try c.decode(BlockMode.self, forKey: .blockMode)
        self.isActive = try c.decode(Bool.self, forKey: .isActive)
        self.selectionData = try c.decodeIfPresent(Data.self, forKey: .selectionData)
        self.usageLimitMinutes = try c.decodeIfPresent(Int.self, forKey: .usageLimitMinutes)
        self.usagePeriod = try c.decodeIfPresent(UsagePeriod.self, forKey: .usagePeriod)
        self.scheduleStartHour = try c.decodeIfPresent(Int.self, forKey: .scheduleStartHour)
        self.scheduleStartMinute = try c.decodeIfPresent(Int.self, forKey: .scheduleStartMinute)
        self.scheduleEndHour = try c.decodeIfPresent(Int.self, forKey: .scheduleEndHour)
        self.scheduleEndMinute = try c.decodeIfPresent(Int.self, forKey: .scheduleEndMinute)
        self.scheduleDaysOfWeek = try c.decodeIfPresent([Int].self, forKey: .scheduleDaysOfWeek)
        self.scheduleRepeats = try c.decodeIfPresent(Bool.self, forKey: .scheduleRepeats) ?? false
        self.notifyBeforeStart = try c.decodeIfPresent(Bool.self, forKey: .notifyBeforeStart) ?? false
        self.deepFocusEnabled = try c.decodeIfPresent(Bool.self, forKey: .deepFocusEnabled) ?? false
        self.customShieldMessage = try c.decodeIfPresent(String.self, forKey: .customShieldMessage)
        self.maxOpensPerDay = try c.decodeIfPresent(Int.self, forKey: .maxOpensPerDay)
        self.maxMinutesPerOpen = try c.decodeIfPresent(Int.self, forKey: .maxMinutesPerOpen)
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
