import Foundation

enum Constants {
    static let appGroupID = "group.com.humblebee.zenlock"
    static let maxActivities = 20
    static let maxStores = 50

    enum Keys {
        static let blockGroups = "zen_block_groups"
        static let selectionPrefix = "zen_selection_"
        static let activeGroupPrefix = "zen_active_"
        static let openCountPrefix = "zen_open_count_"
        static let openCountDatePrefix = "zen_open_count_date_"
        static let frictionBypassUntilPrefix = "friction_bypass_until_"
        static let dailyGoalMinutes = "zen_daily_goal_minutes"
        static let globalCooldownMinutes = "zen_global_cooldown_minutes"
        static let onboardingCompleted = "zen_onboarding_completed"
        static let userTier = "zen_user_tier"
        static let trialStartDate = "zen_trial_start_date"
        static let lastSyncDate = "zen_last_sync_date"
        static let shieldConfigPrefix = "zen_shield_config_"
    }

    enum Defaults {
        static let usageLimitMinutes = 60
        static let trialDurationDays = 7
    }

    static var sharedDefaults: UserDefaults? {
        UserDefaults(suiteName: appGroupID)
    }
}
