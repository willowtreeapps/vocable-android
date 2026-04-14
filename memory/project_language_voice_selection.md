---
name: Language Selection and Voice Title Improvements
description: Language Selection MVI feature added; voice titles now show Male/Female when detectable
type: project
---

Language Selection screen added under `ui/languageselection/` following the same MVI pattern as Voice Selection (`ui/voiceselection/`).

**Why:** User requested both features on the `feature/voice-selection` branch.

**How to apply:** When working on Settings or Voice/Language selection, note these patterns.

Key implementation details:
- Uses `AppCompatDelegate.setApplicationLocales(LocaleListCompat)` for locale switching — triggers activity recreation automatically
- Language preference stored in `IVocableSharedPreferences` via `getSelectedLanguageTag()` / `setSelectedLanguageTag()`
- 15 languages supported matching `values-*` resource directories
- Language names shown in their native script via `Locale.getDisplayName(locale)`
- Voice display names now detect Male/Female: checks for "female"/"male" keyword in name, then Google TTS pattern `-x-[a-z]([fm])[a-z]-`
- Settings screen gets a new "Language" button (4th option, after Selection Mode)
- NavHost route: `ROUTE_LANGUAGE_SELECTION = "languageSelection"`
