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
    @State private var showDeleteConfirm = false

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
        ZenButton(title: "Delete Session", icon: "trash", style: .destructive) {
            showDeleteConfirm = true
        }
        .disabled(lockStructure)
        .opacity(lockStructure ? 0.5 : 1)
        .deleteConfirmation(sessionName: group.name, isPresented: $showDeleteConfirm) {
            deleteSession()
        }
    }

    private func deleteSession() {
        modelContext.delete(group)
        try? modelContext.save()
        BlockingService().removeGroupFromAppGroups(group.id.uuidString)
        dismiss()
    }

    private func save() {
        // While Strict Mode is enforcing, only additive/cosmetic edits are
        // persisted — structure and the Strict Mode flag stay frozen so the
        // session can't be edited into a stoppable state. Outside that window
        // all fields apply normally.
        if lockStructure {
            draft.applyLockedChanges(to: group)
        } else {
            draft.apply(to: group)
        }
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
