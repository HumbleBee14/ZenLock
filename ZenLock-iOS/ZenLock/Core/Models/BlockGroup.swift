import Foundation
import SwiftData
import FamilyControls

@Model
final class BlockGroup {
    @Attribute(.unique) var id: UUID
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

    /// Fire a heads-up notification one minute before the schedule starts.
    var notifyBeforeStart: Bool = false

    var deepFocusEnabled: Bool
    var customShieldMessage: String?

    var maxOpensPerDay: Int?
    var maxMinutesPerOpen: Int?

    var createdAt: Date
    var updatedAt: Date

    init(
        id: UUID = UUID(),
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
        self.createdAt = Date()
        self.updatedAt = Date()
    }

    var decodedSelection: FamilyActivitySelection? {
        get { SelectionCoder.decode(selectionData) }
        set {
            selectionData = SelectionCoder.encode(newValue)
            updatedAt = Date()
        }
    }

    func toShared() -> SharedBlockGroup {
        SharedBlockGroup(
            id: id.uuidString,
            name: name,
            icon: icon,
            colorHex: colorHex,
            blockMode: blockMode,
            isActive: isActive,
            selectionData: selectionData,
            usageLimitMinutes: usageLimitMinutes,
            usagePeriod: usagePeriod,
            scheduleStartHour: scheduleStartHour,
            scheduleStartMinute: scheduleStartMinute,
            scheduleEndHour: scheduleEndHour,
            scheduleEndMinute: scheduleEndMinute,
            scheduleDaysOfWeek: scheduleDaysOfWeek,
            scheduleRepeats: scheduleRepeats,
            notifyBeforeStart: notifyBeforeStart,
            deepFocusEnabled: deepFocusEnabled,
            customShieldMessage: customShieldMessage,
            maxOpensPerDay: maxOpensPerDay,
            maxMinutesPerOpen: maxMinutesPerOpen
        )
    }
}
