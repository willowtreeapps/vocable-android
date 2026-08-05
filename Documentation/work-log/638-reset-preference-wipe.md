# Reset: full preference wipe + defaults inventory

**Issue:** #638 (Part of #627 — Reset App Settings epic)

## What was needed

#627 requires Android's reset to mirror iOS's `AppResetController.performReset()`: an unconditional wipe of every stored preference, not a curated subset, with each one falling back to a defined default on next read. Before any reset trigger can be built (#640) or the category/phrase wipe added (#639), this ticket needed a confirmed inventory of every `IVocableSharedPreferences`-backed setting and proof each has a real default — plus a wipe mechanism that's actually reachable through the interface, not just the concrete class.

## What changed

- **Inventoried every preference** on `IVocableSharedPreferences`/`VocableSharedPreferences`: `mySayings` (empty list), `dwellTime` (1000L), `sensitivity` (0.1F), `headTrackingEnabled` (true), `selectedVoiceName` (null/unset), `firstTime` (true). All six already had correct defaults — no default-value bugs found. One unused legacy constant, `KEY_MY_LOCALIZED_SAYINGS`, exists but isn't exposed by any getter/setter; left untouched since it's dead weight, not a live preference, and `clearAll()`'s `SharedPreferences.clear()` wipes the whole encrypted store regardless of which keys are named as constants.
- **Promoted `clearAll()` to `IVocableSharedPreferences`.** The wipe itself (`encryptedPrefs.edit(commit = true) { clear() }`) already existed and was already a full, unconditional store clear — it just lived only on the concrete `VocableSharedPreferences` class, reachable only by callers holding that concrete type instead of the interface everything else in this codebase is injected against. It's now an interface method so a future reset use case (#640) can call it through normal DI.
- **Named the two remaining inline defaults**: `DEFAULT_HEAD_TRACKING_ENABLED` and `DEFAULT_FIRST_TIME` (both `true`), alongside the existing `DEFAULT_SENSITIVITY`/`DEFAULT_DWELL_TIME` constants, so every default has one canonical source instead of a literal buried in a `getX(key, true)` call.
- **Added `FakeVocableSharedPreferences.clearAll()`**, resetting all six backing fields to the same production defaults (via the named constants above, not re-typed literals).
- **Fixed a pre-existing bug in the fake**: `setFirstTime()` set `firstTime = true`, the inverse of production's `setFirstTime()` (which writes `false`, marking first-launch as consumed). Caught because writing the reset test required reasoning about this preference's real semantics. No existing test relied on the old behavior — `SplashViewModel`, the only caller of `setFirstTime()`/`getFirstTime()`, has no unit test yet (already noted as a coverage gap in `CLAUDE.md`).
- **Added `VocableSharedPreferencesResetTest`** (`app/src/test/java/com/willowtree/vocable/core/`), one test per preference: seed a non-default value via the fake's constructor, call `clearAll()`, assert the getter returns the documented default.

## Key decision: testing strategy

`VocableSharedPreferences` is backed by `EncryptedSharedPreferences`, which needs a real `Context`/Android Keystore — there's no Robolectric in this repo (per `CLAUDE.md`), so it can't be exercised from a JVM unit test. The reset test instead pins the contract via `FakeVocableSharedPreferences`, which mirrors `IVocableSharedPreferences` and now shares the same default constants as production. This documents and regression-guards the expected default for each preference; it does not exercise the real encrypted store's `clear()` call, which remains implicitly covered by `VocableKoinTestRule` resetting real prefs between instrumented tests.

## Resolved: voice preference reset behavior

#627's original open PM decision — whether Android's voice-selection reset should mirror iOS's incidental `nil` fallback or deliberately restore a chosen default — was resolved before this ticket started: per Chris Stroud (Slack, 2026-08-04), an unset `selectedVoiceName` (falling back to the system voice) is the correct, intended end state, matching iOS and a fresh install. `getSelectedVoiceName()`'s existing `null` default already satisfies this; no code change was needed for it specifically. `Documentation/reset-app-settings-ios-reference.md` still describes this as an open/unresolved question and is stale — the resolution lives in #627's issue body, not that doc.

## Branch note

This branch (`feature/638/reset-preference-wipe`) is based on `feature/voice-selection`, not `main`, because `selectedVoiceName`/`firstTime` (and the interface methods for them) don't exist on `main` yet — voice selection (#613) hasn't merged. The PR targets `feature/voice-selection` accordingly; per this repo's convention for sub-issue PRs targeting a parent's integration branch, `closes #638` won't auto-fire on merge, so the issue needs to be closed manually afterward with a link to the merged PR.

## Out of scope (tracked in sibling issues under #627)

- Category/phrase data reset (#639)
- Reset UI entry point/confirmation dialog and actually wiring `clearAll()` up to a user-facing action (#640)

## Pointers

- Issue: #638 · Parent: #627 · Reference: `Documentation/reset-app-settings-ios-reference.md`
- Files: `core/IVocableSharedPreferences.kt`, `core/VocableSharedPreferences.kt`, `utils/FakeVocableSharedPreferences.kt`, `core/VocableSharedPreferencesResetTest.kt` (new)
