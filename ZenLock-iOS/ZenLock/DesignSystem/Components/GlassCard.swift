import SwiftUI

struct GlassCard<Content: View>: View {
    var cornerRadius: CGFloat = ZenTheme.CornerRadius.lg
    var padding: CGFloat = ZenTheme.Spacing.md
    @ViewBuilder let content: () -> Content

    var body: some View {
        content()
            .background(.ultraThinMaterial, in: shape)
            .overlay {
                shape
                    .strokeBorder(
                        LinearGradient(
                            colors: [.white.opacity(0.15), .white.opacity(0.05)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 0.5
                    )
            }
            .shadow(color: .black.opacity(0.25), radius: 16, x: 0, y: 8)
    }

    private var shape: RoundedRectangle {
        RoundedRectangle(cornerRadius: cornerRadius)
    }
}

#Preview {
    GlassCard {
        HStack {
            Image(systemName: "lock.shield.fill")
                .font(.title)
                .foregroundStyle(ZenTheme.accent)
            VStack(alignment: .leading) {
                Text("Screen Time")
                    .font(ZenTheme.headline)
                Text("2h 14m remaining")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
            }
            Spacer()
        }
        .foregroundStyle(ZenTheme.text)
    }
    .padding()
    .background(ZenTheme.background)
}
