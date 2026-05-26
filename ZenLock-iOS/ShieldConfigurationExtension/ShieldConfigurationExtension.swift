import ManagedSettings
import ManagedSettingsUI
import UIKit

class ShieldConfigurationExtension: ShieldConfigurationDataSource {

    private let defaults = UserDefaults(suiteName: "group.com.grepguru.zenlock")

    private let indigoColor = UIColor(red: 79/255, green: 70/255, blue: 229/255, alpha: 1)
    private let grayColor = UIColor(red: 120/255, green: 120/255, blue: 128/255, alpha: 1)

    override func configuration(shielding application: Application) -> ShieldConfiguration {
        buildConfiguration(
            displayName: application.localizedDisplayName,
            entityType: .application
        )
    }

    override func configuration(shielding application: Application, in category: ActivityCategory) -> ShieldConfiguration {
        buildConfiguration(
            displayName: category.localizedDisplayName,
            entityType: .application
        )
    }

    override func configuration(shielding webDomain: WebDomain) -> ShieldConfiguration {
        ShieldConfiguration(
            backgroundBlurStyle: .systemThickMaterial,
            backgroundColor: .white,
            icon: nil,
            title: ShieldConfiguration.Label(text: "🔒 Website Blocked", color: .black),
            subtitle: ShieldConfiguration.Label(
                text: "This website is blocked by ZenLock.",
                color: .secondaryLabel
            ),
            primaryButtonLabel: ShieldConfiguration.Label(text: "Go Back", color: .white),
            primaryButtonBackgroundColor: indigoColor,
            secondaryButtonLabel: nil
        )
    }

    override func configuration(shielding webDomain: WebDomain, in category: ActivityCategory) -> ShieldConfiguration {
        configuration(shielding: webDomain)
    }

    // MARK: - Private

    private enum EntityType {
        case application
        case webDomain
    }

    private func buildConfiguration(
        displayName: String?,
        entityType: EntityType
    ) -> ShieldConfiguration {
        let activeGroupId = defaults?.string(forKey: "active_shield_group_id") ?? ""
        let groupName = defaults?.string(forKey: "group_name_\(activeGroupId)") ?? "ZenLock"
        let frictionType = defaults?.string(forKey: "friction_type_\(activeGroupId)")
        let customMessage = defaults?.string(forKey: "shield_message_\(activeGroupId)")

        if let frictionType = frictionType {
            return frictionConfiguration(
                type: frictionType,
                groupName: groupName,
                customMessage: customMessage
            )
        }

        return blockConfiguration(groupName: groupName, customMessage: customMessage)
    }

    private func blockConfiguration(
        groupName: String,
        customMessage: String?
    ) -> ShieldConfiguration {
        let subtitle = customMessage ?? "This app is blocked by \(groupName)."

        return ShieldConfiguration(
            backgroundBlurStyle: .systemThickMaterial,
            backgroundColor: .white,
            icon: nil,
            title: ShieldConfiguration.Label(text: "🧘 Stay Focused", color: .black),
            subtitle: ShieldConfiguration.Label(text: subtitle, color: .secondaryLabel),
            primaryButtonLabel: ShieldConfiguration.Label(text: "Close App", color: .white),
            primaryButtonBackgroundColor: indigoColor,
            secondaryButtonLabel: ShieldConfiguration.Label(text: "Request Unlock", color: grayColor)
        )
    }

    private func frictionConfiguration(
        type: String,
        groupName: String,
        customMessage: String?
    ) -> ShieldConfiguration {
        let subtitle: String
        switch type {
        case "breathing":
            subtitle = customMessage ?? "Take 3 deep breaths before continuing."
        case "question":
            subtitle = customMessage ?? "Do you really need this app right now?"
        case "delay":
            let openCount = defaults?.integer(forKey: "open_count_\(type)") ?? 0
            let delaySeconds = min(10 + (openCount * 5), 60)
            subtitle = customMessage ?? "Wait \(delaySeconds) seconds before opening."
        default:
            subtitle = customMessage ?? "Take a moment before continuing."
        }

        return ShieldConfiguration(
            backgroundBlurStyle: .systemThickMaterial,
            backgroundColor: .white,
            icon: nil,
            title: ShieldConfiguration.Label(text: "🧘 Mindful Moment", color: .black),
            subtitle: ShieldConfiguration.Label(text: subtitle, color: .secondaryLabel),
            primaryButtonLabel: ShieldConfiguration.Label(
                text: "I'll come back later",
                color: .white
            ),
            primaryButtonBackgroundColor: indigoColor,
            secondaryButtonLabel: ShieldConfiguration.Label(text: "Open anyway", color: grayColor)
        )
    }
}
