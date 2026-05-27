import SwiftUI
import FamilyControls

struct ZenAppPickerSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var selection: FamilyActivitySelection
    var title: String = "Choose Apps"

    @State private var working: FamilyActivitySelection

    init(selection: Binding<FamilyActivitySelection>, title: String = "Choose Apps") {
        self._selection = selection
        self.title = title
        self._working = State(initialValue: selection.wrappedValue)
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                FamilyActivityPicker(selection: $working)
                    .frame(maxHeight: .infinity)

                ZenButton(title: "Done", icon: "checkmark") {
                    selection = working
                    dismiss()
                }
                .padding(.horizontal, ZenTheme.Spacing.md)
                .padding(.top, ZenTheme.Spacing.sm)
                .padding(.bottom, ZenTheme.Spacing.sm)
                .background(ZenTheme.background)
            }
            .background(ZenTheme.background)
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

extension View {
    /// Bottom-Done wrapped app picker — replaces `.familyActivityPicker(isPresented:selection:)`.
    func zenAppPicker(isPresented: Binding<Bool>, selection: Binding<FamilyActivitySelection>, title: String = "Choose Apps") -> some View {
        sheet(isPresented: isPresented) {
            ZenAppPickerSheet(selection: selection, title: title)
        }
    }
}
