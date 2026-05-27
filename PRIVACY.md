# ZenLock — Privacy Policy

**Effective: May 26, 2026**

ZenLock is a digital-detox app that runs entirely on your device. We do not collect, transmit, store, or sell any personal data.

## What ZenLock accesses

| Permission | Why | Where it lives |
| --- | --- | --- |
| **Screen Time (Family Controls)** | To block selected apps via the Screen Time API. | On-device only. |
| **App Selection** | When you pick apps to block, iOS returns *opaque tokens* — random identifiers we cannot decode. We never see the names or bundle IDs of the apps you choose. | On-device, inside an App Group container shared between ZenLock and its system extensions. |
| **Notifications** | To warn you near usage limits and to schedule accountability nudges. | On-device only. |
| **Device Activity** | Reports (Insights tab) render usage charts. The data is sandboxed inside Apple's `DeviceActivityReport` extension — ZenLock never sees the raw numbers. | On-device, inside Apple's sandbox. |

## What ZenLock does NOT do

- We do not have servers. There is no account, no login, no telemetry.
- We do not include any third-party analytics or advertising SDKs.
- We do not share data with anyone. There is nothing to share.

## Data deletion

Uninstalling ZenLock removes all app-local data. App-Group data is wiped automatically by iOS when the last app sharing it is removed.

## Contact

Open an issue on the project's GitHub repository.
