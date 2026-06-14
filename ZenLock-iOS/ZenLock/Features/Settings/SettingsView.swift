import SwiftUI
import SwiftData
import FamilyControls

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @State private var screenTimeManager = ScreenTimeManager()
    @State private var showBypassPrevention = false
    @State private var showDiagnostics = false
    @State private var reapplied = false
    @AppStorage(AppThemeStorage.key) private var themeRaw: String = AppTheme.system.rawValue
    @AppStorage(Constants.Keys.dailyGoalMinutes, store: Constants.sharedDefaults) private var dailyGoalMinutes: Int = DailyGoalStorage.defaultMinutes
    @AppStorage(Constants.Keys.globalCooldownMinutes, store: Constants.sharedDefaults) private var cooldownMinutes: Int = CooldownService.minimum

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: ZenTheme.Spacing.lg) {
                        screenTimeSection
                        appearanceSection
                        dailyGoalSection
                        cooldownSection
                        bypassPreventionSection
                        diagnosticsSection
                        aboutSection
                    }
                    .padding(.horizontal, ZenTheme.Spacing.md)
                    .padding(.vertical, ZenTheme.Spacing.lg)
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                        .foregroundStyle(ZenTheme.primary)
                }
            }
            .preferredColorScheme(AppTheme(rawValue: themeRaw)?.colorScheme)
            .onAppear { screenTimeManager.refreshStatus() }
            .sheet(isPresented: $showBypassPrevention) {
                BypassPreventionView()
            }
            .sheet(isPresented: $showDiagnostics) {
                DiagnosticsView()
            }
        }
    }

    private var diagnosticsSection: some View {
        Button { showDiagnostics = true } label: {
            GlassCard {
                HStack(spacing: ZenTheme.Spacing.md) {
                    GroupIcon(systemName: "stethoscope", color: ZenTheme.warning)
                    VStack(alignment: .leading) {
                        Text("Diagnostics")
                            .font(ZenTheme.body)
                            .foregroundStyle(ZenTheme.text)
                        Text("Check what's actually stored in App Groups")
                            .font(ZenTheme.caption)
                            .foregroundStyle(ZenTheme.textSecondary)
                    }
                    Spacer()
                    Image(systemName: "chevron.right").foregroundStyle(ZenTheme.textSecondary)
                }
                .padding(ZenTheme.Spacing.md)
            }
        }
        .buttonStyle(.plain)
    }

    private var bypassPreventionSection: some View {
        Button { showBypassPrevention = true } label: {
            GlassCard {
                HStack(spacing: ZenTheme.Spacing.md) {
                    GroupIcon(systemName: "lock.shield.fill", color: ZenTheme.primary)
                    VStack(alignment: .leading) {
                        Text("Make it hard to bypass")
                            .font(ZenTheme.body)
                            .foregroundStyle(ZenTheme.text)
                        Text("Three layers of friction — set them all")
                            .font(ZenTheme.caption)
                            .foregroundStyle(ZenTheme.textSecondary)
                    }
                    Spacer()
                    Image(systemName: "chevron.right").foregroundStyle(ZenTheme.textSecondary)
                }
                .padding(ZenTheme.Spacing.md)
            }
        }
        .buttonStyle(.plain)
    }

    private var screenTimeSection: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("Screen Time")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)

                HStack {
                    GroupIcon(systemName: "checkmark.shield", color: screenTimeManager.isAuthorized ? ZenTheme.success : ZenTheme.error)
                    VStack(alignment: .leading) {
                        Text("Authorization Status")
                            .font(ZenTheme.body)
                            .foregroundStyle(ZenTheme.text)
                        Text(screenTimeManager.isAuthorized ? "Authorized" : "Not Authorized")
                            .font(ZenTheme.caption)
                            .foregroundStyle(screenTimeManager.isAuthorized ? ZenTheme.success : ZenTheme.error)
                    }
                    Spacer()
                }

                Divider().overlay(ZenTheme.textSecondary.opacity(0.2))

                Button(action: reapplyBlocks) {
                    HStack(spacing: ZenTheme.Spacing.sm) {
                        Image(systemName: reapplied ? "checkmark.circle.fill" : "arrow.clockwise")
                            .foregroundStyle(reapplied ? ZenTheme.success : ZenTheme.primary)
                            .contentTransition(.symbolEffect(.replace))
                        Text(reapplied ? "Blocks re-applied" : "Re-apply blocks now")
                            .font(ZenTheme.body)
                            .foregroundStyle(ZenTheme.text)
                        Spacer()
                    }
                }
                .buttonStyle(.plain)
                Text("Use this if a scheduled session should be blocking but isn't.")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private func reapplyBlocks() {
        let groups = (try? modelContext.fetch(FetchDescriptor<BlockGroup>())) ?? []
        BlockingService().evaluateActiveGroups(groups)
        withAnimation(ZenTheme.springy) { reapplied = true }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            withAnimation(ZenTheme.smooth) { reapplied = false }
        }
    }

    private var dailyGoalSection: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("Daily Screen Time Goal")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)
                HStack {
                    Text(formatGoal(dailyGoalMinutes))
                        .font(ZenTheme.title2)
                        .foregroundStyle(ZenTheme.text)
                    Spacer()
                }
                Slider(value: Binding(
                    get: { Double(dailyGoalMinutes) },
                    set: { dailyGoalMinutes = Int($0) }
                ), in: 15...720, step: 15)
                .tint(ZenTheme.primary)
                Text("Target time you want to spend on your phone each day. Shown on the Screen Time tab.")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var cooldownSection: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("Stop Cool-down")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)
                HStack {
                    Text(CooldownService.label(cooldownMinutes))
                        .font(ZenTheme.title2)
                        .foregroundStyle(ZenTheme.text)
                    Spacer()
                }
                CooldownSlider(minutes: $cooldownMinutes)
                Text("The wait between requesting to stop a focus session and apps actually unlocking. Applies everywhere. Strict Mode can't be stopped at all.")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private func formatGoal(_ minutes: Int) -> String {
        let h = minutes / 60
        let m = minutes % 60
        if h == 0 { return "\(m)m" }
        if m == 0 { return "\(h)h" }
        return "\(h)h \(m)m"
    }

    private var appearanceSection: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("Appearance")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)

                Picker("Theme", selection: $themeRaw) {
                    ForEach(AppTheme.allCases) { theme in
                        Label(theme.label, systemImage: theme.icon).tag(theme.rawValue)
                    }
                }
                .pickerStyle(.segmented)
                .tint(ZenTheme.primary)
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var aboutSection: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("About")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)

                infoRow(label: "Version", value: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0")
                infoRow(label: "Build", value: Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1")

                HStack {
                    Text("Developer")
                        .font(ZenTheme.body)
                        .foregroundStyle(ZenTheme.textSecondary)
                    Spacer()
                    Link("Dinesh", destination: URL(string: "https://dineshy.com")!)
                        .font(ZenTheme.body)
                        .foregroundStyle(ZenTheme.primary)
                }

                Text("ZenLock is free and open source. Show your support!")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)

                Link(destination: URL(string: "https://github.com/HumbleBee14/ZenLock")!) {
                    HStack(spacing: ZenTheme.Spacing.sm) {
                        Image(systemName: "star.fill")
                        Text("Star on GitHub")
                        Spacer()
                        Image(systemName: "arrow.up.right")
                            .font(.caption)
                    }
                    .font(ZenTheme.body)
                    .foregroundStyle(ZenTheme.primary)
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private func infoRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(ZenTheme.body)
                .foregroundStyle(ZenTheme.textSecondary)
            Spacer()
            Text(value)
                .font(ZenTheme.body)
                .foregroundStyle(ZenTheme.text)
        }
    }
}
