import SwiftUI
import FamilyControls

/// Shared editor used by both Create and Edit screens.
///
/// `lockStructure` is set while a Strict Mode session is actively enforcing.
/// In that state the session structure is frozen so it can't be edited away
/// mid-session: Mode, schedule/limits, and the Strict Mode toggle itself are
/// all locked. The only changes allowed are the ones that can't be used to
/// escape the block — renaming (cosmetic) and *adding* more apps to block.
struct GroupFormView: View {
    @Binding var draft: GroupDraft
    var lockStructure: Bool = false

    @State private var showAppPicker = false
    @FocusState private var nameFocused: Bool

    private static let iconChoices = [
        "lock.shield", "moon.fill", "briefcase.fill", "book.fill",
        "figure.run", "leaf.fill", "person.fill", "gamecontroller.fill",
        "tv.fill", "message.fill", "cart.fill", "newspaper.fill"
    ]

    private static let colorChoices = [
        "#7C3AED", "#4F46E5", "#06B6D4", "#10B981",
        "#F59E0B", "#EF4444", "#EC4899", "#8B5CF6"
    ]

    private static let dayLabels = ["S", "M", "T", "W", "T", "F", "S"]

    var body: some View {
        VStack(spacing: ZenTheme.Spacing.lg) {
            if lockStructure { deepFocusLockBanner }
            identityCard
            appsCard
            blockModeCard
            modeConfigCard
            deepFocusCard
        }
        .contentShape(Rectangle())
        .onTapGesture { nameFocused = false }
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Done") { nameFocused = false }
            }
        }
        .zenAppPicker(isPresented: $showAppPicker, selection: $draft.selection, title: "Apps to Block")
    }

    // MARK: - Sections

    private var deepFocusLockBanner: some View {
        HStack(spacing: ZenTheme.Spacing.sm) {
            Image(systemName: "lock.fill").foregroundStyle(ZenTheme.warning)
            Text("Strict Mode is active. You can rename this session and add more apps, but the mode, schedule, and Strict Mode itself stay locked until the session ends.")
                .font(ZenTheme.caption)
                .foregroundStyle(ZenTheme.textSecondary)
        }
        .padding(ZenTheme.Spacing.sm)
        .background(RoundedRectangle(cornerRadius: ZenTheme.CornerRadius.md).fill(ZenTheme.warning.opacity(0.15)))
    }

    private var identityCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("Name").font(ZenTheme.caption).foregroundStyle(ZenTheme.textSecondary)
                HStack(spacing: ZenTheme.Spacing.sm) {
                    TextField("e.g., Social Media, Work Focus", text: $draft.name)
                        .font(ZenTheme.body)
                        .foregroundStyle(ZenTheme.text)
                        .focused($nameFocused)
                        .submitLabel(.done)
                        .onSubmit { nameFocused = false }
                        .onChange(of: draft.name) { _, v in
                            if v.count > 30 { draft.name = String(v.prefix(30)) }
                        }

                    Picker("", selection: $draft.icon) {
                        ForEach(Self.iconChoices, id: \.self) { sym in
                            Image(systemName: sym).tag(sym)
                        }
                    }
                    .pickerStyle(.menu)
                    .labelsHidden()
                    .tint(Color(hex: draft.colorHex))
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: ZenTheme.Spacing.sm) {
                        ForEach(Self.colorChoices, id: \.self) { hex in
                            Button {
                                withAnimation(ZenTheme.springy) { draft.colorHex = hex }
                            } label: {
                                Circle()
                                    .fill(Color(hex: hex))
                                    .frame(width: 28, height: 28)
                                    .overlay(
                                        Circle()
                                            .strokeBorder(Color.white.opacity(draft.colorHex == hex ? 0.9 : 0), lineWidth: 2)
                                    )
                            }
                        }
                    }
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var appsCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                HStack {
                    GroupIcon(systemName: "app.badge", color: ZenTheme.accent)
                    VStack(alignment: .leading) {
                        Text("Apps to Block").font(ZenTheme.headline).foregroundStyle(ZenTheme.text)
                        Text(selectionSummary).font(ZenTheme.caption).foregroundStyle(ZenTheme.textSecondary)
                    }
                    Spacer()
                }
                SelectionPreview(selection: draft.selection)

                // Adding apps is allowed even while a Strict Mode session is
                // locked — it only ever broadens the block. Removals are
                // ignored on save (see GroupDraft.applyLockedChanges).
                ZenButton(title: draft.hasSelectedApps ? (lockStructure ? "Add More Apps" : "Edit Selection") : "Select Apps",
                          icon: "plus.app", style: .secondary) {
                    showAppPicker = true
                }

                if lockStructure {
                    Text("You can add apps while Strict Mode is active, but apps already being blocked can't be removed until the session ends.")
                        .font(ZenTheme.caption2)
                        .foregroundStyle(ZenTheme.textSecondary)
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var blockModeCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("Blocking Mode").font(ZenTheme.headline).foregroundStyle(ZenTheme.text)

                ForEach(BlockMode.allCases, id: \.self) { mode in
                    Button {
                        withAnimation(ZenTheme.springy) { draft.blockMode = mode }
                    } label: {
                        HStack(spacing: ZenTheme.Spacing.md) {
                            Image(systemName: mode.icon)
                                .font(.title3)
                                .foregroundStyle(draft.blockMode == mode ? ZenTheme.primary : ZenTheme.textSecondary)
                                .frame(width: 28)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(mode.displayName).font(ZenTheme.headline).foregroundStyle(ZenTheme.text)
                                Text(mode.description).font(ZenTheme.caption).foregroundStyle(ZenTheme.textSecondary)
                            }
                            Spacer()
                            Image(systemName: draft.blockMode == mode ? "checkmark.circle.fill" : "circle")
                                .foregroundStyle(draft.blockMode == mode ? ZenTheme.primary : ZenTheme.surfaceLight)
                        }
                        .padding(ZenTheme.Spacing.sm)
                    }
                    .disabled(lockStructure)
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
        .opacity(lockStructure ? 0.6 : 1)
    }

    @ViewBuilder
    private var modeConfigCard: some View {
        switch draft.blockMode {
        case .timeBased: timeBasedCard
        case .usageBased: usageBasedCard
        }
    }

    private var timeBasedCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("Schedule").font(ZenTheme.headline).foregroundStyle(ZenTheme.text)

                HStack {
                    VStack(alignment: .leading) {
                        Text("Start").font(ZenTheme.caption).foregroundStyle(ZenTheme.textSecondary)
                        timePicker(hour: $draft.scheduleStartHour, minute: $draft.scheduleStartMinute)
                    }
                    Spacer()
                    VStack(alignment: .leading) {
                        Text("End").font(ZenTheme.caption).foregroundStyle(ZenTheme.textSecondary)
                        timePicker(hour: $draft.scheduleEndHour, minute: $draft.scheduleEndMinute)
                    }
                }

                ZenToggle(isOn: $draft.scheduleRepeats, label: "Repeat")

                if draft.scheduleRepeats {
                    Text("Active On").font(ZenTheme.caption).foregroundStyle(ZenTheme.textSecondary)
                    daySelector
                }

                if crossesMidnight {
                    Label("Schedule crosses midnight — will be split into two windows.", systemImage: "moon.stars")
                        .font(ZenTheme.caption2)
                        .foregroundStyle(ZenTheme.textSecondary)
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
        .disabled(lockStructure)
        .opacity(lockStructure ? 0.6 : 1)
    }

    private var crossesMidnight: Bool {
        let s = draft.scheduleStartHour * 60 + draft.scheduleStartMinute
        let e = draft.scheduleEndHour * 60 + draft.scheduleEndMinute
        return s > e
    }

    private var daySelector: some View {
        HStack(spacing: 6) {
            ForEach(1...7, id: \.self) { day in
                let selected = draft.scheduleDays.contains(day)
                Button {
                    withAnimation(ZenTheme.springy) {
                        if selected { draft.scheduleDays.remove(day) } else { draft.scheduleDays.insert(day) }
                    }
                } label: {
                    Text(Self.dayLabels[day - 1])
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(selected ? .white : ZenTheme.textSecondary)
                        .frame(width: 32, height: 32)
                        .background(Circle().fill(selected ? Color(hex: draft.colorHex) : ZenTheme.surfaceLight.opacity(0.4)))
                }
            }
        }
    }

    private var usageBasedCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("Time Limit").font(ZenTheme.headline).foregroundStyle(ZenTheme.text)
                HStack {
                    Text(usageLimitLabel(draft.usageLimitMinutes))
                        .font(ZenTheme.title2)
                        .foregroundStyle(ZenTheme.text)
                    Spacer()
                    Picker("Period", selection: $draft.usagePeriod) {
                        Text("Per Hour").tag(UsagePeriod.hourly)
                        Text("Per Day").tag(UsagePeriod.daily)
                    }
                    .pickerStyle(.segmented)
                    .frame(width: 160)
                    .onChange(of: draft.usagePeriod) { _, _ in
                        let opts = usageLimitOptions
                        if !opts.contains(draft.usageLimitMinutes) {
                            draft.usageLimitMinutes = opts.min(by: { abs($0 - draft.usageLimitMinutes) < abs($1 - draft.usageLimitMinutes) }) ?? opts[0]
                        }
                    }
                }
                Slider(value: Binding(
                    get: {
                        let opts = usageLimitOptions
                        let idx = opts.firstIndex(of: draft.usageLimitMinutes)
                            ?? opts.firstIndex(where: { $0 >= draft.usageLimitMinutes })
                            ?? (opts.count - 1)
                        return Double(idx)
                    },
                    set: { newValue in
                        let opts = usageLimitOptions
                        let idx = min(max(Int(newValue.rounded()), 0), opts.count - 1)
                        draft.usageLimitMinutes = opts[idx]
                    }
                ), in: 0...Double(max(usageLimitOptions.count - 1, 1)), step: 1)
                .tint(ZenTheme.primary)

                Divider().background(ZenTheme.surfaceLight.opacity(0.5))

                ZenToggle(isOn: $draft.enableOpenLimit, label: "Limit Opens Per Day")
                if draft.enableOpenLimit {
                    HStack {
                        Text("Max \(draft.maxOpensPerDay) opens / day")
                            .font(ZenTheme.body)
                            .foregroundStyle(ZenTheme.text)
                        Spacer()
                    }
                    Slider(value: Binding(
                        get: { Double(draft.maxOpensPerDay) },
                        set: { draft.maxOpensPerDay = Int($0) }
                    ), in: 1...120, step: 1)
                    .tint(ZenTheme.primary)
                }

                ZenToggle(isOn: $draft.enableSessionLimit, label: "Limit Per-Session Duration")
                if draft.enableSessionLimit {
                    HStack {
                        Text("\(draft.maxMinutesPerOpen) min per open")
                            .font(ZenTheme.body)
                            .foregroundStyle(ZenTheme.text)
                        Spacer()
                    }
                    Slider(value: Binding(
                        get: { Double(draft.maxMinutesPerOpen) },
                        set: { draft.maxMinutesPerOpen = Int($0) }
                    ), in: 1...50, step: 1)
                    .tint(ZenTheme.primary)
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
        .disabled(lockStructure)
        .opacity(lockStructure ? 0.6 : 1)
    }

    private var usageLimitOptions: [Int] {
        switch draft.usagePeriod {
        case .hourly:
            return [5, 10, 15, 20, 25, 30, 35, 40, 45, 50]
        case .daily:
            var opts = [15, 30, 45, 60]
            opts.append(contentsOf: stride(from: 90, through: 720, by: 30))
            return opts
        }
    }

    private func usageLimitLabel(_ m: Int) -> String {
        if m < 60 { return "\(m) min" }
        let h = m / 60
        let rem = m % 60
        if rem == 0 { return "\(h) hr" }
        return "\(h) hr \(rem) min"
    }

    private var deepFocusCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.sm) {
                ZenToggle(isOn: $draft.deepFocusEnabled, label: "🔒 Strict Mode")
                    .disabled(lockStructure)
                    .opacity(lockStructure ? 0.5 : 1)
                if lockStructure {
                    // The whole point of Strict Mode is that it can't be turned
                    // off mid-session — otherwise it's a one-tap bypass.
                    Text("Strict Mode can't be turned off while the session is active. It unlocks automatically when the session ends.")
                        .font(ZenTheme.caption2)
                        .foregroundStyle(ZenTheme.warning)
                } else if draft.deepFocusEnabled {
                    Text("No-escape mode. The shield hides the \"Open Anyway\" button and you can't disable this group while it's active. For time-based groups, it unlocks only when the schedule ends.")
                        .font(ZenTheme.caption)
                        .foregroundStyle(ZenTheme.textSecondary)
                } else {
                    Text("Locks the group so it can't be disabled mid-session.")
                        .font(ZenTheme.caption2)
                        .foregroundStyle(ZenTheme.textSecondary)
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    // MARK: - Helpers

    private var selectionSummary: String {
        let a = draft.selection.applicationTokens.count
        let c = draft.selection.categoryTokens.count
        if a == 0 && c == 0 { return "No apps selected" }
        var parts: [String] = []
        if a > 0 { parts.append("\(a) app\(a == 1 ? "" : "s")") }
        if c > 0 { parts.append("\(c) categor\(c == 1 ? "y" : "ies")") }
        return parts.joined(separator: ", ")
    }

    private func timePicker(hour: Binding<Int>, minute: Binding<Int>) -> some View {
        HStack(spacing: 4) {
            Picker("", selection: hour) {
                ForEach(0..<24, id: \.self) { Text(String(format: "%02d", $0)).tag($0) }
            }
            .frame(width: 60)
            Text(":").foregroundStyle(ZenTheme.textSecondary)
            Picker("", selection: minute) {
                ForEach(Array(stride(from: 0, to: 60, by: 5)), id: \.self) { Text(String(format: "%02d", $0)).tag($0) }
            }
            .frame(width: 60)
        }
        .pickerStyle(.wheel)
        .frame(height: 80)
    }
}

extension BlockMode {
    var description: String {
        switch self {
        case .timeBased: "Block during scheduled hours"
        case .usageBased: "Block after usage limit reached"
        }
    }
}
