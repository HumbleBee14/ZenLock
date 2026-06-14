import SwiftUI
import UIKit
import FamilyControls
import ManagedSettings
import DeviceActivity

// MARK: - Wheel duration picker

struct WheelDurationPicker: UIViewRepresentable {
    let options: [Int]
    @Binding var selectionMinutes: Int

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> UIPickerView {
        let picker = UIPickerView()
        picker.delegate = context.coordinator
        picker.dataSource = context.coordinator
        if let idx = options.firstIndex(of: selectionMinutes) {
            picker.selectRow(idx, inComponent: 0, animated: false)
        }
        return picker
    }

    func updateUIView(_ uiView: UIPickerView, context: Context) {
        context.coordinator.parent = self
        if let idx = options.firstIndex(of: selectionMinutes),
           uiView.selectedRow(inComponent: 0) != idx {
            uiView.selectRow(idx, inComponent: 0, animated: true)
        }
    }

    final class Coordinator: NSObject, UIPickerViewDelegate, UIPickerViewDataSource {
        var parent: WheelDurationPicker
        init(_ parent: WheelDurationPicker) { self.parent = parent }

        func numberOfComponents(in pickerView: UIPickerView) -> Int { 1 }
        func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
            parent.options.count
        }
        func pickerView(_ pickerView: UIPickerView, attributedTitleForRow row: Int, forComponent component: Int) -> NSAttributedString? {
            let m = parent.options[row]
            let label: String
            if m < 60 {
                label = "\(m)min"
            } else if m % 60 == 0 {
                label = "\(m / 60)h"
            } else {
                let h = m / 60
                let min = m % 60
                label = "\(h)h,\(min)min"
            }
            return NSAttributedString(string: label, attributes: [.foregroundColor: UIColor.label])
        }
        func pickerView(_ pickerView: UIPickerView, didSelectRow row: Int, inComponent component: Int) {
            parent.selectionMinutes = parent.options[row]
        }
    }
}

struct QuickFocusSheet: View {
    @Environment(\.dismiss) private var dismiss

    @State private var selection = FamilyActivitySelection()
    @State private var showPicker = false
    @State private var durationMinutes: Int = 30
    @State private var startError: String?
    @State private var active: ActiveSession?
    @State private var now = Date()
    @State private var showExtendPicker = false
    @State private var extendMinutes: Int = 10
    @State private var showAddAppsPicker = false
    @State private var addAppsSelection = FamilyActivitySelection()

    static let durationOptions: [Int] = [10, 20, 30, 40, 50, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330, 360, 390, 420, 450, 480, 510, 540, 570, 600, 630, 660, 690, 720]

