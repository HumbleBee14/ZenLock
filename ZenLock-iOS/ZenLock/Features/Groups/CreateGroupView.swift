import SwiftUI
import SwiftData
import FamilyControls

struct CreateGroupView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var icon = "lock.shield"
    @State private var colorHex = "#7C3AED"
    @State private var blockMode: BlockMode = .timeBased
    @State private var selection = FamilyActivitySelection()
    @State private var showAppPicker = false

    private static let iconChoices = [
        "lock.shield", "moon.fill", "briefcase.fill", "book.fill",
        "figure.run", "leaf.fill", "person.fill", "gamecontroller.fill",
        "tv.fill", "message.fill", "cart.fill", "newspaper.fill"
    ]

    private static let colorChoices = [
        "#7C3AED", "#4F46E5", "#06B6D4", "#10B981",
        "#F59E0B", "#EF4444", "#EC4899", "#8B5CF6"
    ]

    @State private var scheduleStartHour = 22
    @State private var scheduleStartMinute = 0
    @State private var scheduleEndHour = 6
    @State private var scheduleEndMinute = 0
    @State private var scheduleRepeats = true

    @State private var usageLimitMinutes = 60
    @State private var usagePeriod: UsagePeriod = .daily

    @State private var frictionType: FrictionType = .breathing
    @State private var frictionDelay = 10
    @State private var progressiveDelay = false

    @State private var webFilterEnabled = false
    @State private var blockAdultContent = false

    var onCreated: () -> Void

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: ZenTheme.Spacing.lg) {
                        nameSection
                        identitySection
                        appSelectionSection
                        blockModeSection
                        modeConfigSection
                        webFilterSection
                        createButton
                    }
                    .padding(.horizontal, ZenTheme.Spacing.md)
                    .padding(.vertical, ZenTheme.Spacing.lg)
                }
            }
            .navigationTitle("New Group")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                        .foregroundStyle(ZenTheme.textSecondary)
                }
            }
            .familyActivityPicker(
                isPresented: $showAppPicker,
                selection: $selection
            )
        }
    }

    private var nameSection: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.sm) {
                Text("Group Name")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
                TextField("e.g., Social Media, Work Focus", text: $name)
                    .font(ZenTheme.body)
                    .foregroundStyle(ZenTheme.text)
                    .tint(ZenTheme.primary)
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var identitySection: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("Icon & Color")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)

                HStack(spacing: ZenTheme.Spacing.md) {
                    GroupIcon(systemName: icon, color: Color(hex: colorHex), size: 56)

                    VStack(alignment: .leading, spacing: ZenTheme.Spacing.sm) {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: ZenTheme.Spacing.sm) {
                                ForEach(Self.iconChoices, id: \.self) { symbol in
                                    Button {
                                        withAnimation(ZenTheme.springy) { icon = symbol }
                                    } label: {
                                        Image(systemName: symbol)
                                            .font(.system(size: 18, weight: .semibold))
                                            .foregroundStyle(icon == symbol ? Color(hex: colorHex) : ZenTheme.textSecondary)
                                            .frame(width: 36, height: 36)
                                            .background(
                                                Circle().fill(icon == symbol ? Color(hex: colorHex).opacity(0.18) : ZenTheme.surfaceLight.opacity(0.4))
                                            )
                                    }
                                }
                            }
                        }

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: ZenTheme.Spacing.sm) {
                                ForEach(Self.colorChoices, id: \.self) { hex in
                                    Button {
                                        withAnimation(ZenTheme.springy) { colorHex = hex }
                                    } label: {
                                        Circle()
                                            .fill(Color(hex: hex))
                                            .frame(width: 28, height: 28)
                                            .overlay(
                                                Circle()
                                                    .strokeBorder(Color.white.opacity(colorHex == hex ? 0.9 : 0), lineWidth: 2)
                                            )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var appSelectionSection: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                HStack {
                    GroupIcon(systemName: "app.badge", color: ZenTheme.accent)
                    VStack(alignment: .leading) {
                        Text("Apps to Block")
                            .font(ZenTheme.headline)
                            .foregroundStyle(ZenTheme.text)
                        Text(selectionSummary)
                            .font(ZenTheme.caption)
                            .foregroundStyle(ZenTheme.textSecondary)
                    }
                    Spacer()
                }
                ZenButton(title: "Select Apps", icon: "plus.app", style: .secondary) {
                    showAppPicker = true
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var blockModeSection: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("Blocking Mode")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)

                ForEach(BlockMode.allCases, id: \.self) { mode in
                    Button {
                        withAnimation(ZenTheme.springy) { blockMode = mode }
                    } label: {
                        HStack(spacing: ZenTheme.Spacing.md) {
                            Image(systemName: mode.icon)
                                .font(.title3)
                                .foregroundStyle(blockMode == mode ? ZenTheme.primary : ZenTheme.textSecondary)
                                .frame(width: 28)

                            VStack(alignment: .leading, spacing: 2) {
                                Text(mode.displayName)
                                    .font(ZenTheme.headline)
                                    .foregroundStyle(ZenTheme.text)
                                Text(mode.description)
                                    .font(ZenTheme.caption)
                                    .foregroundStyle(ZenTheme.textSecondary)
                            }

                            Spacer()

                            Image(systemName: blockMode == mode ? "checkmark.circle.fill" : "circle")
                                .foregroundStyle(blockMode == mode ? ZenTheme.primary : ZenTheme.surfaceLight)
                        }
                        .padding(ZenTheme.Spacing.sm)
                    }
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    @ViewBuilder
    private var modeConfigSection: some View {
        switch blockMode {
        case .timeBased:
            timeBasedConfig
        case .usageBased:
            usageBasedConfig
        case .frictionBased:
            frictionBasedConfig
        }
    }

    private var timeBasedConfig: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("Schedule")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)

                HStack {
                    VStack(alignment: .leading) {
                        Text("Start")
                            .font(ZenTheme.caption)
                            .foregroundStyle(ZenTheme.textSecondary)
                        timePickerRow(hour: $scheduleStartHour, minute: $scheduleStartMinute)
                    }
                    Spacer()
                    VStack(alignment: .leading) {
                        Text("End")
                            .font(ZenTheme.caption)
                            .foregroundStyle(ZenTheme.textSecondary)
                        timePickerRow(hour: $scheduleEndHour, minute: $scheduleEndMinute)
                    }
                }

                ZenToggle(isOn: $scheduleRepeats, label: "Repeat Daily")
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var usageBasedConfig: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("Usage Limit")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)

                HStack {
                    Text("\(usageLimitMinutes) min")
                        .font(ZenTheme.title2)
                        .foregroundStyle(ZenTheme.text)
                    Spacer()
                    Picker("Period", selection: $usagePeriod) {
                        Text("Per Hour").tag(UsagePeriod.hourly)
                        Text("Per Day").tag(UsagePeriod.daily)
                    }
                    .pickerStyle(.segmented)
                    .frame(width: 160)
                }

                Slider(value: Binding(
                    get: { Double(usageLimitMinutes) },
                    set: { usageLimitMinutes = Int($0) }
                ), in: 5...480, step: 5)
                .tint(ZenTheme.primary)
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var frictionBasedConfig: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("Friction Type")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)

                ForEach([FrictionType.breathing, .delay, .question], id: \.self) { type in
                    Button {
                        withAnimation(ZenTheme.springy) { frictionType = type }
                    } label: {
                        HStack {
                            Text(type.displayName)
                                .font(ZenTheme.body)
                                .foregroundStyle(ZenTheme.text)
                            Spacer()
                            Image(systemName: frictionType == type ? "checkmark.circle.fill" : "circle")
                                .foregroundStyle(frictionType == type ? ZenTheme.accent : ZenTheme.surfaceLight)
                        }
                        .padding(.vertical, ZenTheme.Spacing.xs)
                    }
                }

                ZenToggle(isOn: $progressiveDelay, label: "Progressive Delay")

                if progressiveDelay {
                    Text("Delay increases each time you try to open the app")
                        .font(ZenTheme.caption)
                        .foregroundStyle(ZenTheme.textSecondary)
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var webFilterSection: some View {
        GlassCard {
            VStack(spacing: ZenTheme.Spacing.md) {
                ZenToggle(isOn: $webFilterEnabled, label: "Block Websites")
                if webFilterEnabled {
                    ZenToggle(isOn: $blockAdultContent, label: "Block Adult Content")
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var createButton: some View {
        ZenButton(title: "Create Group", icon: "checkmark.shield") {
            createGroup()
        }
        .disabled(name.isEmpty || !hasSelectedApps)
        .opacity(name.isEmpty || !hasSelectedApps ? 0.5 : 1)
    }

    private var hasSelectedApps: Bool {
        !selection.applicationTokens.isEmpty || !selection.categoryTokens.isEmpty
    }

    private var selectionSummary: String {
        let appCount = selection.applicationTokens.count
        let catCount = selection.categoryTokens.count
        if appCount == 0 && catCount == 0 { return "No apps selected" }
        var parts: [String] = []
        if appCount > 0 { parts.append("\(appCount) app\(appCount == 1 ? "" : "s")") }
        if catCount > 0 { parts.append("\(catCount) categor\(catCount == 1 ? "y" : "ies")") }
        return parts.joined(separator: ", ")
    }

    private func timePickerRow(hour: Binding<Int>, minute: Binding<Int>) -> some View {
        HStack(spacing: 4) {
            Picker("", selection: hour) {
                ForEach(0..<24, id: \.self) { Text(String(format: "%02d", $0)).tag($0) }
            }
            .frame(width: 60)
            Text(":")
                .foregroundStyle(ZenTheme.textSecondary)
            Picker("", selection: minute) {
                ForEach(Array(stride(from: 0, to: 60, by: 5)), id: \.self) { Text(String(format: "%02d", $0)).tag($0) }
            }
            .frame(width: 60)
        }
        .pickerStyle(.wheel)
        .frame(height: 80)
    }

    private func createGroup() {
        let group = BlockGroup(name: name, icon: icon, colorHex: colorHex, blockMode: blockMode)
        group.decodedSelection = selection
        group.scheduleStartHour = scheduleStartHour
        group.scheduleStartMinute = scheduleStartMinute
        group.scheduleEndHour = scheduleEndHour
        group.scheduleEndMinute = scheduleEndMinute
        group.scheduleRepeats = scheduleRepeats
        group.usageLimitMinutes = usageLimitMinutes
        group.usagePeriod = usagePeriod
        group.frictionType = frictionType
        group.frictionDelaySeconds = frictionDelay
        group.progressiveDelay = progressiveDelay
        group.webFilterEnabled = webFilterEnabled
        group.blockAdultContent = blockAdultContent

        modelContext.insert(group)
        try? modelContext.save()

        let service = BlockingService()
        service.syncGroupToAppGroups(group)

        onCreated()
        dismiss()
    }
}

extension FrictionType {
    var displayName: String {
        switch self {
        case .breathing: "🫁 Breathing Exercise"
        case .delay: "⏳ Timed Delay"
        case .question: "🤔 Mindful Question"
        }
    }
}

extension BlockMode {
    var description: String {
        switch self {
        case .timeBased: "Block during scheduled hours"
        case .usageBased: "Block after usage limit reached"
        case .frictionBased: "Add friction before opening"
        }
    }
}
