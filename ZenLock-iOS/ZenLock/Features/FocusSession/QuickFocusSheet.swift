import SwiftUI
import FamilyControls
import ManagedSettings
import DeviceActivity

struct QuickFocusSheet: View {
    @Environment(\.dismiss) private var dismiss

    @State private var selection = FamilyActivitySelection()
    @State private var showPicker = false
    @State private var durationMinutes: Double = 25
    @State private var startError: String?

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: ZenTheme.Spacing.lg) {
                        header
                        durationCard
                        appsCard
                        startButton
                    }
                    .padding(ZenTheme.Spacing.md)
                }
            }
            .navigationTitle("Quick Focus")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                        .foregroundStyle(ZenTheme.textSecondary)
                }
            }
            .familyActivityPicker(isPresented: $showPicker, selection: $selection)
            .alert("Couldn't start", isPresented: Binding(get: { startError != nil }, set: { if !$0 { startError = nil } })) {
                Button("OK", role: .cancel) {}
            } message: { Text(startError ?? "") }
        }
    }

    private var header: some View {
        VStack(spacing: ZenTheme.Spacing.xs) {
            Image(systemName: "timer")
                .font(.system(size: 40))
                .foregroundStyle(ZenTheme.primary)
            Text("Block everything, right now.")
                .font(ZenTheme.title2)
                .foregroundStyle(ZenTheme.text)
            Text("One-shot session. No schedule. Apps unblock when the timer ends.")
                .font(ZenTheme.callout)
                .foregroundStyle(ZenTheme.textSecondary)
                .multilineTextAlignment(.center)
        }
        .padding(.top, ZenTheme.Spacing.md)
    }

    private var durationCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                HStack(alignment: .lastTextBaseline) {
                    Text("\(Int(durationMinutes))")
                        .font(.system(size: 56, weight: .bold))
                        .foregroundStyle(ZenTheme.text)
                    Text("minutes")
                        .font(ZenTheme.headline)
                        .foregroundStyle(ZenTheme.textSecondary)
                }
                Slider(value: $durationMinutes, in: 5...240, step: 5)
                    .tint(ZenTheme.primary)
                HStack {
                    ForEach([15, 25, 45, 60, 90], id: \.self) { preset in
                        Button("\(preset)m") {
                            withAnimation(ZenTheme.springy) { durationMinutes = Double(preset) }
                        }
                        .font(ZenTheme.caption)
                        .foregroundStyle(durationMinutes == Double(preset) ? .white : ZenTheme.textSecondary)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(
                            Capsule().fill(durationMinutes == Double(preset) ? ZenTheme.primary : ZenTheme.surfaceLight.opacity(0.4))
                        )
                    }
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var appsCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.sm) {
                Text("What to block")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)
                Text(summary)
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
                ZenButton(title: "Choose Apps", icon: "plus.app", style: .secondary) {
                    showPicker = true
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var startButton: some View {
        ZenButton(title: "Start Focus", icon: "play.fill") {
            startSession()
        }
        .disabled(!hasSelection)
        .opacity(hasSelection ? 1 : 0.5)
    }

    private var hasSelection: Bool {
        !selection.applicationTokens.isEmpty || !selection.categoryTokens.isEmpty
    }

    private var summary: String {
        let a = selection.applicationTokens.count
        let c = selection.categoryTokens.count
        if a == 0 && c == 0 { return "Pick the apps or categories you want blocked." }
        return "\(a) apps · \(c) categories"
    }

    private func startSession() {
        let sessionId = "quick_\(UUID().uuidString.prefix(8))"
        let store = ManagedSettingsStore(named: .init(sessionId))
        store.clearAllSettings()
        if !selection.applicationTokens.isEmpty {
            store.shield.applications = selection.applicationTokens
        }
        if !selection.categoryTokens.isEmpty {
            store.shield.applicationCategories = .specific(selection.categoryTokens)
        }

        let now = Date()
        let end = now.addingTimeInterval(durationMinutes * 60)
        let cal = Calendar.current
        let startComps = cal.dateComponents([.hour, .minute], from: now)
        let endComps = cal.dateComponents([.hour, .minute], from: end)

        let schedule = DeviceActivitySchedule(
            intervalStart: startComps,
            intervalEnd: endComps,
            repeats: false
        )

        do {
            try DeviceActivityCenter().startMonitoring(
                DeviceActivityName(sessionId),
                during: schedule
            )
            dismiss()
        } catch {
            startError = "Failed to start quick focus: \(error.localizedDescription)"
            store.clearAllSettings()
        }
    }
}
