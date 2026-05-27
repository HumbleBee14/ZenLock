import SwiftUI
import SwiftData
import FamilyControls

struct CreateGroupView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    @State private var draft = GroupDraft()
    var onCreated: () -> Void

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: ZenTheme.Spacing.lg) {
                        GroupFormView(draft: $draft)
                        createButton
                    }
                    .padding(.horizontal, ZenTheme.Spacing.md)
                    .padding(.vertical, ZenTheme.Spacing.lg)
                }
                .scrollDismissesKeyboard(.interactively)
            }
            .navigationTitle("New Group")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                        .foregroundStyle(ZenTheme.textSecondary)
                }
            }
        }
    }

    private var createButton: some View {
        ZenButton(title: "Create Group", icon: "checkmark.shield") {
            createGroup()
        }
        .disabled(draft.name.isEmpty || !draft.hasSelectedApps)
        .opacity(draft.name.isEmpty || !draft.hasSelectedApps ? 0.5 : 1)
    }

    private func createGroup() {
        let group = draft.makeGroup()
        modelContext.insert(group)
        try? modelContext.save()
        BlockingService().syncGroupToAppGroups(group)
        onCreated()
        dismiss()
    }
}
