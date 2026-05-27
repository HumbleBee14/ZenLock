import SwiftUI

struct ZenButton: View {
    enum Style {
        case primary, secondary, destructive
    }

    let title: String
    var icon: String? = nil
    var style: Style = .primary
    var isLoading: Bool = false
    let action: () -> Void

    @State private var isPressed = false

    var body: some View {
        Button {
            let generator = UIImpactFeedbackGenerator(style: .medium)
            generator.impactOccurred()
            action()
        } label: {
            HStack(spacing: ZenTheme.Spacing.sm) {
                if isLoading {
                    ProgressView()
                        .tint(labelColor)
                } else {
                    if let icon {
                        Image(systemName: icon)
                            .font(.system(size: 16, weight: .semibold))
                    }
                    Text(title)
                        .font(ZenTheme.headline)
                }
            }
            .foregroundStyle(labelColor)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(backgroundView)
            .clipShape(RoundedRectangle(cornerRadius: ZenTheme.CornerRadius.md))
            .overlay {
                if style == .secondary {
                    RoundedRectangle(cornerRadius: ZenTheme.CornerRadius.md)
                        .strokeBorder(ZenTheme.primary.opacity(0.6), lineWidth: 1.5)
                }
            }
        }
        .buttonStyle(ZenButtonStyle())
        .disabled(isLoading)
        .opacity(isLoading ? 0.8 : 1)
    }

    @ViewBuilder
    private var backgroundView: some View {
        switch style {
        case .primary:
            ZenTheme.primaryGradient
        case .secondary:
            ZenTheme.surface
        case .destructive:
            ZenTheme.error
        }
    }

    private var labelColor: Color {
        switch style {
        case .primary, .destructive:
            return .white
        case .secondary:
            return ZenTheme.primary
        }
    }
}

private struct ZenButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.96 : 1)
            .animation(ZenTheme.Animations.springy, value: configuration.isPressed)
    }
}

#Preview {
    VStack(spacing: 16) {
        ZenButton(title: "Get Started", icon: "arrow.right", style: .primary) {}
        ZenButton(title: "Settings", icon: "gear", style: .secondary) {}
        ZenButton(title: "Delete", icon: "trash", style: .destructive) {}
        ZenButton(title: "Loading", style: .primary, isLoading: true) {}
    }
    .padding()
    .background(ZenTheme.background)
}
