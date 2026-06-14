import Foundation

/// Single source of truth for the global cool-down — the friction delay between
/// requesting a stop and apps actually unlocking. Used by every focus session
/// (Quick Focus and saved sessions). Strict Mode ignores it (no stopping at all).
enum CooldownService {
    /// Allowed durations: 1–10 min in 1-min steps, then 10-min jumps to 60.
    static let options: [Int] = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 30, 40, 50, 60]

    static let minimum = 1
    static let maximum = 60

    /// Global cool-down in minutes. Always at least 1.
    static var minutes: Int {
        get {
            let stored = Constants.sharedDefaults?.integer(forKey: Constants.Keys.globalCooldownMinutes) ?? 0
            return stored < minimum ? minimum : min(stored, maximum)
        }
        set {
            Constants.sharedDefaults?.set(snap(newValue), forKey: Constants.Keys.globalCooldownMinutes)
        }
    }

    /// Snap an arbitrary value to the nearest allowed option.
    static func snap(_ value: Int) -> Int {
        options.min(by: { abs($0 - value) < abs($1 - value) }) ?? minimum
    }

    static func label(_ minutes: Int) -> String {
        minutes >= 60 ? "1 hr" : "\(minutes) min"
    }
}
