# TTS: live device-voice fallback when nothing is explicitly selected

**Issue:** #632 (Part of #617, which is part of #613 — the overall Voice Selection feature)

## What was needed

`VocableTextToSpeech.applySelectedVoice()` early-returned whenever no voice had been explicitly picked in-app — meaning Vocable never read or applied the device's actual configured TTS voice in that case, it just left whatever the engine's internal state happened to default to at init. There was also no recovery path if a previously-picked voice became unavailable (e.g. removed after an OS/engine update): the code would just log a warning and silently keep speaking in whatever voice was already active, with the stale preference still persisted indefinitely.

The product decision (from #613/#617) is a "hybrid" model: Vocable should track the device's own default voice live, on demand, whenever nothing's explicitly chosen — but once a user picks something different in Vocable's own picker, that choice should stick across restarts even if the device's system voice changes later. Neither half of that existed in code before this change.

## What changed

- `VocableTextToSpeech.speak()` now returns a `Boolean` (previously `Unit`) — `true` means the caller's persisted voice selection was stale (no longer resolves to an installed voice) and should be cleared. All three existing call sites (`MainActivity.kt`, `PresetsViewModel.kt`, `KeyboardViewModel.kt`) were updated to react to this and call `setSelectedVoiceName(null)` when it happens.
- `applySelectedVoice()` now calls a new `applyLiveDefaultVoice()` (wrapping `TextToSpeech.getDefaultVoice()`) whenever `selectedVoiceName` is null/blank, instead of no-op-ing. This is checked fresh on every `speak()` call — never cached or persisted, per the resolved product decision.
- The actual "which case are we in" decision was extracted into a pure function, `resolveVoiceSelection(selectedVoiceName, availableVoiceNames) -> VoiceResolution`, kept free of any real `android.speech.tts` types. This exists specifically so it can be unit-tested: this module's JVM unit tests run against the plain Android SDK stub jar (no Robolectric, no mocking framework, per this repo's existing conventions), under which `Voice`/`TextToSpeech` can't actually be constructed. `VocableTextToSpeechTest.kt` covers all three branches (live default, explicit match, stale-fallback) this way.
- Existing `lastSetLocale` skip logic and the ordering (voice applied *after* `setLanguage()`, since `setLanguage()` clobbers a previously-applied voice) were preserved unchanged.

## Known verification gap — needs a real device or Play-Store-enabled emulator

The emulator available during this work has **no TTS engine installed at all** (not Google's, not any other) and no Play Store to add one. That means the actual `getDefaultVoice()` live-fallback behavior could only be verified via unit tests on the extracted pure decision logic, plus a no-crash/no-regression check on a real app launch — not the full manual-test plan #617 specifies (confirming the spoken voice actually matches a manually-set system voice, confirming the live check reflects a system-voice change mid-session, confirming an explicit pick survives a system-voice change). **Someone with a real device, or an emulator image with Google TTS installed, needs to run through those before this is considered fully verified** — not just before merge to `main`, but ideally before merging into `feature/voice-selection` too, since later sub-issues (the Settings UI) will build on top of this and inherit any latent bug here.

## Also done in this same unit of work

- Preserved the pre-Language-Selection-removal state of `feature/voice-selection` as branch `archive/language-selection-pre-removal` (from commit `b450b519`), since #617's own acceptance criteria required that work be kept on a branch rather than deleted outright, and PR #631 had already merged the removal before this was caught.
- Established the "Starting new work" standard workflow in `CLAUDE.md` (issue-first via the new `/create-ticket` skill, `feature/<issue>/<description>` branch naming, PR template, this work-log convention) — this doc is itself the first application of that new standard.

## Pointers

- Issue: #632 · Parent: #617 → #613
- PR: (see PR opened from `feature/617/tts-live-voice-fallback` against `feature/voice-selection`)
- New skill: `.claude/skills/create-ticket/SKILL.md`
