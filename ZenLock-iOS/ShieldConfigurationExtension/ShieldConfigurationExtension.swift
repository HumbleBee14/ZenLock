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

    // MARK: - Configuration building

    private func buildConfiguration(for group: SharedBlockGroup?, fallbackName: String?) -> ShieldConfiguration {
        let groupName = group?.name ?? fallbackName ?? "ZenLock"
        let tint = group.flatMap { UIColor(hex: $0.colorHex) } ?? indigoColor
        return blockConfiguration(group: group, groupName: groupName, tint: tint)
    }

    private func blockConfiguration(group: SharedBlockGroup?, groupName: String, tint: UIColor) -> ShieldConfiguration {
        let isDeepFocus = group?.deepFocusEnabled ?? false
        let openCount = group.map { currentDailyOpenCount(for: $0.id) } ?? 0

        let capReached: Bool = {
            guard let cap = group?.maxOpensPerDay else { return false }
            return openCount >= cap
        }()

        var subtitleParts: [String] = []
        if let msg = group?.customShieldMessage {
            subtitleParts.append(msg)
        } else {
            subtitleParts.append("This app is blocked by ZenLock.")
        }
        if let cap = group?.maxOpensPerDay {
            subtitleParts.append("\(openCount)/\(cap) opens today")
        }
        if let mins = group?.maxMinutesPerOpen {
            subtitleParts.append("\(mins) min per open")
        }

        let canBypass = !isDeepFocus && !capReached
        let secondaryText: String
        if capReached {
            secondaryText = ""
        } else if group?.maxMinutesPerOpen != nil || group?.maxOpensPerDay != nil {
            secondaryText = "Open Anyway"
        } else {
            secondaryText = "Request Unlock"
        }
        let secondaryLabel: ShieldConfiguration.Label? = canBypass
            ? ShieldConfiguration.Label(text: secondaryText, color: .white)
            : nil

        let title: String
        if capReached {
            title = "🚫 Daily Limit Reached"
        } else if isDeepFocus {
            title = "🔒 \(groupName)"
        } else {
            title = "🧘 \(groupName)"
        }

        return ShieldConfiguration(
            backgroundBlurStyle: .systemUltraThinMaterialDark,
            backgroundColor: UIColor.black.withAlphaComponent(0.85),
            icon: nil,
            title: ShieldConfiguration.Label(text: title, color: .white),
            subtitle: ShieldConfiguration.Label(text: subtitleParts.joined(separator: "\n"), color: UIColor.white.withAlphaComponent(0.7)),
            primaryButtonLabel: ShieldConfiguration.Label(text: "Close App", color: .white),
            primaryButtonBackgroundColor: tint,
            secondaryButtonLabel: secondaryLabel
        )
    }

    private func currentDailyOpenCount(for groupId: String) -> Int {
        let today = Calendar.current.startOfDay(for: Date())
        let dateKey = Constants.Keys.openCountDatePrefix + groupId
        let stored = defaults?.object(forKey: dateKey) as? Date
        if stored != today { return 0 }
        return defaults?.integer(forKey: Constants.Keys.openCountPrefix + groupId) ?? 0
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
