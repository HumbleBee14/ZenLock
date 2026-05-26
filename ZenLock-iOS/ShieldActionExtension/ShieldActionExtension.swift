import ManagedSettings
import UserNotifications

class ShieldActionExtension: ShieldActionDelegate {

    private let defaults = UserDefaults(suiteName: "group.com.grepguru.zenlock")

    override func handle(
        action: ShieldAction,
        for application: ApplicationToken,
        completionHandler: @escaping (ShieldActionResponse) -> Void
    ) {
        switch action {
        case .primaryButtonPressed:
            completionHandler(.close)
        case .secondaryButtonPressed:
            handleUnlockRequest()
            completionHandler(.defer)
        @unknown default:
            completionHandler(.close)
        }
    }

    override func handle(
        action: ShieldAction,
        for webDomain: WebDomainToken,
        completionHandler: @escaping (ShieldActionResponse) -> Void
    ) {
        switch action {
        case .primaryButtonPressed:
            completionHandler(.close)
        case .secondaryButtonPressed:
            handleUnlockRequest()
            completionHandler(.defer)
        @unknown default:
            completionHandler(.close)
        }
    }

    override func handle(
        action: ShieldAction,
        for category: ActivityCategoryToken,
        completionHandler: @escaping (ShieldActionResponse) -> Void
    ) {
        switch action {
        case .primaryButtonPressed:
            completionHandler(.close)
        case .secondaryButtonPressed:
            handleUnlockRequest()
            completionHandler(.defer)
        @unknown default:
            completionHandler(.close)
        }
    }

    private func handleUnlockRequest() {
        defaults?.set(true, forKey: "zen_unlock_requested")

        let content = UNMutableNotificationContent()
        content.title = "🔓 Unlock Requested"
        content.body = "Open ZenLock to manage your blocking session."
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: "unlock_request_\(UUID().uuidString)",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request)
    }
}
