import Foundation
import SwiftData
import FamilyControls

@Model
final class BlockGroup {
    @Attribute(.unique) var id: UUID
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

    var createdAt: Date
    var updatedAt: Date

    init(
        id: UUID = UUID(),
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
            blockMode: blockMode,
            isActive: isActive,
            selectionData: selectionData,
            usageLimitMinutes: usageLimitMinutes,
            usagePeriod: usagePeriod,
            frictionDelaySeconds: frictionDelaySeconds,
            frictionType: frictionType,
            progressiveDelay: progressiveDelay,
            scheduleStartHour: scheduleStartHour,
            scheduleStartMinute: scheduleStartMinute,
            scheduleEndHour: scheduleEndHour,
            scheduleEndMinute: scheduleEndMinute,
            scheduleDaysOfWeek: scheduleDaysOfWeek,
            scheduleRepeats: scheduleRepeats,
            webFilterEnabled: webFilterEnabled,
            blockAdultContent: blockAdultContent,
            deepFocusEnabled: deepFocusEnabled,
            customShieldMessage: customShieldMessage
        )
    }
}
