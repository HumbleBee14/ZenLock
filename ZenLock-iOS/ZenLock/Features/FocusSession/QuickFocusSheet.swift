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
    @State private var active: ActiveSession?
    @State private var now = Date()

    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: ZenTheme.Spacing.lg) {
                        if let active {
                            activeCard(active)
                        } else {
                            header
                            durationCard
                            appsCard
                            startButton
                        }
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
            .onAppear { reload() }
            .onReceive(timer) { _ in
                now = Date()
                if let a = active, now >= a.endsAt { stop() }
            }
        }
    }

    // MARK: - Active state

    private func activeCard(_ a: ActiveSession) -> some View {
        VStack(spacing: ZenTheme.Spacing.lg) {
            Image(systemName: "shield.lefthalf.filled")
                .font(.system(size: 56))
                .foregroundStyle(ZenTheme.success)
                .padding(.top, ZenTheme.Spacing.lg)

            Text("Focus session active")
                .font(ZenTheme.title2)
                .foregroundStyle(ZenTheme.text)

            GlassCard {
                VStack(spacing: ZenTheme.Spacing.sm) {
                    Text(timeRemaining(a.endsAt))
                        .font(.system(size: 56, weight: .bold, design: .monospaced))
                        .foregroundStyle(ZenTheme.text)
                    Text("until apps unlock")
                        .font(ZenTheme.caption)
                        .foregroundStyle(ZenTheme.textSecondary)
                }
                .frame(maxWidth: .infinity)
                .padding(ZenTheme.Spacing.lg)
            }

            ZenButton(title: "Stop Session", icon: "stop.fill", style: .destructive) {
                stop()
            }
        }
    }

    private func timeRemaining(_ end: Date) -> String {
        let s = max(0, Int(end.timeIntervalSince(now)))
        return String(format: "%02d:%02d", s / 60, s % 60)
    }

    // MARK: - Inactive state

    private var header: some View {
        VStack(spacing: ZenTheme.Spacing.xs) {
            Image(systemName: "timer")
                .font(.system(size: 40))
                .foregroundStyle(ZenTheme.primary)
            Text("Block everything, right now.")
                .font(ZenTheme.title2)
                .foregroundStyle(ZenTheme.text)
            Text("One-shot session. Apps unlock when the timer ends.")
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
                Slider(value: $durationMinutes, in: 1...240, step: 1)
                    .tint(ZenTheme.primary)
                HStack {
                    ForEach([5, 15, 25, 45, 60, 90], id: \.self) { preset in
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

    // MARK: - Session lifecycle

    private static let storeName = ManagedSettingsStore.Name("zen_quick_focus")
    private static let sessionKey = "zen_quick_focus_session"

    private func reload() {
        active = ActiveSession.load()
        // If the saved session already expired, clean up.
        if let a = active, Date() >= a.endsAt { stop() }
    }

    private func startSession() {
        let store = ManagedSettingsStore(named: Self.storeName)
        store.clearAllSettings()
        if !selection.applicationTokens.isEmpty {
            store.shield.applications = selection.applicationTokens
        }
        if !selection.categoryTokens.isEmpty {
            store.shield.applicationCategories = .specific(selection.categoryTokens)
        }

        if store.shield.applications == nil && store.shield.applicationCategories == nil {
            startError = "Nothing selected to block."
            return
        }

        let endsAt = Date().addingTimeInterval(durationMinutes * 60)
        let session = ActiveSession(endsAt: endsAt, appCount: selection.applicationTokens.count, catCount: selection.categoryTokens.count)
        session.save()
        active = session

        // Local notification when the timer ends, so we can clean up even if the app is killed.
        scheduleEndNotification(at: endsAt)
    }

    private func stop() {
        ManagedSettingsStore(named: Self.storeName).clearAllSettings()
        ActiveSession.clear()
        active = nil
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["zen_quick_focus_end"])
    }

    private func scheduleEndNotification(at date: Date) {
        let content = UNMutableNotificationContent()
        content.title = "Focus session ended"
        content.body = "Open ZenLock to confirm and unlock your apps."
        content.sound = .default
        let interval = max(1, date.timeIntervalSinceNow)
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
        let req = UNNotificationRequest(identifier: "zen_quick_focus_end", content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(req)
    }
}

// MARK: - Active session persistence

private struct ActiveSession: Codable {
    let endsAt: Date
    let appCount: Int
    let catCount: Int

    static func load() -> ActiveSession? {
        guard let data = Constants.sharedDefaults?.data(forKey: "zen_quick_focus_session"),
              let s = try? JSONDecoder().decode(ActiveSession.self, from: data) else { return nil }
        return s
    }

    func save() {
        guard let data = try? JSONEncoder().encode(self) else { return }
        Constants.sharedDefaults?.set(data, forKey: "zen_quick_focus_session")
    }

    static func clear() {
        Constants.sharedDefaults?.removeObject(forKey: "zen_quick_focus_session")
    }
}
