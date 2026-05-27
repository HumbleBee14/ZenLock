# ZenLock

System-level focus and addiction-prevention apps for both major mobile platforms — free and open source.

| Platform | Tech | Status |
| --- | --- | --- |
| **iOS** | Swift 6 · SwiftUI · Screen Time API (FamilyControls / ManagedSettings / DeviceActivity) | Phases 1–4 implemented, pending Apple Family Controls distribution entitlement |
| **Android** | Java · XML layouts · AccessibilityService + overlay | Shipping (Play Store) |

## Subprojects

- [`ZenLock-iOS/`](ZenLock-iOS/) — the iOS app (this branch's focus). Built with native Screen Time APIs and four sandboxed extensions (DeviceActivityMonitor, ShieldConfiguration, ShieldAction, DeviceActivityReport) plus a WidgetKit extension. Project file is generated from `project.yml` via [XcodeGen](https://github.com/yonaskolb/XcodeGen) — run `xcodegen generate` after pulling a branch that adds or removes targets.
- [`ZenLock-Android/`](ZenLock-Android/) — the existing Android app.

## iOS architecture (overview)

```
ZenLock-iOS/
├── ZenLock/                  Main app (SwiftUI + SwiftData + @Observable services)
├── Shared/                   Cross-target code (Constants, SharedBlockGroup, enums, ScheduleEvaluator)
├── DeviceActivityMonitorExtension/   Background shield enforcement
├── ShieldConfigurationExtension/     Custom block-screen UI (per-group themed)
├── ShieldActionExtension/            Shield button handlers + friction bypass
├── DeviceActivityReportExtension/    Privacy-sandboxed usage charts
└── ZenLockWidget/                    Home screen widget (focus score + Quick Focus deep link)
```

See [`implementation_plan.md`](implementation_plan.md) and [`zenlock_ios_technical_reference.md`](zenlock_ios_technical_reference.md) for deep-dive design docs.

## Building iOS

```bash
cd ZenLock-iOS
brew install xcodegen        # one-time
xcodegen generate            # regenerate ZenLock.xcodeproj from project.yml
open ZenLock.xcodeproj
```

Screen Time APIs do not work in the simulator — testing requires a physical iPhone with the Family Controls entitlement.

## Privacy

ZenLock collects nothing. See [`ZenLock-iOS/PRIVACY.md`](ZenLock-iOS/PRIVACY.md).

## License

Open source. See `LICENSE` (TBD).
