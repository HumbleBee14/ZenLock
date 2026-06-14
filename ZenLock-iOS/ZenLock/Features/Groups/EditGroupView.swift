import SwiftUI
import SwiftData
import FamilyControls

struct EditGroupView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    @Bindable var group: BlockGroup
    @State private var draft: GroupDraft
    @State private var pending: AccountabilityManager.PendingUnlock?
    @State private var now = Date()
    @State private var showStopConfirm = false

    private let stopCoordinator = SessionStopCoordinator()
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
            .onAppear { refreshPending() }
            .onReceive(countdownTimer) { _ in
                now = Date()
                if let pending, now >= pending.unlocksAt { finalizeUnlock() }
            }
        }
    }

    @ViewBuilder
    private var unlockCard: some View {
        // Strict Mode can never be unlocked early. Normal active sessions can,
        // after a Face ID check and the global cool-down.
        if group.isActive && !group.deepFocusEnabled {
            if let pending {
                CooldownCountdownView(
                    endsAt: pending.unlocksAt,
                    startedAt: pending.requestedAt,
                    onCancel: {
                        stopCoordinator.cancelStop(group)
                        refreshPending()
                    }
                )
            } else {
                ZenButton(title: "Stop session", icon: "hourglass", style: .secondary) {
                    showStopConfirm = true
                }
                .alert("Stop the focus session?", isPresented: $showStopConfirm) {
                    Button("No", role: .cancel) {}
                    Button("Yes", role: .destructive) {
                        Task {
                            _ = await stopCoordinator.requestStop(group)
                            refreshPending()
                        }
                    }
                }
            }
        }
    }

    private func refreshPending() {
        pending = stopCoordinator.pendingUnlock(for: group)
    }

    private func finalizeUnlock() {
        stopCoordinator.finalizeIfElapsed(group, context: modelContext)
        refreshPending()
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
