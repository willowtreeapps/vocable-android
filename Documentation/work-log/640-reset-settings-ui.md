# Reset: Settings UI entry point + confirmation + accessibility

**Issue:** #640 (Part of #627 — Reset App Settings epic)

## What was needed

#638 and #639 built the reset mechanics (`IVocableSharedPreferences.clearAll()`, `ICategoriesUseCase.resetToDefaults()`) but neither wired them to anything a user can actually trigger. #640 adds the missing "Reset App Settings" row and a confirmation dialog to the Settings screen, since the action is total and irreversible.

## iOS reference

Read the shipped implementation in `../vocable-ios` (`SettingsViewController.swift`, `AppResetController.swift`) before designing this, per this repo's cross-repo convention:

- Row is **last** in the internal-settings list (Voice, Categories, Timing, Listening Mode, Selection Mode, then Reset), styled identically to every other row — no special "danger" treatment on the row itself.
- Confirmation copy (from `Localizable.xcstrings`): title implicit in the row itself, body **"Are you sure you want to reset Vocable to default settings? This action cannot be undone."**, buttons **Cancel** / **Reset** (destructive-styled).
- `performReset()` returns a `Bool` and iOS follows up with a separate success/failure alert.
- No mid-speech/TTS guard exists anywhere in iOS's reset path — confirmed this is simply unaddressed on iOS, not a pattern to copy defensively.
- Dialog buttons are the same generic `GazeableButton` used everywhere else in the app — no reset-specific gaze handling.

Copy matches iOS's `Localizable.xcstrings` exactly (pulled the raw JSON rather than trusting a paraphrase): body **"Are you sure you want to reset Vocable to default settings? This action cannot be undone."**, Cancel **"Cancel"**, Confirm **"Reset"**. iOS's alert has no separate title key — only a body — so since Android's existing dialog block always renders a title slot, it reuses the row's own label (`settings_reset_app`, "Reset App Settings") rather than inventing new title wording. First draft of this dialog used invented copy ("This will permanently erase all custom categories, phrases, and settings...") before being corrected to match the shipped iOS text.

**Divergences from iOS, and why:**
- No separate success/failure follow-up alert. iOS's `performReset()` can fail (Core Data). Android's `resetToDefaults()`/`clearAll()` are simple Room/EncryptedSharedPreferences calls with no established failure path anywhere else in this codebase (`EditCategoryMenuViewModel`'s category delete doesn't catch either) — adding one here would be defensive handling for a scenario nothing else in the app guards against.

## What changed

