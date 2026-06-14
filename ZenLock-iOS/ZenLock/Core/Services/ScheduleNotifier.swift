import Foundation
import UserNotifications

/// Backstop local notifications for scheduled sessions. iOS's silent
/// DeviceActivity start callback is unreliable, so we also fire a notification
/// at the scheduled start — tapping it opens ZenLock, which applies the shield
/// via the foreground self-heal even if the OS callback was missed.
struct ScheduleNotifier {
    private let center = UNUserNotificationCenter.current()

    private func identifier(_ groupId: String) -> String { "zen_schedule_start_\(groupId)" }

    func scheduleStartNotification(groupId: String, groupName: String, hour: Int, minute: Int, repeats: Bool) {
        let content = UNMutableNotificationContent()
        content.title = "🔒 \(groupName) is starting"
        content.body = "Your focus session begins now. Open ZenLock if apps aren't blocked yet."
        content.sound = .default

        var components = DateComponents()
        components.hour = hour
        components.minute = minute

        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: repeats)
        let request = UNNotificationRequest(identifier: identifier(groupId), content: content, trigger: trigger)
        center.add(request)
    }

    func cancelStartNotification(groupId: String) {
        center.removePendingNotificationRequests(withIdentifiers: [identifier(groupId)])
    }
}
