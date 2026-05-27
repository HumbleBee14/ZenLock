import SwiftUI

struct ZenCard<Content: View>: View {
    var showGradientBorder: Bool = false
    var cornerRadius: CGFloat = ZenTheme.CornerRadius.lg
    var padding: CGFloat = ZenTheme.Spacing.md
    @ViewBuilder let content: () -> Content

    var body: some View {
        content()
            .padding(padding)
            .background(ZenTheme.surface, in: shape)
            .overlay {
                if showGradientBorder {
                    shape
                        .strokeBorder(
                            LinearGradient(
                                colors: [
                                    ZenTheme.primary.opacity(0.6),
                                    ZenTheme.primaryLight.opacity(0.2),
                                    ZenTheme.accent.opacity(0.3),
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 1
                        )
                } else {
                    shape
                        .strokeBorder(Color.white.opacity(0.06), lineWidth: 0.5)
                }
            }
    }

    private var shape: RoundedRectangle {
        RoundedRectangle(cornerRadius: cornerRadius)
    }
}

#Preview {
    VStack(spacing: 16) {
        ZenCard {
            Text("Standard Card")
                .foregroundStyle(ZenTheme.text)
        }
        ZenCard(showGradientBorder: true) {
            Text("Gradient Border Card")
                .foregroundStyle(ZenTheme.text)
        }
    }
    .padding()
    .background(ZenTheme.background)
}
