import Foundation
import FamilyControls

struct GroupDraft {
    var name: String = ""
    var icon: String = "lock.shield"
    var colorHex: String = "#7C3AED"
    var blockMode: BlockMode = .timeBased
    var selection: FamilyActivitySelection = FamilyActivitySelection()

    var scheduleStartHour: Int = 22
    var scheduleStartMinute: Int = 0
    var scheduleEndHour: Int = 6
    var scheduleEndMinute: Int = 0
    var scheduleRepeats: Bool = true
    var scheduleDays: Set<Int> = Set(1...7)
    var notifyBeforeStart: Bool = false

    var usageLimitMinutes: Int = 60
    var usagePeriod: UsagePeriod = .daily

    var deepFocusEnabled: Bool = false

    var hasSelectedApps: Bool {
        !selection.applicationTokens.isEmpty || !selection.categoryTokens.isEmpty
    }
}

extension GroupDraft {
    init(from group: BlockGroup) {
        self.name = group.name
        self.icon = group.icon
        self.colorHex = group.colorHex
        self.blockMode = group.blockMode
        self.selection = group.decodedSelection ?? FamilyActivitySelection()
        self.scheduleStartHour = group.scheduleStartHour ?? 22
        self.scheduleStartMinute = group.scheduleStartMinute ?? 0
        self.scheduleEndHour = group.scheduleEndHour ?? 6
        self.scheduleEndMinute = group.scheduleEndMinute ?? 0
        self.scheduleRepeats = group.scheduleRepeats
        self.scheduleDays = Set(group.scheduleDaysOfWeek ?? Array(1...7))
        self.notifyBeforeStart = group.notifyBeforeStart
        self.usagePeriod = group.usagePeriod ?? .daily
        self.usageLimitMinutes = usagePeriod.normalizedLimit(group.usageLimitMinutes ?? 60)
        self.deepFocusEnabled = group.deepFocusEnabled
    }

    /// Cosmetic edits must not reset usage accounting or clear an active shield.
    func canPreserveMonitoring(for group: BlockGroup) -> Bool {
        guard group.isActive else { return false }
        if group.toShared().isStrictLocked { return true }
        guard group.blockMode == .usageBased, blockMode == .usageBased,
              group.usagePeriod ?? .daily == usagePeriod,
              group.usageLimitMinutes == usagePeriod.normalizedLimit(usageLimitMinutes),
              let previous = group.decodedSelection else { return false }
        return previous.applicationTokens == selection.applicationTokens
            && previous.categoryTokens == selection.categoryTokens
    }

    func apply(to group: BlockGroup) {
        group.name = name
        group.icon = icon
        group.colorHex = colorHex
        group.blockMode = blockMode
        group.decodedSelection = selection
        group.scheduleStartHour = scheduleStartHour
        group.scheduleStartMinute = scheduleStartMinute
        group.scheduleEndHour = scheduleEndHour
        group.scheduleEndMinute = scheduleEndMinute
        group.scheduleRepeats = scheduleRepeats
        group.scheduleDaysOfWeek = scheduleRepeats ? Array(scheduleDays).sorted() : nil
        group.notifyBeforeStart = notifyBeforeStart
        group.usageLimitMinutes = usagePeriod.normalizedLimit(usageLimitMinutes)
        group.usagePeriod = usagePeriod
        group.deepFocusEnabled = deepFocusEnabled
        group.updatedAt = Date()
    }

    func applyLockedChanges(to group: BlockGroup) {
        group.name = name
        group.icon = icon
        group.colorHex = colorHex
        group.updatedAt = Date()
    }

    func makeGroup() -> BlockGroup {
        let g = BlockGroup(name: name, icon: icon, colorHex: colorHex, blockMode: blockMode)
        apply(to: g)
        return g
    }
}
