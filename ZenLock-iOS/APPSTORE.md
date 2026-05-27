# App Store Submission Checklist — ZenLock iOS

## Before first submission

- [ ] **Family Controls entitlement** — request via Apple Developer portal → Certificates, Identifiers & Profiles → Capability Requests. Required separately for the main app and *each* extension:
  - `com.grepguru.zenlock`
  - `com.grepguru.zenlock.DeviceActivityMonitor`
  - `com.grepguru.zenlock.ShieldConfiguration`
  - `com.grepguru.zenlock.ShieldAction`
  - `com.grepguru.zenlock.DeviceActivityReport`
- [ ] **App Group** `group.com.grepguru.zenlock` provisioned for all the targets above + `com.grepguru.zenlock.Widget`.
- [ ] **Privacy policy URL** — host `PRIVACY.md` somewhere reachable (GitHub Pages is fine).
- [ ] **App Store privacy nutrition labels**: "Data Not Collected" across the board.
- [ ] **Screenshots** for required device sizes (6.7", 6.1", 5.5" iPhone). Need at least 3 per size — recommended: Dashboard, Block Group creation, Shield in the wild, Insights, Quick Focus.
- [ ] **App Review notes**: Family Controls apps get extra scrutiny. Explain that ZenLock uses the entitlement *only* to help the user block their own apps (self-use), not for parental control of others.

## What to put in the App Review note

> ZenLock is a self-use digital wellbeing app. We use `FamilyControls` with `.individual` authorization so users can block their own distracting apps via `ManagedSettings` shields and `DeviceActivity` schedules. No data leaves the device. There is no parental-control surface — the picker, the shield, and the reports are all driven by Apple's privacy-preserving opaque tokens.

## Marketing copy (draft)

**Title:** ZenLock — Free Focus & App Blocker
**Subtitle:** System-level focus sessions, no subscription.
**Keywords:** focus, screen time, app blocker, digital detox, productivity, opal alternative, one sec alternative, screenzen, freedom

**Description:**

ZenLock blocks distracting apps using Apple's Screen Time framework — the same enforcement parental controls use. Shields persist when you force-quit, schedules run in the background, and your usage data never leaves the device.

What you get for free:
• Block apps by name or by category (Social, Games, Entertainment, …).
• Three blocking modes: time-based schedule, usage limit, or "mindful moment" friction screens.
• Deep Focus mode — make blocks impossible to disable until the session ends.
• Always-on web filter (adult content + custom domains).
• Insights powered by Apple's privacy-sandboxed DeviceActivityReport.
• Streak tracking and focus scores.
• Accountability partner with cool-down + nudge notifications.
• Home screen widget for one-tap Quick Focus.

No subscriptions. No tracking. No account. Open source.

## Limitations to disclose

- Requires iOS 16+. Some features require iOS 26+.
- Will not work in the iOS Simulator (Apple's Screen Time framework is device-only).
- iOS does not expose an API to count app launches, so frequency-based blocking is approximated via usage-time limits or friction-with-progressive-delays.
