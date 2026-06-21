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

    var enableOpenLimit: Bool = false
    var maxOpensPerDay: Int = 5
    var enableSessionLimit: Bool = false
    var maxMinutesPerOpen: Int = 5

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
        self.usageLimitMinutes = group.usageLimitMinutes ?? 60
        self.usagePeriod = group.usagePeriod ?? .daily
        self.enableOpenLimit = group.maxOpensPerDay != nil
        self.maxOpensPerDay = group.maxOpensPerDay ?? 5
        self.enableSessionLimit = group.maxMinutesPerOpen != nil
        self.maxMinutesPerOpen = group.maxMinutesPerOpen ?? 5
        self.deepFocusEnabled = group.deepFocusEnabled
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
        group.usageLimitMinutes = usageLimitMinutes
        group.usagePeriod = usagePeriod
        group.maxOpensPerDay = enableOpenLimit ? maxOpensPerDay : nil
        group.maxMinutesPerOpen = enableSessionLimit ? maxMinutesPerOpen : nil
        group.deepFocusEnabled = deepFocusEnabled
        group.updatedAt = Date()
    }

    /// Applies only the changes that are safe to make while a Strict Mode
    /// session is actively enforcing. This is the real enforcement boundary —
    /// the UI also disables these controls, but a frozen session must never be
    /// editable away even if the UI is bypassed.
    ///
    /// Allowed: rename + cosmetic (icon/color) only.
    /// Preserved (untouched): apps, mode, schedule, limits, and the Strict Mode
    /// flag — so nothing about the active block can be changed until it ends.
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
