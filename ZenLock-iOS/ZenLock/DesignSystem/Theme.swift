import SwiftUI
import UIKit

extension UIColor {
    convenience init?(hex: String) {
        var hex = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        if hex.hasPrefix("#") { hex.removeFirst() }
        guard hex.count == 6, let int = UInt32(hex, radix: 16) else { return nil }
        self.init(
            red: CGFloat((int >> 16) & 0xFF) / 255,
            green: CGFloat((int >> 8) & 0xFF) / 255,
            blue: CGFloat(int & 0xFF) / 255,
            alpha: 1
        )
    }
}

enum ZenTheme {

    // MARK: - Colors

    static let primary = Color(hex: "4F46E5")
    static let primaryLight = Color(hex: "7C3AED")
    static let accent = Color(hex: "06B6D4")
    static let success = Color(hex: "10B981")
    static let warning = Color(hex: "F59E0B")
    static let error = Color(hex: "EF4444")

    static let background = Color.dynamic(light: "F5F5F7", dark: "0F0F1A")
    static let surface = Color.dynamic(light: "FFFFFF", dark: "1A1A2E")
    static let surfaceLight = Color.dynamic(light: "EEEEF3", dark: "2A2A3E")
    static let text = Color.dynamic(light: "0F0F1A", dark: "FFFFFF")
    static let textSecondary = Color.dynamic(light: "5B5B6E", dark: "B8B8C4")

    // MARK: - Gradients

    static let primaryGradient = LinearGradient(
        colors: [primary, primaryLight],
        startPoint: .leading,
        endPoint: .trailing
    )

    static let accentGradient = LinearGradient(
        colors: [accent, Color(hex: "0891B2")],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    static let backgroundGradient = LinearGradient(
        colors: [background, surface],
        startPoint: .top,
        endPoint: .bottom
    )

    static let shimmerGradient = LinearGradient(
        colors: [.clear, .white.opacity(0.1), .clear],
        startPoint: .leading,
        endPoint: .trailing
    )

    // MARK: - Typography

    static let largeTitle = Font.system(size: 34, weight: .bold, design: .default)
    static let title = Font.system(size: 28, weight: .bold, design: .default)
    static let title2 = Font.system(size: 22, weight: .semibold, design: .default)
    static let headline = Font.system(size: 17, weight: .semibold, design: .default)
    static let body = Font.system(size: 17, weight: .regular, design: .default)
    static let callout = Font.system(size: 16, weight: .regular, design: .default)
    static let caption = Font.system(size: 12, weight: .regular, design: .default)
    static let caption2 = Font.system(size: 11, weight: .regular, design: .default)

    // MARK: - Spacing

    enum Spacing {
        static let xs: CGFloat = 4
        static let sm: CGFloat = 8
        static let md: CGFloat = 16
        static let lg: CGFloat = 24
        static let xl: CGFloat = 32
        static let xxl: CGFloat = 48
    }

    // MARK: - Corner Radius

    enum CornerRadius {
        static let sm: CGFloat = 8
        static let md: CGFloat = 12
        static let lg: CGFloat = 16
        static let xl: CGFloat = 24
        static let pill: CGFloat = 9999
    }

    // MARK: - Animation

    enum Animations {
        static let springy = Animation.spring(response: 0.35, dampingFraction: 0.7)
        static let smooth = Animation.easeInOut(duration: 0.3)
        static let slow = Animation.easeInOut(duration: 0.6)
    }

    static let springy = Animations.springy
    static let smooth = Animations.smooth
}

// MARK: - Color Extension

extension Color {
    static func dynamic(light: String, dark: String) -> Color {
        Color(UIColor { trait in
            UIColor(hex: trait.userInterfaceStyle == .dark ? dark : light) ?? .black
        })
    }

    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 6:
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8:
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}
