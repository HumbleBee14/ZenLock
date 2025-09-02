# ZenLock - Mindful Focus App - Project Context

## 📱 CORE APP

**ZenLock** - Productivity app that blocks distracting apps during focus sessions with SMS-based accountability partner unlock system.

**Package:** `com.grepguru.zenlock`  
**Target:** Google Play Store  

## 🎯 FUNCTIONALITY

### User Flow
1. Set focus timer (hours/minutes)
2. Long-press "ENTER ZEN MODE" (3-second activation)
3. Apps get blocked except whitelisted ones
4. Early unlock: Partner receives SMS with code → User enters code → Session unlocks

### Key Features
- Timer-based app blocking via AccessibilityService
- SMS OTP system for accountability partner
- App whitelist management
- Scheduled focus sessions
- Zen-themed UI with meditation elements

## 🏗️ TECHNICAL STACK

### Requirements
- **Java**, Min SDK 28, Target SDK 35
- **Permissions:** SEND_SMS, ACCESSIBILITY_SERVICE, POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM
- **Gradle** with ProGuard for release builds

### Core Architecture
```
MainActivity → HomeFragment (timer setup)
             → AppBlockerService (blocking enforcement)
             → OTPManager (SMS system)
             → ScheduleManager (recurring sessions)
```

### Key Components
- **OTPManager.java** - SMS sending, OTP generation/verification
- **AppBlockerService.java** - AccessibilityService for app blocking
- **EnhancedUnlockManager.java** - Handles unlock flow
- **ScheduleActivator.java** - AlarmManager integration

## 💾 DATA

**SharedPreferences:** `FocusLockPrefs`
- `partner_phone` - Accountability partner number
- `whitelisted_apps` - Allowed apps during focus
- `focus_schedules` - Recurring session data

## 🔐 SECURITY (Production Ready)

### Build Configuration
```gradle
release {
    isMinifyEnabled = true
    isShrinkResources = true
    isDebuggable = false
    proguardFiles(...)
    buildConfigField("boolean", "DEBUG_LOGGING", "false")
}
```

### ProGuard Rules
- Strips all debug logs in release
- Obfuscates sensitive classes
- Removes System.out.print statements
- Keeps essential public methods only

## 📁 PROJECT STRUCTURE

```
app/src/main/java/com/grepguru/zenlock/
├── MainActivity.java
├── LockScreenActivity.java
├── PartnerContactActivity.java
├── WhitelistActivity.java
├── AppBlockerService.java
├── fragments/
│   ├── HomeFragment.java
│   └── SettingsFragment.java
└── utils/
    ├── OTPManager.java
    ├── ScheduleManager.java
    ├── ScheduleActivator.java
    └── EnhancedUnlockManager.java
```

## 🚀 DEPLOYMENT

### Build Commands
```bash
./gradlew assembleRelease    # Production APK
./gradlew assembleDebug      # Debug APK
```

### Google Play Requirements
- ✅ Target SDK 35
- ✅ ProGuard obfuscation
- ✅ No debug logs in release
- ✅ Proper permission justification
- ✅ Security hardened

### Store Listing Essentials
- Privacy policy for SMS usage
- Data safety questionnaire completion
- App content rating
- Accountability/productivity category

## 📋 NEXT STEPS

1. **Final Testing** - Verify all core features work
2. **Play Console Setup** - Upload, configure store listing
3. **Launch** - Submit for review

---
**STATUS:** Production ready, security hardened, ready for Google Play Store submission.
