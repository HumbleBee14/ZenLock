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
end components. The real SDK integration must also compile:

```sh
xcodebuild -project ZenLock-iOS/ZenLock.xcodeproj -scheme ZenLock \
  -configuration Debug -destination 'generic/platform=iOS' \
  -derivedDataPath /tmp/zenlock-usage-build CODE_SIGNING_ALLOWED=NO build
```

## Physical-device acceptance

Host checks and unsigned builds do not validate callback delivery, app-group
provisioning, or enforcement by the Screen Time daemon. Before release, use a
signed build on an iPhone and record its iOS version and app build:

- Verify hourly and daily sliders start at 15 minutes and display the minimum.
  Switch periods with the slider at each end. Edit a legacy 5/10-minute session:
  an unlocked draft becomes 15 minutes; a running strict session retains its
  enforced configuration through cosmetic edits.
- Select a single app, then repeat with a category. With no previous usage, use
  the selection for 15 minutes in the current period. Verify the shield appears
  with ZenLock backgrounded. Repeat for both hourly and daily limits.
- On iOS 17.4+, accumulate 15 minutes before creating the limit. Verify that
  existing usage counts and an immediate threshold is honored. On iOS 17.0–17.3,
  the older initializer only counts usage after registration.
- Verify the shield clears at the next period and the next threshold blocks
  again. Exercise the last minute of the hour/day, midnight, and a time-zone or
  daylight-saving transition. Calendar boundaries are controlled by iOS.
- Rename a shielded usage session and verify it stays shielded. Change its
  threshold, period, or selection and verify the new registration takes effect.
- Cause an activation failure (for example, revoke Screen Time authorization).
  Verify the error is shown and no session is falsely marked active. Correct the
  problem and retry from the same editor.
- Stop or delete a session and verify late callbacks cannot reactivate it.

## Platform limits

The 15-minute usage minimum is a product rule. Apple's 15-minute API minimum
applies to the monitoring schedule, not to the usage threshold.

The app honors matching threshold callbacks, including immediate callbacks for
past usage. It cannot distinguish a legitimate callback from Apple's reported
premature-callback regression using elapsed wall-clock time. This change removes
the silent discard, but does not claim to repair Screen Time daemon bugs.

References:
- https://developer.apple.com/documentation/deviceactivity/deviceactivityevent/includespastactivity
- https://developer.apple.com/documentation/deviceactivity/deviceactivitycenter/monitoringerror/intervaltooshort
- https://developer.apple.com/forums/thread/808470
