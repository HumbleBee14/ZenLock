import ManagedSettings
import ManagedSettingsUI
import FamilyControls
import UIKit

class ShieldConfigurationExtension: ShieldConfigurationDataSource {

    private let defaults = UserDefaults(suiteName: Constants.appGroupID)

    private let indigoColor = UIColor(red: 79/255, green: 70/255, blue: 229/255, alpha: 1)
    private let grayColor = UIColor(red: 120/255, green: 120/255, blue: 128/255, alpha: 1)

    override func configuration(shielding application: Application) -> ShieldConfiguration {
        let group = resolveGroup(for: application.token, categoryToken: nil)
        return buildConfiguration(for: group, fallbackName: application.localizedDisplayName)
    }

    override func configuration(shielding application: Application, in category: ActivityCategory) -> ShieldConfiguration {
        let group = resolveGroup(for: application.token, categoryToken: category.token)
        return buildConfiguration(for: group, fallbackName: category.localizedDisplayName)
    }

    override func configuration(shielding webDomain: WebDomain) -> ShieldConfiguration {
        let group = webDomain.token.flatMap { resolveGroup(forWebDomain: $0) }
        let groupName = group?.name ?? "ZenLock"
        let tint = group.map { UIColor(hex: $0.colorHex) ?? indigoColor } ?? indigoColor

        return ShieldConfiguration(
            backgroundBlurStyle: .systemThickMaterial,
            backgroundColor: .white,
            icon: nil,
            title: ShieldConfiguration.Label(text: "🔒 Website Blocked", color: .black),
            subtitle: ShieldConfiguration.Label(
                text: "Blocked by \(groupName).",
                color: .secondaryLabel
            ),
            primaryButtonLabel: ShieldConfiguration.Label(text: "Go Back", color: .white),
            primaryButtonBackgroundColor: tint,
            secondaryButtonLabel: nil
        )
    }

    override func configuration(shielding webDomain: WebDomain, in category: ActivityCategory) -> ShieldConfiguration {
        configuration(shielding: webDomain)
    }

    // MARK: - Group resolution

    private func loadGroups() -> [SharedBlockGroup] {
        guard let data = defaults?.data(forKey: Constants.Keys.blockGroups),
              let groups = try? JSONDecoder().decode([SharedBlockGroup].self, from: data) else {
            return []
        }
        return groups
    }

    private func decodeSelection(_ data: Data?) -> FamilyActivitySelection? {
        guard let data else { return nil }
        return try? JSONDecoder().decode(FamilyActivitySelection.self, from: data)
    }

    private func resolveGroup(for appToken: ApplicationToken?, categoryToken: ActivityCategoryToken?) -> SharedBlockGroup? {
        let groups = loadGroups().filter(\.isActive)
        for group in groups {
            guard let selectionData = defaults?.data(forKey: Constants.Keys.selectionPrefix + group.id),
                  let selection = decodeSelection(selectionData) else { continue }
            if let appToken, selection.applicationTokens.contains(appToken) { return group }
            if let categoryToken, selection.categoryTokens.contains(categoryToken) { return group }
        }
        return groups.first
    }

    private func resolveGroup(forWebDomain token: WebDomainToken) -> SharedBlockGroup? {
        let groups = loadGroups().filter(\.isActive)
        for group in groups {
            guard let selectionData = defaults?.data(forKey: Constants.Keys.selectionPrefix + group.id),
                  let selection = decodeSelection(selectionData) else { continue }
            if selection.webDomainTokens.contains(token) { return group }
        }
        return groups.first
    }

    // MARK: - Configuration building

    private func buildConfiguration(for group: SharedBlockGroup?, fallbackName: String?) -> ShieldConfiguration {
        let groupName = group?.name ?? fallbackName ?? "ZenLock"
        let tint = group.flatMap { UIColor(hex: $0.colorHex) } ?? indigoColor

        if let group, group.blockMode == .frictionBased {
            return frictionConfiguration(group: group, groupName: groupName, tint: tint)
        }

        return blockConfiguration(groupName: groupName, message: group?.customShieldMessage, tint: tint, isDeepFocus: group?.deepFocusEnabled ?? false)
    }

    private func blockConfiguration(groupName: String, message: String?, tint: UIColor, isDeepFocus: Bool) -> ShieldConfiguration {
        let subtitle = message ?? "This app is blocked by \(groupName)."
        let secondary: ShieldConfiguration.Label? = isDeepFocus
            ? nil
            : ShieldConfiguration.Label(text: "Request Unlock", color: grayColor)

        return ShieldConfiguration(
            backgroundBlurStyle: .systemThickMaterial,
            backgroundColor: .white,
            icon: nil,
            title: ShieldConfiguration.Label(text: isDeepFocus ? "🔒 Deep Focus" : "🧘 Stay Focused", color: .black),
            subtitle: ShieldConfiguration.Label(text: subtitle, color: .secondaryLabel),
            primaryButtonLabel: ShieldConfiguration.Label(text: "Close App", color: .white),
            primaryButtonBackgroundColor: tint,
            secondaryButtonLabel: secondary
        )
    }

    private func frictionConfiguration(group: SharedBlockGroup, groupName: String, tint: UIColor) -> ShieldConfiguration {
        let openCount = defaults?.integer(forKey: Constants.Keys.openCountPrefix + group.id) ?? 0
        let baseDelay = group.frictionDelaySeconds ?? Constants.Defaults.frictionDelaySeconds
        let delaySeconds: Int = {
            guard group.progressiveDelay else { return baseDelay }
            return min(baseDelay + (openCount * 5), 120)
        }()

        let subtitle: String
        switch group.frictionType ?? .breathing {
        case .breathing:
            subtitle = group.customShieldMessage ?? "Take 3 deep breaths.\nInhale 4 · Hold 7 · Exhale 8."
        case .question:
            subtitle = group.customShieldMessage ?? "🤔 What are you opening \(groupName) for?\nIs it worth your time right now?"
        case .delay:
            subtitle = group.customShieldMessage ?? "Wait \(delaySeconds) seconds before opening.\n(Then you'll get a brief window to use \(groupName).)"
        }

        return ShieldConfiguration(
            backgroundBlurStyle: .systemThickMaterial,
            backgroundColor: .white,
            icon: nil,
            title: ShieldConfiguration.Label(text: "🧘 Mindful Moment", color: .black),
            subtitle: ShieldConfiguration.Label(text: subtitle, color: .secondaryLabel),
            primaryButtonLabel: ShieldConfiguration.Label(text: "I'll come back later", color: .white),
            primaryButtonBackgroundColor: tint,
            secondaryButtonLabel: group.deepFocusEnabled
                ? nil
                : ShieldConfiguration.Label(text: "Open anyway", color: grayColor)
        )
    }
}

private extension UIColor {
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
