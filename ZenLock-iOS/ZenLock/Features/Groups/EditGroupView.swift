import SwiftUI
import SwiftData
import FamilyControls

struct EditGroupView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    @Bindable var group: BlockGroup
    @State private var selection: FamilyActivitySelection
    @State private var showAppPicker = false
    @State private var scheduleDays: Set<Int>

    init(group: BlockGroup) {
        self.group = group
        let decoded = group.decodedSelection ?? FamilyActivitySelection()
        _selection = State(initialValue: decoded)
        _scheduleDays = State(initialValue: Set(group.scheduleDaysOfWeek ?? Array(1...7)))
    }

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: ZenTheme.Spacing.lg) {
                        identityCard
                        appsCard
                        modeSpecificCard
                        flagsCard
                        deleteCard
                    }
                    .padding(ZenTheme.Spacing.md)
                }
            }
            .navigationTitle(group.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { save() }
                        .foregroundStyle(ZenTheme.primary)
                }
            }
            .familyActivityPicker(isPresented: $showAppPicker, selection: $selection)
        }
    }

    private var identityCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("Name")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
                TextField("", text: $group.name)
                    .font(ZenTheme.body)
                    .foregroundStyle(ZenTheme.text)

                HStack {
                    GroupIcon(systemName: group.icon, color: Color(hex: group.colorHex), size: 48)
                    Spacer()
                    Picker("", selection: $group.icon) {
                        ForEach(["lock.shield", "moon.fill", "briefcase.fill", "book.fill", "gamecontroller.fill"], id: \.self) { sym in
                            Image(systemName: sym).tag(sym)
                        }
                    }
                    .pickerStyle(.menu)
                    .tint(ZenTheme.primary)
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var appsCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.sm) {
                HStack {
                    Text("Apps")
                        .font(ZenTheme.headline)
                        .foregroundStyle(ZenTheme.text)
                    Spacer()
                    Text(selectionSummary)
                        .font(ZenTheme.caption)
                        .foregroundStyle(ZenTheme.textSecondary)
                }
                ZenButton(title: "Re-select Apps", icon: "square.and.pencil", style: .secondary) {
                    showAppPicker = true
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    @ViewBuilder
    private var modeSpecificCard: some View {
        switch group.blockMode {
        case .timeBased:
            GlassCard {
                VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                    Text("Schedule").font(ZenTheme.headline).foregroundStyle(ZenTheme.text)
                    HStack(spacing: ZenTheme.Spacing.md) {
                        Stepper("Start \(formatted(group.scheduleStartHour, group.scheduleStartMinute))",
                                value: Binding(get: { group.scheduleStartHour ?? 22 },
                                               set: { group.scheduleStartHour = $0 }), in: 0...23)
                            .foregroundStyle(ZenTheme.text)
                    }
                    HStack(spacing: ZenTheme.Spacing.md) {
                        Stepper("End \(formatted(group.scheduleEndHour, group.scheduleEndMinute))",
                                value: Binding(get: { group.scheduleEndHour ?? 6 },
                                               set: { group.scheduleEndHour = $0 }), in: 0...23)
                            .foregroundStyle(ZenTheme.text)
                    }
                }
                .padding(ZenTheme.Spacing.md)
            }
        case .usageBased:
            GlassCard {
                VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                    Text("Usage Limit").font(ZenTheme.headline).foregroundStyle(ZenTheme.text)
                    Stepper("\(group.usageLimitMinutes ?? 60) minutes",
                            value: Binding(get: { group.usageLimitMinutes ?? 60 },
                                           set: { group.usageLimitMinutes = $0 }),
                            in: 5...480, step: 5)
                        .foregroundStyle(ZenTheme.text)
                }
                .padding(ZenTheme.Spacing.md)
            }
        case .frictionBased:
            GlassCard {
                VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                    Text("Friction").font(ZenTheme.headline).foregroundStyle(ZenTheme.text)
                    Stepper("\(group.frictionDelaySeconds ?? 10)s base delay",
                            value: Binding(get: { group.frictionDelaySeconds ?? 10 },
                                           set: { group.frictionDelaySeconds = $0 }),
                            in: 3...60)
                        .foregroundStyle(ZenTheme.text)
                    ZenToggle(isOn: $group.progressiveDelay, label: "Progressive Delay")
                }
                .padding(ZenTheme.Spacing.md)
            }
        }
    }

    private var flagsCard: some View {
        GlassCard {
            VStack(spacing: ZenTheme.Spacing.sm) {
                ZenToggle(isOn: $group.deepFocusEnabled, label: "🔒 Deep Focus")
                ZenToggle(isOn: $group.webFilterEnabled, label: "Block Websites")
                if group.webFilterEnabled {
                    ZenToggle(isOn: $group.blockAdultContent, label: "Adult Content Filter")
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var deleteCard: some View {
        ZenButton(title: "Delete Group", icon: "trash", style: .destructive) {
            modelContext.delete(group)
            try? modelContext.save()
            BlockingService().removeGroupFromAppGroups(group.id.uuidString)
            dismiss()
        }
    }

    private var selectionSummary: String {
        let a = selection.applicationTokens.count
        let c = selection.categoryTokens.count
        if a == 0 && c == 0 { return "None" }
        return "\(a) apps · \(c) categories"
    }

    private func formatted(_ h: Int?, _ m: Int?) -> String {
        String(format: "%02d:%02d", h ?? 0, m ?? 0)
    }

    private func save() {
        group.decodedSelection = selection
        group.scheduleDaysOfWeek = group.scheduleRepeats ? Array(scheduleDays).sorted() : nil
        try? modelContext.save()
        BlockingService().syncGroupToAppGroups(group)
        dismiss()
    }
}
