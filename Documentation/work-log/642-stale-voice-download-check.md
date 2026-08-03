# Stale-voice detection must check download status, not just name/locale match

**Issue:** #642 (Part of #613, follow-up to #632/PR #633)

## What was needed

#632/#633 added live device-voice fallback and stale-voice recovery to `VocableTextToSpeech`, but its explicit-match check (`applySelectedVoice()`) only verified a selected voice's name, locale, and network-required flag — it never checked whether the voice's data was actually downloaded. Android's TTS engine keeps a voice listed in `TextToSpeech.getVoices()` even after its data is uninstalled; it's only flagged via `TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED`, not removed from the list. So a voice a user had explicitly picked, which later got uninstalled (the literal scenario #619 describes — "removed after an OS/engine update"), would still "match" by name and keep being treated as valid.

## What changed

- Added `isVoiceDownloaded()` (two overloads: one operating on a real `Voice`, one on the raw `features: Set<String>?` for testability) checking `KEY_FEATURE_NOT_INSTALLED`
- `applySelectedVoice()`'s `availableVoiceNames` computation now requires both `isVoiceSupportedForLocale()` and `isVoiceDownloaded()` — a voice whose data was uninstalled no longer counts as "available," so `resolveVoiceSelection()` correctly returns `STALE_FALLBACK_TO_LIVE_DEFAULT` for it instead of `EXPLICIT`
- `getAvailableVoices()` refactored to reuse the same helper instead of inlining the same check twice
- 3 new unit tests on the pure `isVoiceDownloaded(features: Set<String>?)` overload

## How this was found and verified

Found via real on-device reproduction (not just code review) on a Google Play emulator image: selected a downloaded voice in Vocable's picker, uninstalled its data through the OS's own TTS voice manager (Settings → Accessibility → Text-to-speech → Install voice data → [language] → delete), then returned to the app — before the fix, it kept logging `applied voice: <name>` (the explicit-match branch) with no stale warning. After the fix, the same repro correctly logs the "no longer resolves" warning and falls back to `applied device default voice: ...`, and the persisted preference visibly clears back to "Default" in Selection Mode — confirmed both immediately (no relaunch needed, since it's evaluated live on the next `speak()` call) and after a full app relaunch.

## Pointers

- Issue: #642 · Parent: #613 · Follow-up to: #632 / PR #633
- Related: #619 (this fixes a real gap in that ticket's stale-detection scope)
