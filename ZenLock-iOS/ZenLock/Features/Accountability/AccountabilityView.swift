import SwiftUI

struct AccountabilityView: View {
    @Environment(\.dismiss) private var dismiss

    private let manager = AccountabilityManager()
    @State private var name = ""
    @State private var coolDown = 5
    @State private var partnerSet = false

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: ZenTheme.Spacing.lg) {
                        explainer
                        partnerCard
                        coolDownCard
                        actionButton
                    }
                    .padding(ZenTheme.Spacing.md)
                }
            }
            .navigationTitle("Accountability")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarColorScheme(.dark, for: .navigationBar)
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

    private var coolDownCard: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: ZenTheme.Spacing.md) {
                HStack {
                    Text("Cool-down")
                        .font(ZenTheme.headline)
                        .foregroundStyle(ZenTheme.text)
                    Spacer()
                    Text("\(coolDown) min")
                        .font(ZenTheme.headline)
                        .foregroundStyle(ZenTheme.primary)
                }
                Slider(value: Binding(get: { Double(coolDown) }, set: { coolDown = Int($0) }), in: 1...30, step: 1)
                    .tint(ZenTheme.primary)
                Text("How long ZenLock makes you wait between requesting an unlock and the shield actually coming down.")
                    .font(ZenTheme.caption)
                    .foregroundStyle(ZenTheme.textSecondary)
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
            coolDown = existing.coolDownMinutes
            partnerSet = true
        }
    }

    private func save() {
        manager.partner = AccountabilityPartner(name: name, coolDownMinutes: coolDown)
        partnerSet = true
    }
}
