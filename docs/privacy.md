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

ZenLock on iOS uses Apple's built-in Screen Time framework to provide app blocking and usage tracking. Here's what happens:

- **App Blocking Permission** — iOS requires permission to block apps. You grant this once during setup. ZenLock cannot block anything without your explicit approval.
- **App Selection** — When you choose which apps to block, iOS protects your privacy by not showing ZenLock the actual app names. Instead, iOS gives ZenLock encrypted references that it cannot read or decode. This means even if someone accessed ZenLock's code or data, they couldn't see which apps you selected.
- **Usage Statistics** — Your daily usage report (the Insights tab) is rendered by iOS itself in a sandboxed area that ZenLock's main app cannot access. ZenLock never receives the raw numbers; Apple displays them directly.
- **Notifications** — ZenLock sends you reminders and alerts during focus sessions (e.g., "You're approaching your limit").

All of this data stays in a private container on your device, visible only to ZenLock and its system extensions. Nothing leaves your phone.

### Android

ZenLock on Android uses Android's built-in system permissions to monitor which app is open and apply blocks. Here's what it accesses:

- **Usage Permission** — ZenLock needs permission to see which app you're currently using so it can block it if necessary. You grant this once in system Settings. Without this, ZenLock cannot block anything.
- **Accessibility Service (optional)** — On some Android devices, the basic usage permission isn't enough to block apps reliably. If you enable the Accessibility Service (optional), ZenLock can respond faster. You can turn this off anytime in Settings.
- **Notifications** — ZenLock sends you reminders during focus sessions, just like on iOS.
- **Auto-Start Permission** — If you set up recurring focus schedules, ZenLock needs permission to restart your schedules after your phone reboots.

All data is stored in a private folder on your device that only ZenLock can access. Nothing is sent anywhere.

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
