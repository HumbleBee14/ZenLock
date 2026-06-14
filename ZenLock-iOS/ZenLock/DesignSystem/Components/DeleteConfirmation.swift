import SwiftUI

extension View {
    func deleteConfirmation(
        sessionName: String,
        isPresented: Binding<Bool>,
        onConfirm: @escaping () -> Void
    ) -> some View {
        modifier(DeleteConfirmationModifier(sessionName: sessionName, isPresented: isPresented, onConfirm: onConfirm))
    }
}

private struct DeleteConfirmationModifier: ViewModifier {
    let sessionName: String
    @Binding var isPresented: Bool
    let onConfirm: () -> Void

    @State private var confirmText = ""

    func body(content: Content) -> some View {
        content.alert("Delete “\(sessionName)”?", isPresented: $isPresented) {
            TextField("Type \"delete\" to confirm", text: $confirmText)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            Button("Cancel", role: .cancel) { confirmText = "" }
            Button("Delete", role: .destructive) {
                if confirmText.trimmingCharacters(in: .whitespaces).lowercased() == "delete" {
                    onConfirm()
                }
                confirmText = ""
            }
        } message: {
            Text("Type \"delete\" to permanently delete this session.")
        }
    }
}
