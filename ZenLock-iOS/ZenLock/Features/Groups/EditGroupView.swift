import SwiftUI
import SwiftData
import FamilyControls

struct EditGroupView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    @Bindable var group: BlockGroup
    @State private var draft: GroupDraft
    @State private var pendingUnlockAt: Date?
    @State private var now = Date()

    private let countdownTimer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    init(group: BlockGroup) {
        self.group = group
        _draft = State(initialValue: GroupDraft(from: group))
    }

    /// Lock structure (mode/apps/schedule) when Deep Focus is enforcing an active session.
    private var lockStructure: Bool {
        guard group.deepFocusEnabled, group.isActive else { return false }
        let shared = group.toShared()
        if shared.blockMode == .timeBased {
            return ScheduleEvaluator.isWithinSchedule(shared)
        }
        return true
    }

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: ZenTheme.Spacing.lg) {
                        GroupFormView(draft: $draft, lockStructure: lockStructure)
                        unlockCard
                        deleteCard
                    }
                    .padding(.horizontal, ZenTheme.Spacing.md)
                    .padding(.vertical, ZenTheme.Spacing.lg)
                }
                .scrollDismissesKeyboard(.interactively)
            }
            .navigationTitle(group.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                        .foregroundStyle(ZenTheme.textSecondary)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { save() }
                        .foregroundStyle(ZenTheme.primary)
                }
            }
            .onAppear { pendingUnlockAt = AccountabilityManager().pendingUnlock.flatMap { $0.groupId == group.id.uuidString ? $0.unlocksAt : nil } }
            .onReceive(countdownTimer) { _ in
                now = Date()
                if let end = pendingUnlockAt, now >= end { finalizeUnlock() }
            }
        }
    }

    @ViewBuilder
    private var unlockCard: some View {
        // Strict Mode can never be unlocked early. Normal active sessions can,
        // after a Face ID check and the global cool-down.
        if group.isActive && !group.deepFocusEnabled {
            if let end = pendingUnlockAt, now < end {
                cooldownCountdownCard(end: end)
            } else {
                ZenButton(title: "Stop session", icon: "hourglass", style: .secondary) {
                    Task { await requestUnlock() }
                }
            }
        }
    }

    private func cooldownCountdownCard(end: Date) -> some View {
        GlassCard {
            VStack(spacing: ZenTheme.Spacing.sm) {
                Label("Cooling down", systemImage: "hourglass")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.accent)
                Text(remaining(until: end))
                    .font(.system(size: 36, weight: .bold, design: .monospaced))
                    .foregroundStyle(ZenTheme.accent)
                Text("Apps unlock automatically when this ends.")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
                    .multilineTextAlignment(.center)
                ZenButton(title: "Keep focusing", icon: "arrow.uturn.backward", style: .primary) {
                    AccountabilityManager().cancelPendingUnlock()
                    pendingUnlockAt = nil
                }
            }
            .frame(maxWidth: .infinity)
            .padding(ZenTheme.Spacing.md)
        }
    }

    private func remaining(until end: Date) -> String {
        let s = max(0, Int(end.timeIntervalSince(now)))
        return String(format: "%02d:%02d", s / 60, s % 60)
    }

    private func finalizeUnlock() {
        pendingUnlockAt = nil
        AccountabilityManager().cancelPendingUnlock()
        _ = BlockingService().deactivateGroup(group)
        try? modelContext.save()
    }

    private func requestUnlock() async {
        let ok = await BiometricGate.authenticate(reason: "Stop “\(group.name)”")
        guard ok else { return }
        pendingUnlockAt = AccountabilityManager().requestUnlock(group: group)
    }

    private var deleteCard: some View {
        ZenButton(title: "Delete Group", icon: "trash", style: .destructive) {
            modelContext.delete(group)
            try? modelContext.save()
            BlockingService().removeGroupFromAppGroups(group.id.uuidString)
            dismiss()
        }
        .disabled(lockStructure)
        .opacity(lockStructure ? 0.5 : 1)
    }

    private func save() {
        draft.apply(to: group)
        try? modelContext.save()

        let service = BlockingService()
        if group.isActive {
            // Re-register monitoring so edited schedule/apps take effect and
            // continue to auto-activate without reopening the app.
            service.removeGroupFromAppGroups(group.id.uuidString)
            group.isActive = true
            _ = try? service.armOrActivate(group)
            try? modelContext.save()
        } else {
            service.syncGroupToAppGroups(group)
        }
        dismiss()
    }
}
