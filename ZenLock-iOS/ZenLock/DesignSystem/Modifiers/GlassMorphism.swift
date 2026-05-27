import SwiftUI

struct GlassMorphismModifier: ViewModifier {
    var cornerRadius: CGFloat = ZenTheme.CornerRadius.lg

    func body(content: Content) -> some View {
        content
            .background(.ultraThinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .stroke(Color.white.opacity(0.1), lineWidth: 1)
            )
    }
}

extension View {
    func glassMorphism(cornerRadius: CGFloat = ZenTheme.CornerRadius.lg) -> some View {
        modifier(GlassMorphismModifier(cornerRadius: cornerRadius))
    }
}
