import SwiftUI

struct BypassPreventionView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: ZenTheme.Spacing.lg) {
                        header
                        layerCard(
                            tier: 1,
                            icon: "checkmark.shield.fill",
                            color: ZenTheme.success,
                            title: "System-level shields",
                            body: "ZenLock uses Apple's Screen Time API — the same system Parental Controls runs on. Shields persist even if you force-quit the app."
                        )
                        layerCard(
                            tier: 2,
                            icon: "lock.shield.fill",
                            color: ZenTheme.primary,
                            title: "Screen Time passcode",
                            body: "On iOS 26, revoking Screen Time permission requires the passcode. Set one in Settings → Screen Time → Use Screen Time Passcode. Tip: ask a friend to set it (and don't memorize it)."
                        )
                        layerCard(
                            tier: 3,
                            icon: "moon.zzz.fill",
                            color: ZenTheme.accent,
                            title: "Strict Mode",
                            body: "Turn on Strict Mode per group to make it impossible to disable the block until its schedule ends — even from inside ZenLock."
                        )
                        openSettings
                    }
                    .padding(ZenTheme.Spacing.md)
                }
            }
            .navigationTitle("Make it hard to bypass")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                        .foregroundStyle(ZenTheme.textSecondary)
                }
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: ZenTheme.Spacing.sm) {
            Text("Three layers of friction")
                .font(ZenTheme.title2)
                .foregroundStyle(ZenTheme.text)
            Text("No app on iOS can truly stop a determined user from bypassing a block — Apple's sandbox doesn't allow it, and apps that claim otherwise are using fragile tricks that break. What we can do is stack enough small frictions that bypassing isn't worth the effort in the moment of weakness.")
                .font(ZenTheme.callout)
                .foregroundStyle(ZenTheme.textSecondary)
        }
    }

    private func layerCard(tier: Int, icon: String, color: Color, title: String, body: String) -> some View {
        GlassCard {
            HStack(alignment: .top, spacing: ZenTheme.Spacing.md) {
                ZStack {
                    Circle().fill(color.opacity(0.15)).frame(width: 44, height: 44)
                    Image(systemName: icon).foregroundStyle(color)
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text("Layer \(tier)")
                        .font(ZenTheme.caption2)
                        .foregroundStyle(ZenTheme.textSecondary)
                    Text(title)
                        .font(ZenTheme.headline)
                        .foregroundStyle(ZenTheme.text)
                    Text(body)
                        .font(ZenTheme.callout)
                        .foregroundStyle(ZenTheme.textSecondary)
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var openSettings: some View {
        ZenButton(title: "Open iOS Settings", icon: "arrow.up.right.square") {
            if let url = URL(string: UIApplication.openSettingsURLString) {
                UIApplication.shared.open(url)
            }
        }
    }
}
