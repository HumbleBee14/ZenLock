import SwiftUI

struct ZenToastData: Equatable {
    enum Kind { case success, info, warning }
    let message: String
    let kind: Kind

    var icon: String {
        switch kind {
        case .success: "checkmark.circle.fill"
        case .info: "info.circle.fill"
        case .warning: "exclamationmark.triangle.fill"
        }
    }

    var tint: Color {
        switch kind {
        case .success: ZenTheme.success
        case .info: ZenTheme.accent
        case .warning: ZenTheme.warning
        }
    }
}

private struct ZenToastModifier: ViewModifier {
    @Binding var toast: ZenToastData?

    func body(content: Content) -> some View {
        content.overlay(alignment: .bottom) {
            if let toast {
                HStack(spacing: 10) {
                    Image(systemName: toast.icon)
                        .foregroundStyle(toast.tint)
                    Text(toast.message)
                        .font(ZenTheme.callout.weight(.medium))
                        .foregroundStyle(ZenTheme.text)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.horizontal, ZenTheme.Spacing.md)
                .padding(.vertical, ZenTheme.Spacing.sm + 2)
                .background(
                    RoundedRectangle(cornerRadius: ZenTheme.CornerRadius.lg, style: .continuous)
                        .fill(.ultraThinMaterial)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: ZenTheme.CornerRadius.lg, style: .continuous)
                        .strokeBorder(toast.tint.opacity(0.35), lineWidth: 1)
                )
                .padding(.horizontal, ZenTheme.Spacing.lg)
                .padding(.bottom, ZenTheme.Spacing.lg)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .task(id: toast) {
                    try? await Task.sleep(nanoseconds: 2_800_000_000)
                    withAnimation(ZenTheme.smooth) { self.toast = nil }
                }
            }
        }
        .animation(ZenTheme.springy, value: toast)
    }
}

extension View {
    func zenToast(_ toast: Binding<ZenToastData?>) -> some View {
        modifier(ZenToastModifier(toast: toast))
    }
}
