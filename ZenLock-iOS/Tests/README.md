# Usage-limit regression checks

From the repository root on macOS with Xcode command-line tools:

```sh
python3 ZenLock-iOS/Tests/run_usage_tests.py
```

The runner compiles the production monitor, scheduling service, blocking service,
models, draft normalization, and editor save action. It substitutes in-memory
stand-ins for the iOS-only DeviceActivity, ManagedSettings, FamilyControls,
notification, and persistence UI boundaries. The app-group suite is replaced with
an isolated test suite. No real Screen Time settings are modified.

Coverage includes immediate and duplicate thresholds, hourly/daily interval
resets, stopped/deleted groups, unrelated events, mode changes, invalid or empty
selections, category-only selections, registration rollback, edit retries,
strict legacy sessions, minimum/maximum limits, integer extremes, and schedule
end components. Persisted threshold state is tested at exact schedule boundaries
and across 23/25-hour DST days. Additional checks cover duplicate interval
callbacks, missing-monitor recovery, expired-shield cleanup on foreground, and
recovery failure diagnostics. Cool-down checks cover the release activity the
extension uses to unlock after a stop, early and late release callbacks,
foreground reconciliation, and Quick Focus release. Self-healing shield checks
cover expired usage periods, finished time windows, inactive or deleted
sessions, elapsed cool-downs, orphaned Quick Focus shields, and the sweep every
monitor callback performs for other sessions, while live blocks stay in place. Warning checks cover schedule configuration,
valid notifications, and rejection of unrelated/stopped/deleted-group events.
The real SDK integration must also compile:

```sh
xcodebuild -project ZenLock-iOS/ZenLock.xcodeproj -scheme ZenLock \
  -configuration Debug -destination 'generic/platform=iOS' \
  -derivedDataPath /tmp/zenlock-usage-build CODE_SIGNING_ALLOWED=NO build
```

## Physical-device acceptance

Host checks and unsigned builds do not validate callback delivery, app-group
provisioning, or enforcement by the Screen Time daemon. Before release, use a
signed build on an iPhone and record its iOS version and app build:

- Verify the hourly slider moves in 1-minute steps and the daily slider in
  10-minute steps. Switch periods with the slider at each end. Edit a legacy
  15-minute daily session: an unlocked draft snaps to a 10-minute step; a running
  strict session retains its enforced configuration through cosmetic edits.
- Set a 1-minute hourly limit and use the app; verify the shield appears within a
  few minutes of the limit (callback timing is controlled by iOS).
- Stop a non-strict session with a 1-minute cool-down, leave ZenLock, and open a
  blocked app after the countdown: it must open without reopening ZenLock. Also
  stop a Quick Focus session the same way. Reopen ZenLock and verify the session
  shows as off and its history entry is closed.
- Select a single app, then repeat with a category. With no previous usage, use
  the selection for 15 minutes in the current period. With notification permission
  enabled, verify the usage warning is delivered near 14 minutes (delivery timing
  is controlled by iOS). Verify the shield appears
  with ZenLock backgrounded. Repeat for both hourly and daily limits.
- On iOS 17.4+, accumulate 15 minutes before creating the limit. Verify that
  existing usage counts and an immediate threshold is honored. On iOS 17.0–17.3,
  the older initializer only counts usage after registration.
- Verify the shield clears at the next period and the next threshold blocks
  again. The monitor explicitly clears expired shields; the settings store does
  not expire them independently. Start/end callbacks occur when the device is in
  use. Reopening ZenLock also reconciles recorded expiry and restores missing
  monitors without restarting healthy registrations. Exercise the last minute of the hour/day, midnight, and a time-zone or
  daylight-saving transition. Calendar boundaries are controlled by iOS.
- Rename a shielded usage session and verify it stays shielded. Change its
  threshold, period, or selection and verify the new registration takes effect.
- Cause an activation failure (for example, revoke Screen Time authorization).
  Verify the error is shown and no session is falsely marked active. Correct the
  problem and retry from the same editor.
- Stop or delete a session and verify late callbacks cannot reactivate it.
- Force a stale shield (for example, reach an hourly limit, then keep the phone
  idle past the hour) and tap the blocked app: the shield should read "Block
  ended" with an Unlock button, and tapping it should open the app without
  opening ZenLock.

## Platform limits

Usage limits move in 1-minute steps per hour (1–59 min) and 10-minute steps per
day (10 min–12 hr). Apple's 15-minute API minimum applies to the monitoring
schedule, not to the usage threshold, so short thresholds are valid; iOS still
controls how promptly a threshold callback is delivered.

The app honors matching threshold callbacks, including immediate callbacks for
past usage. It cannot distinguish a legitimate callback from Apple's reported
premature-callback regression using elapsed wall-clock time. This change removes
the silent discard, but does not claim to repair Screen Time daemon bugs.

References:
- https://developer.apple.com/documentation/deviceactivity/deviceactivityevent/includespastactivity
- https://developer.apple.com/documentation/deviceactivity/deviceactivitycenter/monitoringerror/intervaltooshort
- https://developer.apple.com/forums/thread/808470

A threshold delivered after its original period has ended cannot be assigned to
that original period from the callback alone. Such a callback may record the
current period. Start/end callback reordering is covered by persisted expiry;
this does not establish safety for every delayed threshold from the OS.
