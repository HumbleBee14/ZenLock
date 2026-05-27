import SwiftUI
import FamilyControls

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var screenTimeManager = ScreenTimeManager()
    @State private var showWebFilter = false

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: ZenTheme.Spacing.lg) {
                        screenTimeSection
                        webFilterSection
                        aboutSection
                    }
                    .padding(.horizontal, ZenTheme.Spacing.md)
                    .padding(.vertical, ZenTheme.Spacing.lg)
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                        .foregroundStyle(ZenTheme.primary)
                }
            }
            .onAppear { screenTimeManager.refreshStatus() }
            .sheet(isPresented: $showWebFilter) {
                WebFilterView()
            }
        }
    }

    private var webFilterSection: some View {
        Button { showWebFilter = true } label: {
            GlassCard {
                HStack(spacing: ZenTheme.Spacing.md) {
                    GroupIcon(systemName: "globe", color: ZenTheme.accent)
                    VStack(alignment: .leading) {
                        Text("Always-On Web Filter")
                            .font(ZenTheme.body)
                            .foregroundStyle(ZenTheme.text)
                        Text("Block adult content + custom domains 24/7")
                            .font(ZenTheme.caption)
                            .foregroundStyle(ZenTheme.textSecondary)
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .foregroundStyle(ZenTheme.textSecondary)
                }
                .padding(ZenTheme.Spacing.md)
            }
        }
        .buttonStyle(.plain)
    }

    private var screenTimeSection: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("Screen Time")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)

                HStack {
                    GroupIcon(systemName: "checkmark.shield", color: screenTimeManager.isAuthorized ? ZenTheme.success : ZenTheme.error)
                    VStack(alignment: .leading) {
                        Text("Authorization Status")
                            .font(ZenTheme.body)
                            .foregroundStyle(ZenTheme.text)
                        Text(screenTimeManager.isAuthorized ? "Authorized" : "Not Authorized")
                            .font(ZenTheme.caption)
                            .foregroundStyle(screenTimeManager.isAuthorized ? ZenTheme.success : ZenTheme.error)
                    }
                    Spacer()
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var aboutSection: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                Text("About")
                    .font(ZenTheme.headline)
                    .foregroundStyle(ZenTheme.text)

                infoRow(label: "Version", value: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0")
                infoRow(label: "Build", value: Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1")

                Text("ZenLock is free and open source. Your data never leaves your device.")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private func infoRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(ZenTheme.body)
                .foregroundStyle(ZenTheme.textSecondary)
            Spacer()
            Text(value)
                .font(ZenTheme.body)
                .foregroundStyle(ZenTheme.text)
        }
    }
}
