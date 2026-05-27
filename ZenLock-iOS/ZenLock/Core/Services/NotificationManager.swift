import Foundation
import UserNotifications

final class NotificationManager: Sendable {
    static let shared = NotificationManager()
    private init() {}

    func requestPermission() async -> Bool {
        do {
            return try await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])
        } catch {
            return false
        }
    }

    func scheduleUsageLimitWarning(groupName: String) {
        let content = UNMutableNotificationContent()
        content.title = "⏳ Usage Limit Reached"
        content.body = "\(groupName) has been blocked. Great job staying focused!"
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: "usage_limit_\(UUID().uuidString)",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request)
    }

    func scheduleUnlockRequest() {
        let content = UNMutableNotificationContent()
        content.title = "🔓 Unlock Requested"
        content.body = "Open ZenLock to manage your blocking session."
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: "unlock_request",
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request)
    }

    func cancelAll() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
    }
}
