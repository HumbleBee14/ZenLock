import SwiftUI
import SwiftData
import FamilyControls

struct EditGroupView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    @Bindable var group: BlockGroup
    @State private var draft: GroupDraft
    @State private var pendingUnlockAt: Date?

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
        }
    }

    @ViewBuilder
    private var unlockCard: some View {
        // Strict Mode can never be unlocked early. Normal active sessions can,
        // after a Face ID check and the global cool-down.
        if group.isActive && !group.deepFocusEnabled {
            VStack(spacing: ZenTheme.Spacing.sm) {
                ZenButton(title: "Stop (cool-down)", icon: "hourglass", style: .secondary) {
                    Task { await requestUnlock() }
                }
                if let pendingUnlockAt {
                    Text("Cool-down ends \(pendingUnlockAt, style: .relative). Apps unlock automatically then.")
                        .font(ZenTheme.caption)
                        .foregroundStyle(ZenTheme.textSecondary)
                        .multilineTextAlignment(.center)
                }
            }
        }
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
