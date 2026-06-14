import Foundation
import UserNotifications

/// Optional heads-up notification fired one minute before a scheduled session
/// starts. Scheduled via UNCalendarNotificationTrigger, so iOS delivers it even
/// when the app is closed — it is independent of the locking mechanism.
struct ScheduleNotifier {
    private let center = UNUserNotificationCenter.current()

    private func identifier(_ groupId: String) -> String { "zen_schedule_start_\(groupId)" }

    func scheduleHeadsUpNotification(groupId: String, groupName: String, hour: Int, minute: Int, repeats: Bool) {
        let content = UNMutableNotificationContent()
        content.title = "Focus session starting soon"
        content.body = "Wrap up! \(groupName) mode will activate in a minute."
        content.sound = .default

        // One minute before the scheduled start, wrapping across the hour/day.
        let total = (hour * 60 + minute - 1 + 1440) % 1440
        var components = DateComponents()
        components.hour = total / 60
        components.minute = total % 60

        let trigger: UNNotificationTrigger
        if repeats {
            trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
        } else {
            // Non-repeating: fire at the next occurrence of this time. A calendar
            // trigger would roll a just-passed time to tomorrow, so use a precise
            // interval to the upcoming HH:MM today (or tomorrow if already past).
            let cal = Calendar.current
            var next = cal.nextDate(after: Date(), matching: components, matchingPolicy: .nextTime) ?? Date().addingTimeInterval(60)
            if next.timeIntervalSinceNow < 1 { next = next.addingTimeInterval(86400) }
            trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(1, next.timeIntervalSinceNow), repeats: false)
        }
        let request = UNNotificationRequest(identifier: identifier(groupId), content: content, trigger: trigger)

        // Ensure permission is granted before adding — onboarding may have been
        // skipped. center.add silently no-ops without authorization.
        Task {
            let center = UNUserNotificationCenter.current()
            _ = try? await center.requestAuthorization(options: [.alert, .sound])
            try? await center.add(request)
        }
    }

    func cancelStartNotification(groupId: String) {
        center.removePendingNotificationRequests(withIdentifiers: [identifier(groupId)])
    }
}
