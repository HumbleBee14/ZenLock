import SwiftUI
import FamilyControls

struct ZenAppPickerSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var selection: FamilyActivitySelection
    var title: String = "Choose Apps"
    var onDismiss: (() -> Void)?

    @State private var working: FamilyActivitySelection

    init(selection: Binding<FamilyActivitySelection>, title: String = "Choose Apps", onDismiss: (() -> Void)? = nil) {
        self._selection = selection
        self.title = title
        self.onDismiss = onDismiss
        self._working = State(initialValue: selection.wrappedValue)
    }

    var body: some View {
        NavigationStack {
            FamilyActivityPicker(selection: $working)
                .navigationTitle(title)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { dismiss() }
                            .foregroundStyle(ZenTheme.textSecondary)
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button {
                            selection = working
                            dismiss()
                            onDismiss?()
                        } label: {
                            Image(systemName: "checkmark")
                        }
                        .foregroundStyle(ZenTheme.primary)
                    }
                }
        }
    }
}

extension View {
    /// Bottom-Done wrapped app picker — replaces `.familyActivityPicker(isPresented:selection:)`.
    func zenAppPicker(isPresented: Binding<Bool>, selection: Binding<FamilyActivitySelection>, title: String = "Choose Apps", onDismiss: (() -> Void)? = nil) -> some View {
        sheet(isPresented: isPresented) {
            ZenAppPickerSheet(selection: selection, title: title, onDismiss: onDismiss)
        }
    }
}
