import Foundation

/// Toast message for focus session arm/activate outcome.
enum ScheduleToastFactory {
    static func make(for outcome: BlockingService.ArmOutcome, group: BlockGroup) -> ZenToastData {
        switch outcome {
        case .activeNow:
            if group.blockMode == .usageBased {
                let message: String
                if #available(iOS 17.4, *) {
                    switch group.usagePeriod ?? .daily {
                    case .hourly:
                        message = "Limit active. Time already used this hour counts."
                    case .daily:
                        message = "Limit active. Time already used today counts."
                    }
                } else {
                    message = "Limit active. Counting starts now."
                }
                return ZenToastData(message: message, kind: .success)
            }
            return ZenToastData(message: "Blocking now.", kind: .success)

        case .armed(let startsAt):
            let time = startsAt.map { timeString($0) }
            let repeats = group.scheduleRepeats
            if let time {
                let base = "Armed — blocks automatically at \(time)."
                return ZenToastData(message: repeats ? base + " Repeats." : base, kind: .info)
            }
            return ZenToastData(message: "Armed — will block automatically.", kind: .info)

        case .windowPassed:
            return ZenToastData(
                message: "That time has passed today. Turn it on manually, or enable Repeat.",
                kind: .warning
            )
        }
    }

    private static func timeString(_ date: Date) -> String {
        let f = DateFormatter()
        f.timeStyle = .short
        return f.string(from: date)
    }
}
