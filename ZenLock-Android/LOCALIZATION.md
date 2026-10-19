# Android languages

ZenLock ships English (fallback), Portuguese (`pt-BR`, labeled **Português**), Arabic, French, German and Spanish. The app seeds English on first use. Android 13+ owns language persistence. On Android 9–12, one preference stores the selected locale and restores it through AppCompat before Activity creation. That preference is consumed once on upgrading to Android 13, before background notifications can render, then Android owns the choice. AndroidX automatic storage is intentionally disabled so two migration mechanisms cannot override each other. A one-time initialization flag prevents English reseeding after a system reset.

`app/src/main/res/xml/locale_config.xml` is the catalogue used by both Android Settings and the in-app picker. Native language labels are generated from locale metadata; **Change language** is deliberately untranslatable. Each English resource has translations in `values-pt-rBR`, `values-ar`, `values-fr`, `values-de`, `values-es`. All language resources are packaged together so switching works offline on Play installations.

## Adding a language

1. Add its BCP 47 tag to `locale_config.xml` and its Android `values-*` directory. Put English first.
2. Translate every translatable string, array and plural from `res/values/strings*.xml` and `arrays.xml`. Preserve indexed placeholders, XML markup, Android escaping and array order. Use native-speaker review for permission, PIN and unlock wording.
3. Supply the locale's plural categories. Arabic has zero, one, two, few, many and other. Do not override `translatable="false"` resources.
4. Run `python3 scripts/check_localization.py`, both flavor unit suites and lint. The catalogue automatically updates the picker; no Java language list is required.

## Writing localized UI

Use `getString` with arguments for complete sentences, and `getQuantityString` for counts. Keep numbers as 0–9 with ordinary `Locale.ROOT` formatting; there is no numeral-conversion layer. Numeric clock dialogs use the standard Android TimePickerDialog with English numeric content and translated titles/actions. Translate static labels and units; leave dynamic user content unchanged. Use `AppLanguage.localizedContext(context)` for presentation outside AppCompat activities, especially services/receivers on Android 12 and earlier. Keep database keys, protocol values, notification IDs, custom schedule names, app names and custom quotes unchanged. Bundled quotes are English-only resources marked `translatable="false"`. Quote text is dynamic saved content: never translate, reinterpret, or rewrite it on language changes. Users can add quotes in any language. The quote editor labels still localize normally.

Use start/end spacing and mirrored navigation icons. The allowed-app scroll arrows intentionally retain physical left/right positions and unmirrored icons to match RecyclerView scroll directions and accessibility labels. Keep PINs, telephone inputs and digital countdown order left-to-right inside Arabic layouts. Weekday chip labels come from ICU locale data, with full weekday accessibility descriptions.

## Verification

```sh
python3 scripts/check_localization.py
./gradlew testPlaystoreDebugUnitTest testSmsDebugUnitTest lintPlaystoreDebug assemblePlaystoreDebug assembleSmsDebug bundlePlaystoreRelease
```

Robolectric tests cover language selection and English initialization on API 28/33, background formatting, quote persistence and active-lock configuration recreation. Migration tests cover a background-first Android 13 startup, respecting existing framework choices, and not resurrecting an old choice after System default reset. Quote tests verify English defaults, custom text in another language and deletions remain unchanged across all app languages.

Before release, review all screens on actual devices at narrow width and large font sizes, in every language. Check cold restart, system app-language changes, notifications, onboarding, permissions, active focus sessions and scheduling across Android 12→13 upgrades. Debug builds include pseudolocales for expansion/RTL checks. Tests and builds cannot establish translation fluency or physical-device layout correctness.
