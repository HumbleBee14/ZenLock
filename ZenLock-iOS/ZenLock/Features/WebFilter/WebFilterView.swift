import SwiftUI
import FamilyControls

struct WebFilterView: View {
    @Environment(\.dismiss) private var dismiss

    private let manager = WebFilterManager()
    @State private var selection = FamilyActivitySelection()
    @State private var adultFilter = false
    @State private var showPicker = false

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: ZenTheme.Spacing.lg) {
                        explainer
                        adultCard
                        customCard
                        applyButton
                        clearButton
                    }
                    .padding(ZenTheme.Spacing.md)
                }
            }
            .navigationTitle("Always-On Filter")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                        .foregroundStyle(ZenTheme.textSecondary)
                }
            }
            .familyActivityPicker(isPresented: $showPicker, selection: $selection)
            .onAppear {
                selection = manager.loadSelection()
                adultFilter = manager.adultFilterEnabled
            }
        }
    }

    private var explainer: some View {
        VStack(alignment: .leading, spacing: ZenTheme.Spacing.sm) {
            Text("Always-on web filtering")
                .font(ZenTheme.title2)
                .foregroundStyle(ZenTheme.text)
            Text("These filters apply 24/7, independent of your blocking groups. Use this for things you never want to see — adult content, gambling sites, specific domains.")
                .font(ZenTheme.callout)
                .foregroundStyle(ZenTheme.textSecondary)
        }
    }

    private var adultCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.sm) {
                ZenToggle(isOn: $adultFilter, label: "🛡️ Block Adult Content")
                Text("Uses Apple's built-in adult content classifier in Safari and apps that respect the system filter.")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var customCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.sm) {
                HStack {
                    Text("Custom Domains")
                        .font(ZenTheme.headline)
                        .foregroundStyle(ZenTheme.text)
                    Spacer()
                    Text("\(selection.webDomainTokens.count)")
                        .font(ZenTheme.headline)
                        .foregroundStyle(ZenTheme.primary)
                }
                Text(selection.webDomainTokens.isEmpty
                     ? "No custom domains yet. Tap below to pick websites to block."
                     : "These websites will be blocked across all browsers.")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
                ZenButton(title: "Choose Websites", icon: "globe", style: .secondary) {
                    showPicker = true
                }
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    private var applyButton: some View {
        ZenButton(title: "Apply Filter", icon: "checkmark.shield") {
            manager.apply(selection: selection, blockAdultContent: adultFilter)
            dismiss()
        }
    }

    private var clearButton: some View {
        ZenButton(title: "Turn Off Filter", icon: "xmark.shield", style: .destructive) {
            manager.clear()
            selection = FamilyActivitySelection()
            adultFilter = false
            dismiss()
        }
    }
}