    private func durationLabel(_ m: Int) -> String {
        if m < 60 { return "\(m)min" }
        let h = m / 60
        let rem = m % 60
        return rem == 0 ? "\(h)h" : "\(h)h,\(rem)min"
    }

    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: ZenTheme.Spacing.md) {
                        if let active {
                            activeCard(active)
                                .transition(.opacity)
                        } else {
                            header
                            durationCard
                            appsCard
                        }
                    }
                    .padding(ZenTheme.Spacing.md)
                    .padding(.bottom, 100)
                }
                .animation(.easeInOut(duration: 0.3), value: active)
            }
            .safeAreaInset(edge: .bottom) {
                if active == nil {
                    startBar
                }
            }
            .navigationTitle("Quick Focus")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                        .foregroundStyle(ZenTheme.textSecondary)
                }
            }
            .zenAppPicker(isPresented: $showPicker, selection: $selection, title: "Apps to Block")
            .zenAppPicker(isPresented: $showAddAppsPicker, selection: $addAppsSelection, title: "Add more apps", onDismiss: {
                if !addAppsSelection.applicationTokens.isEmpty || !addAppsSelection.categoryTokens.isEmpty {
                    addMoreApps()
                }
            })
            .sheet(isPresented: $showExtendPicker) { extendPickerSheet }
            .alert("Couldn't start", isPresented: Binding(get: { startError != nil }, set: { if !$0 { startError = nil } })) {
                Button("OK", role: .cancel) {}
            } message: { Text(startError ?? "") }
            .onAppear {
                reload()
                checkAndCleanupExpiredSession()
            }
            .onReceive(timer) { _ in
                now = Date()
                if let a = active {
                    if now >= a.endsAt {
                        stop()
                    } else if let cd = a.cooldownEndsAt, now >= cd {
                        stop()
                    }
                }
            }
        }
    }

    // MARK: - Active state

    private func activeCard(_ a: ActiveSession) -> some View {
        VStack(spacing: ZenTheme.Spacing.lg) {
            Image(systemName: "shield.lefthalf.filled")
                .font(.system(size: 48))
                .foregroundStyle(ZenTheme.success)
                .padding(.top, ZenTheme.Spacing.lg)

            GlassCard {
                Text(timeRemaining(a.endsAt))
                    .font(.system(size: 56, weight: .bold, design: .monospaced))
                    .foregroundStyle(ZenTheme.text)
                    .frame(maxWidth: .infinity)
                    .padding(ZenTheme.Spacing.lg)
            }

            if let cd = a.cooldownEndsAt, now < cd {
                cooldownActiveCard(endsAt: cd)
            } else {
                VStack(spacing: ZenTheme.Spacing.sm) {
                    HStack(spacing: ZenTheme.Spacing.sm) {
                        ZenButton(title: "Stop", icon: "stop.fill", style: .destructive) {
                            Task { await requestStop() }
                        }
                        ZenButton(title: "Extend", icon: "plus.circle.fill", style: .primary) {
                            extendMinutes = 10
                            showExtendPicker = true
                        }
                    }
                    Button {
                        addAppsSelection = FamilyActivitySelection()
                        showAddAppsPicker = true
                    } label: {
                        HStack(spacing: 10) {
                            Image(systemName: "app.badge.fill")
                                .font(.body.weight(.semibold))
                            Text("Add more apps")
                                .font(ZenTheme.body.weight(.semibold))
                            Spacer()
                        }
                        .frame(maxWidth: .infinity)
                        .foregroundStyle(.white)
                        .padding(.horizontal, ZenTheme.Spacing.md)
                        .padding(.vertical, 12)
                        .background(
                            RoundedRectangle(cornerRadius: ZenTheme.CornerRadius.md, style: .continuous)
                                .fill(ZenTheme.accent)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

private var extendPickerSheet: some View {
        NavigationStack {
            VStack(spacing: ZenTheme.Spacing.lg) {
                GlassCard {
                    WheelDurationPicker(
                        options: Self.durationOptions,
                        selectionMinutes: $extendMinutes
                    )
                    .frame(height: 160)
                    .padding(ZenTheme.Spacing.md)
                }
                .padding(.horizontal, ZenTheme.Spacing.md)

                ZenButton(title: "Add \(extendLabel(extendMinutes))", icon: "checkmark") {
                    extendSession(by: extendMinutes)
                    showExtendPicker = false
                }
                .padding(.horizontal, ZenTheme.Spacing.md)

                Spacer()
            }
            .background(ZenTheme.background.ignoresSafeArea())
            .navigationTitle("Extend")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { showExtendPicker = false }
                        .foregroundStyle(ZenTheme.textSecondary)
                }
            }
        }
        .presentationDetents([.medium])
    }

    private func extendLabel(_ m: Int) -> String {
        if m < 60 { return "\(m)min" }
        if m % 60 == 0 { return "\(m / 60)h" }
        return "\(m / 60)h,\(m % 60)min"
    }

    private func cooldownActiveCard(endsAt: Date) -> some View {
        CooldownCountdownView(
            endsAt: endsAt,
            startedAt: endsAt.addingTimeInterval(TimeInterval(-CooldownService.minutes * 60)),
            onCancel: { cancelCooldown() }
        )
    }

    private func timeRemaining(_ end: Date) -> String {
        let s = max(0, Int(end.timeIntervalSince(now)))
        return String(format: "%02d:%02d", s / 60, s % 60)
    }

    private func requestStop() async {
        guard active != nil else { return }
        let ok = await BiometricGate.authenticate(reason: "Stop your focus session")
        guard ok else { return }
        startCooldown(minutes: CooldownService.minutes)
    }

    private func startCooldown(minutes: Int) {
        guard var a = active else { return }
        a.cooldownEndsAt = Date().addingTimeInterval(TimeInterval(minutes * 60))
        a.save()
        active = a
    }

    private func cancelCooldown() {
        guard var a = active else { return }
        a.cooldownEndsAt = nil
        a.save()
        active = a
    }

    private func extendSession(by minutes: Int) {
        guard var a = active else { return }
        a.endsAt = a.endsAt.addingTimeInterval(TimeInterval(minutes * 60))
        a.cooldownEndsAt = nil
        a.save()
        active = a

        let center = DeviceActivityCenter()
        center.stopMonitoring([DeviceActivityName(Self.storeNameString)])
        registerDeviceActivitySchedule(endsAt: a.endsAt)

        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["zen_quick_focus_end"])
        scheduleEndNotification(at: a.endsAt)
    }

    // MARK: - Inactive state

    private var header: some View {
        VStack(spacing: ZenTheme.Spacing.xs) {
            Image(systemName: "timer")
                .font(.system(size: 40))
                .foregroundStyle(ZenTheme.primary)
            Text("Block apps, right now.")
                .font(ZenTheme.title2)
                .foregroundStyle(ZenTheme.text)
        }
        .padding(.top, ZenTheme.Spacing.md)
    }

    private var durationCard: some View {
        WheelDurationPicker(
            options: Self.durationOptions,
            selectionMinutes: $durationMinutes
        )
        .frame(height: 180)
        .padding(.vertical, ZenTheme.Spacing.md)
    }

    private var appsCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.sm) {
                if hasSelection {
                    SelectionPreview(selection: selection)
                }
                Button {
                    showPicker = true
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: hasSelection ? "pencil" : "plus.app.fill")
                            .font(.body.weight(.semibold))
                        Text(hasSelection ? "Edit apps" : "Choose apps to block")
                            .font(ZenTheme.body.weight(.semibold))
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption.weight(.bold))
                            .opacity(0.6)
                    }
                    .foregroundStyle(.white)
                    .padding(.horizontal, ZenTheme.Spacing.md)
                    .padding(.vertical, 12)
                    .background(
                        RoundedRectangle(cornerRadius: ZenTheme.CornerRadius.md, style: .continuous)
                            .fill(ZenTheme.primary)
                    )
                }
                .buttonStyle(.plain)
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

    private var startBar: some View {
        VStack(spacing: 0) {
            LinearGradient(
                colors: [ZenTheme.background.opacity(0), ZenTheme.background],
                startPoint: .top, endPoint: .bottom
            )
            .frame(height: 24)

            startButton
                .padding(.horizontal, ZenTheme.Spacing.md)
                .padding(.bottom, ZenTheme.Spacing.sm)
                .background(ZenTheme.background)
        }
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

    private static let storeNameString = "zen_quick_focus"
    private static let storeName = ManagedSettingsStore.Name("zen_quick_focus")
    private static let sessionKey = "zen_quick_focus_session"

    private func reload() {
        active = ActiveSession.load()
        // If the saved session already expired, clean up.
        if let a = active, Date() >= a.endsAt { stop() }
    }

    private func checkAndCleanupExpiredSession() {
        guard let a = active else { return }
        if Date() >= a.endsAt {
            stop()
            now = Date()
        }
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

        let endsAt = Date().addingTimeInterval(TimeInterval(durationMinutes * 60))
        let session = ActiveSession(
            endsAt: endsAt,
            appCount: selection.applicationTokens.count,
            catCount: selection.categoryTokens.count,
            cooldownEndsAt: nil
        )
        session.save()
        active = session

        registerDeviceActivitySchedule(endsAt: endsAt)
        scheduleEndNotification(at: endsAt)
    }

    private func stop() {
        let center = DeviceActivityCenter()
        center.stopMonitoring([DeviceActivityName(Self.storeNameString)])
        ManagedSettingsStore(named: Self.storeName).clearAllSettings()
        ActiveSession.clear()
        active = nil
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["zen_quick_focus_end"])
    }

    private func registerDeviceActivitySchedule(endsAt: Date) {
        let now = Date()
        let startComponents = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute, .second], from: now)
        let endComponents = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute, .second], from: endsAt)

        let schedule = DeviceActivitySchedule(
            intervalStart: startComponents,
            intervalEnd: endComponents,
            repeats: false
        )

        let center = DeviceActivityCenter()
        do {
            try center.startMonitoring(DeviceActivityName(Self.storeNameString), during: schedule)
        } catch {
            print("Failed to register DeviceActivity schedule: \(error)")
        }
    }

    private func addMoreApps() {
        guard var a = active else { return }
        let store = ManagedSettingsStore(named: Self.storeName)

        let newAppTokens = addAppsSelection.applicationTokens
        let newCatTokens = addAppsSelection.categoryTokens

        a.appCount += newAppTokens.count
        a.catCount += newCatTokens.count

        if !newAppTokens.isEmpty {
            if var existing = store.shield.applications {
                existing.formUnion(newAppTokens)
                store.shield.applications = existing
            } else {
                store.shield.applications = newAppTokens
            }
        }

        if !newCatTokens.isEmpty {
            if case let .specific(cats, except) = store.shield.applicationCategories {
                var updated = cats
                updated.formUnion(newCatTokens)
                store.shield.applicationCategories = .specific(updated, except: except)
            } else {
                store.shield.applicationCategories = .specific(newCatTokens)
            }
        }

        a.save()
        active = a
        addAppsSelection = FamilyActivitySelection()
        showAddAppsPicker = false
    }

    private func scheduleEndNotification(at date: Date) {
        let content = UNMutableNotificationContent()
        content.title = "Focus session complete!"
        content.body = "Apps are now unlocked. Great work staying focused."
        content.sound = .default
        let interval = max(1, date.timeIntervalSinceNow)
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
        let req = UNNotificationRequest(identifier: "zen_quick_focus_end", content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(req)
    }
}

// MARK: - Active session persistence

struct ActiveSession: Codable, Equatable {
    var endsAt: Date
    var appCount: Int
    var catCount: Int
    var cooldownEndsAt: Date?

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