- **`ExitDialogType`** gained a third value, `RESET_APP_SETTINGS` (alongside the misnamed-but-accurate `PRIVACY_POLICY`/`CONTACT_DEVELOPERS`). Fixed the two existing entries' comments while touching this file — both were copy-pasted ("...while editing categories"), unrelated to what either dialog actually does.
- **Reused the Settings screen's one existing confirmation dialog** (`SettingsScreen.kt`'s `if (dialogOpen) { ... }` block) rather than building a new dialog primitive — it's the only confirmation-dialog implementation in the whole codebase (there is no delete-confirmation dialog anywhere else to mirror instead; category/phrase deletion has none today). The block's title/message/confirm-button copy now switch on `state.dialogType`, so the existing "Leaving the app" dialogs (Privacy Policy, Contact Developers) are untouched and the reset dialog gets its own copy.
- **New strings** (base `values/strings.xml` only — translations lag via the existing pipeline, matching this repo's current partial-translation state): `settings_reset_dialog_message`, `settings_reset_dialog_confirm`. Reused the existing `settings_reset_app` (row label, doubles as the dialog title) and `settings_dialog_cancel` (Cancel button) — both already existed; `settings_reset_app` was previously dead, referenced only by an unused legacy string-array.
- **Fifth `OptionItem` row** added to `SettingsScreen.kt`, reusing the existing `SettingsButton` composable — identical rendering/gaze/dwell behavior to the other four rows for free, no new component.
- **`SettingsViewModel`** gained an `ICategoriesUseCase` dependency (previously only took `IVocableSharedPreferences`) and:
  - `requestReset()` — opens the dialog (mirrors `requestPrivacyPolicy`/`requestContactDevs`).
  - A new `confirmDialog()` branch — the **first** branch needing `viewModelScope.launch`, since every prior branch was synchronous: `categoriesUseCase.resetToDefaults()` then `prefs.clearAll()`. `resetToDefaults()` internally also wipes/reseeds phrases (per #639), so this one call plus the prefs wipe is the complete reset.
- **Koin wiring**: `SettingsViewModel(get())` → `SettingsViewModel(get(), get())` (`AppKoinModule.kt`); `ICategoriesUseCase` was already a registered singleton.
- **`FakeCategoriesUseCase.resetToDefaults()`** filled in (was `TODO`) — resets its backing `_categories` `MutableStateFlow` to the same single-item list it starts with, so `SettingsViewModelTest` can assert the reset branch actually ran.
- **6 new `SettingsViewModelTest` cases**: `requestReset` opens the dialog, confirming it wipes both prefs and category state and clears the dialog, and cancelling after `requestReset` makes no changes — using the existing Turbine/`uiState.value` style, extending `createViewModel()` to accept both fakes.
- **Fixed a pre-existing dialog text-color bug**, caught during manual on-device verification (Cancel/Reset rendered in a washed-out pale mint instead of the intended dark navy on the dialog's white background). Root cause: the app's shared `Typography.labelLarge` (`Type.kt`, Material3 `Button`'s default content text style) hardcodes `color = TextColor`. Since that's an explicit color rather than `Color.Unspecified`, it wins over `LocalContentColor` for any `Text()` that doesn't set its own `color=` — so `VocableButton`'s `textColor` param is silently ignored whenever a button uses a non-default color, exactly what these two dialog buttons do (`textColor = ColorPrimaryDark` on a white surface). Fixed by adding an explicit `color = ColorPrimaryDark` to both `Text()` calls — the same latent bug also affects the pre-existing Privacy Policy/Contact Developers Cancel/Continue buttons (same code path, unchanged before this ticket), fixed for free by touching this shared block. Did not touch the shared `Typography`/`VocableButton` default itself, since that's a wider blast radius than this ticket's scope.

## Critical fix found during manual verification: Selection Mode didn't reflect reset

Manually testing the real reset flow on a physical device (not just unit tests) surfaced a genuine correctness bug that no automated test in this codebase would have caught: after tapping Reset, the Settings → Selection Mode screen kept showing "Head Tracking" as whatever it was *before* the reset, instead of flipping back to the default (on) — the actual ARCore tracking session stayed off too, not just the toggle's visual state.

**Root cause, two layers deep:**

1. `SelectionModeViewModel.headTrackingEnabled` is bound to `IFaceTrackingPermissions.permissionState` (`FaceTrackingPermissions.kt`), an in-memory `MutableStateFlow` that's only updated by its own `enableFaceTracking()`/`disableFaceTracking()` methods — never by observing `IVocableSharedPreferences` directly. `clearAll()` doesn't call either of those, so this cached state was structurally guaranteed to go stale on reset, unlike `SensitivityViewModel`/`SettingsVoiceViewModel`, which read prefs fresh at construction and so happen to self-correct on next screen visit (nav-scoped, not activity-scoped).
2. Fixing that alone (adding an `OnSharedPreferenceChangeListener` inside `FaceTrackingPermissions` to sync `permissionState` off `KEY_HEAD_TRACKING_ENABLED`, mirroring the identical pattern already in `FaceTrackingViewModel`) *still didn't work* when tested. The deeper cause: **`VocableSharedPreferences.clearAll()`'s bare `encryptedPrefs.edit { clear() }` never fires `OnSharedPreferenceChangeListener` at all.** Android's `SharedPreferences` only adds a key to its notified-keys set when that key is explicitly `put`/`removed` within an edit; a pure `clear()` with no accompanying puts silently wipes the store without notifying anyone. This meant *every* listener-driven live component in the app — `GazeButton`'s dwell-time listener, `FaceTrackingViewModel`'s sensitivity/head-tracking listener, and the new `FaceTrackingPermissions` one — was already broken by `clearAll()` itself, not just the one this ticket happened to surface by adding a new consumer.

**Fix**, in `VocableSharedPreferences.clearAll()`: keep the `clear()` (a genuine safety net for any future/unknown key), then follow it with a **second edit that explicitly writes back the default value** for every key a live listener depends on (`mySayings`, `dwellTime`, `sensitivity`, `headTrackingEnabled`, `firstTime`). Explicit `put`s always register as changed keys regardless of prior value, so this reliably fires every listener immediately — same net final state as before, now actually observed by everything watching it. `selectedVoiceName` is deliberately left out of the follow-up write (its default is *absence* of the key, and nothing holds a live listener on it — every voice-related screen re-reads it fresh per `koinViewModel()` navigation, per #638's investigation).

Also added `FaceTrackingPermissions`'s missing `OnSharedPreferenceChangeListener` (needed regardless, once `clearAll()` was fixed) and a new androidTest, `VocableSharedPreferencesTest`, with a regression case (`clearAll_notifiesListenersForLiveObservedKeys`) asserting a registered listener actually fires for `KEY_DWELL_TIME`/`KEY_SENSITIVITY`/`KEY_HEAD_TRACKING_ENABLED` —— this is the first test in the repo exercising the *real* `EncryptedSharedPreferences`-backed class rather than the fake, since this specific bug class (Android's `clear()` vs. listener semantics) can't be observed through the fake at all.

Verified the full fix on-device: disabled Head Tracking via Selection Mode, reset from Settings, and confirmed Selection Mode immediately showed it back on **and** the ARCore session had genuinely resumed (the "Head tracking is paused, move your head to resume" banner only renders while tracking is actually active) — no navigation or app restart needed.

## Why no dedicated mid-speech/onboarding guard was added

Per #640's own AC ("reset behaves correctly mid-speech/onboarding/first-install — no crash, no stuck state"), investigated whether any of these are reachable races in this app's actual architecture, rather than assuming they need new guard code:

- **Onboarding**: Settings is a route only inside `VocableNavHost`, which only exists inside `MainActivity`, which only launches after `SplashActivity` finishes its one-time preset population and posts `exitSplash = true`. There is no separate onboarding UI beyond that splash gate — Settings (and therefore this row) is structurally unreachable until first-run population has already fully completed. Not a reachable race to guard against.
- **Mid-speech TTS**: no destructive action anywhere in this codebase (including existing category/phrase delete) checks `VocableTextToSpeech.isSpeakingFlow` before running. This dialog's buttons don't pass an `accessibilityLabel` to `gazeClickable`, so they use the same 500ms non-speaking dwell-reset path as every other non-speaking button in the app (Cancel/Continue on the existing Privacy/Contact-Devs dialog use the identical path) — no new interaction with a concurrent utterance is introduced here.
- Manually verified on-device (see below) that triggering reset does not crash or leave the app in a stuck state; the Settings screen and home/presets screen both remain fully responsive immediately after.

## Verification

- `./gradlew testDebug` — full unit suite green, including 6 new `SettingsViewModelTest` cases (10 total in that file).
- `./gradlew connectedDebugAndroidTest` — full instrumented suite green on a physical device (53 tests, 0 failed, 4 pre-existing unrelated skips), including the 2 new `VocableSharedPreferencesTest` cases.
- `./gradlew assembleDebug` clean.
- **Manually driven, first on `emulator-5554` then on a physical Samsung device** (screenshots at each step): installed the debug build, opened Settings, confirmed "Reset App Settings" renders as the 5th row below Voice; tapped it and confirmed the dialog shows the exact title/message/button copy above; tapped Cancel and confirmed no visible change; reopened the dialog and tapped Reset, confirming it dismisses with no crash (checked `adb logcat` for exceptions — none) and the app remains fully usable afterward. Switching to a physical device (real camera/ARCore, real head-tracking session) is what surfaced the Selection Mode bug above — the emulator pass alone would not have caught it, since it can't exercise a real tracking session.

## Not done here (tracked elsewhere or genuinely out of scope)

- No true Compose UI/instrumented test was added confirming post-reset category/phrase state matches a fresh install exactly through the actual Settings screen interaction — #639's work-log already covers this at the use-case layer (`CategoriesUseCaseTest`/`PhrasesUseCaseTest`); adding a `SettingsScreen`-driven Compose test that also asserts full data-layer state would duplicate that coverage rather than test something new about the UI wiring itself.
- No success/failure follow-up alert (see iOS divergence above).
- Switch-input support is inherited automatically from `GazeButton`/standard focusable Compose views — this repo has no bespoke switch-access handling anywhere (documented gap in `CLAUDE.md`), so there was nothing switch-specific to add for this row/dialog beyond what every other row already gets.

## Branch note

Same stack as #638/#639: this branch (`feature/640/reset-settings-ui`) is based on `feature/639/reset-category-phrase-reseed`, which is based on `feature/638/reset-preference-wipe`, which is based on `feature/voice-selection`. PR targets `feature/639/reset-category-phrase-reseed`. Per this repo's sub-issue convention, `closes #640` won't auto-fire on merge into a non-default branch — close manually afterward with a link to the merged PR.

## Pointers

- Issue: #640 · Parent: #627 · Depends on: #638, #639
- iOS reference: `vocable-ios` `Vocable/Features/Settings/SettingsViewController.swift` (`presentAppResetPrompt`), `Vocable/CoreData/AppResetController.swift`
- Files: `ui/settings/{ExitDialogType,SettingsViewModel,SettingsScreen,SettingsState}.kt`, `ui/VocableNavHost.kt`, `di/AppKoinModule.kt`, `values/strings.xml`, `FakeCategoriesUseCase.kt`, `settings/SettingsViewModelTest.kt`, `core/{VocableSharedPreferences,FaceTrackingPermissions}.kt`, `core/VocableSharedPreferencesTest.kt` (new, androidTest)
