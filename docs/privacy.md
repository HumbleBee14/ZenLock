---
layout: default
title: Privacy Policy
---

# ZenLock — Privacy Policy

**Effective: May 27, 2026**

ZenLock is a digital wellbeing app that runs entirely on your device. We do not collect, transmit, store, or sell any personal data. This policy applies to both the iOS and Android versions of ZenLock.

## Summary

- **No accounts. No servers. No telemetry. No advertising.**
- Everything you do in ZenLock stays on your device.
- We do not share your data with anyone — because there is nothing to share.

## What ZenLock accesses

### iOS

ZenLock on iOS uses Apple's Screen Time API (Family Controls framework) to provide blocking and usage insights. Specifically:

- **Family Controls authorization** — Required by iOS so ZenLock can block selected apps. Granted once during onboarding.
- **App selection (FamilyActivityPicker)** — When you pick which apps to block, iOS returns *opaque tokens* — random identifiers ZenLock cannot decode into app names or bundle identifiers. We literally cannot tell which apps you chose; only iOS itself knows.
- **DeviceActivity reports** — The Screen Time tab renders today's usage. This data is sandboxed inside Apple's `DeviceActivityReport` system extension. ZenLock's main app process never reads the raw numbers; Apple renders them directly inside an isolated UI extension.
- **Local notifications** — Used to warn you when approaching a usage limit or when an accountability prompt fires.

All of the above lives inside an App Group container on your device, shared only between the ZenLock app and its system extensions. No data leaves the device.

### Android

ZenLock on Android uses platform APIs to provide blocking and usage tracking. Specifically:

- **Usage Access (`PACKAGE_USAGE_STATS`)** — Required so ZenLock can read which app is currently in the foreground in order to apply blocks. Granted once via system Settings.
- **Accessibility Service (optional)** — Used for more responsive blocking on devices where Usage Access alone is insufficient. You can revoke at any time.
- **Notifications** — Used for the same warnings and nudges as on iOS.
- **Boot Completed (`RECEIVE_BOOT_COMPLETED`)** — So your schedules and active blocks resume automatically after a restart.

All data is stored in the app's private storage on your device.

## What ZenLock does NOT do

- We do not have any backend servers. There is no API for the app to call.
- We do not include any third-party analytics SDKs (no Firebase Analytics, no Mixpanel, no Amplitude, no Crashlytics, no Sentry).
- We do not include any advertising SDKs.
- We do not collect device identifiers (no IDFA, no Advertising ID).
- We do not perform background network requests of any kind.

## Data retention

All ZenLock data lives on your device. We do not retain anything because we never receive anything.

## Data deletion

Uninstalling ZenLock removes all locally stored data:

- **iOS**: App-Group data is wiped automatically by iOS once the app and its extensions are removed.
- **Android**: App private storage is wiped automatically on uninstall.

## Children's privacy

ZenLock does not knowingly collect any data, from children or adults, because it does not collect data at all. The app is suitable for users of any age. If you are a parent or guardian setting up ZenLock on a child's device, please be aware that Apple's Family Controls and Google's Family Link have their own privacy policies that apply to those underlying services.

## Changes to this policy

If we ever change our practices in a way that affects this policy, we'll update the **Effective** date at the top and publish the new version at this same URL.

## Contact

For privacy questions, please open an issue on the project's [GitHub repository](https://github.com/humblebee/ZenLock).
