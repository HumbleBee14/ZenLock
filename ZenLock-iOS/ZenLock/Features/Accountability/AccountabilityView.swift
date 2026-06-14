import SwiftUI

struct AccountabilityView: View {
    @Environment(\.dismiss) private var dismiss

    private let manager = AccountabilityManager()
    @State private var name = ""
    @State private var partnerSet = false

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: ZenTheme.Spacing.lg) {
                        explainer
                        partnerCard
                        actionButton
                    }
                    .padding(ZenTheme.Spacing.md)
                }
            }
            .navigationTitle("Accountability")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                        .foregroundStyle(ZenTheme.textSecondary)
                }
            }
            .onAppear { load() }
        }
    }

    private var explainer: some View {
        VStack(alignment: .leading, spacing: ZenTheme.Spacing.sm) {
            Text("Accountability partner")
                .font(ZenTheme.title2)
                .foregroundStyle(ZenTheme.text)
            Text("Name someone you trust. When you ask to unlock a group, ZenLock starts a cool-down with their name on the screen — and a sequence of notifications during the wait. You're meant to show your phone to them in that moment.")
                .font(ZenTheme.callout)
                .foregroundStyle(ZenTheme.textSecondary)
        }
    }

    private var partnerCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.sm) {
                Text("Partner's name")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
                TextField("e.g. Alex", text: $name)
                    .font(ZenTheme.body)
                    .foregroundStyle(ZenTheme.text)
            }
            .padding(ZenTheme.Spacing.md)
        }
    }

    @ViewBuilder
    private var actionButton: some View {
        if partnerSet {
            VStack(spacing: ZenTheme.Spacing.sm) {
                ZenButton(title: "Update Partner", icon: "checkmark") { save() }
                ZenButton(title: "Remove Partner", icon: "person.fill.xmark", style: .destructive) {
                    manager.partner = nil
                    name = ""
                    partnerSet = false
                }
            }
        } else {
            ZenButton(title: "Set as Partner", icon: "person.fill.checkmark") { save() }
                .disabled(name.isEmpty)
                .opacity(name.isEmpty ? 0.5 : 1)
        }
    }

    private func load() {
        if let existing = manager.partner {
            name = existing.name
            partnerSet = true
        }
    }

    private func save() {
        manager.partner = AccountabilityPartner(name: name, coolDownMinutes: CooldownService.minutes)
        partnerSet = true
    }
}
