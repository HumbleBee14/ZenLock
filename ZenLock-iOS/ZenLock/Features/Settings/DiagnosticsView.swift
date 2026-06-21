import SwiftUI
import FamilyControls
import ManagedSettings

struct DiagnosticsView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var lines: [String] = []

    var body: some View {
        NavigationStack {
            ZStack {
                ZenTheme.background.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 4) {
                        ForEach(Array(lines.enumerated()), id: \.offset) { _, line in
                            Text(line)
                                .font(.system(size: 12, design: .monospaced))
                                .foregroundStyle(ZenTheme.text)
                                .textSelection(.enabled)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }
                    .padding(ZenTheme.Spacing.md)
                }
            }
            .navigationTitle("Diagnostics")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                        .foregroundStyle(ZenTheme.textSecondary)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Refresh") { runDiagnostics() }
                        .foregroundStyle(ZenTheme.primary)
                }
            }
            .onAppear { runDiagnostics() }
        }
    }

    private func runDiagnostics() {
        var out: [String] = []
        out.append("=== ZenLock Diagnostics ===")
        out.append("App Group: \(Constants.appGroupID)")

        let auth = AuthorizationCenter.shared.authorizationStatus
        out.append("Screen Time auth: \(authString(auth))")

        guard let defaults = UserDefaults(suiteName: Constants.appGroupID) else {
            out.append("❌ App Group UserDefaults FAILED to open.")
            out.append("    → check entitlements + provisioning")
            lines = out
            return
        }
        out.append("✅ App Group UserDefaults opened")

        let groups = SharedBlockGroup.load(from: defaults)
        out.append("")
        out.append("Stored groups: \(groups.count)")
        for g in groups {
            out.append("  • \(g.name) (\(g.id))")
            out.append("    mode=\(g.blockMode), active=\(g.isActive), deepFocus=\(g.deepFocusEnabled)")
            if let data = g.selectionData {
                if let sel = try? JSONDecoder().decode(FamilyActivitySelection.self, from: data) {
                    out.append("    selection: apps=\(sel.applicationTokens.count), cats=\(sel.categoryTokens.count)")
                } else {
                    out.append("    ❌ selectionData failed to decode")
                }
            } else {
                out.append("    ⚠️ no selectionData stored")
            }

            let activeKey = Constants.Keys.activeGroupPrefix + g.id
            out.append("    \(activeKey) = \(defaults.bool(forKey: activeKey))")

            let selKey = Constants.Keys.selectionPrefix + g.id
            if let _ = defaults.data(forKey: selKey) {
                out.append("    ✅ \(selKey) exists")
            } else {
                out.append("    ❌ \(selKey) MISSING — extension can't read selection")
            }

            let storeName = ManagedSettingsStore.Name(g.id)
            let store = ManagedSettingsStore(named: storeName)
            let appCount = store.shield.applications?.count ?? 0
            let catCount: Int = {
                guard let policy = store.shield.applicationCategories else { return 0 }
                if case .specific(let tokens, _) = policy { return tokens.count }
                return 0
            }()
            out.append("    Shield store: apps=\(appCount), cats=\(catCount)")
        }

        out.append("")
        out.append("Bundle ID: \(Bundle.main.bundleIdentifier ?? "?")")
        out.append("Onboarding done: \(defaults.bool(forKey: Constants.Keys.onboardingCompleted))")

        lines = out
    }

    private func authString(_ s: AuthorizationStatus) -> String {
        switch s {
        case .notDetermined: return "notDetermined"
        case .denied: return "denied"
        case .approved: return "approved"
        default: return "unknown(\(s.rawValue))"
        }
    }
}
