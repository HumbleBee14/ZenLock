import Foundation
import UserNotifications

/// Shows ZenLock notifications even while the app is foregrounded. Tapping a
/// notification just opens the app — the user navigates to the session and
/// completes the same Face ID + cool-down flow. No shortcut around it.
final class ZenNotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    override init() {
        super.init()
        UNUserNotificationCenter.current().delegate = self
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }
}
