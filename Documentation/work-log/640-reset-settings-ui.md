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

**Divergences from iOS, and why:**
- No separate success/failure follow-up alert. iOS's `performReset()` can fail (Core Data). Android's `resetToDefaults()`/`clearAll()` are simple Room/EncryptedSharedPreferences calls with no established failure path anywhere else in this codebase (`EditCategoryMenuViewModel`'s category delete doesn't catch either) — adding one here would be defensive handling for a scenario nothing else in the app guards against.
- Dialog title is explicit ("Reset App Settings?") rather than implied by the row, since Android's existing dialog block (see below) always renders a title.

## What changed

- **`ExitDialogType`** gained a third value, `RESET_APP_SETTINGS` (alongside the misnamed-but-accurate `PRIVACY_POLICY`/`CONTACT_DEVELOPERS`). Fixed the two existing entries' comments while touching this file — both were copy-pasted ("...while editing categories"), unrelated to what either dialog actually does.
- **Reused the Settings screen's one existing confirmation dialog** (`SettingsScreen.kt`'s `if (dialogOpen) { ... }` block) rather than building a new dialog primitive — it's the only confirmation-dialog implementation in the whole codebase (there is no delete-confirmation dialog anywhere else to mirror instead; category/phrase deletion has none today). The block's title/message/confirm-button copy now switch on `state.dialogType`, so the existing "Leaving the app" dialogs (Privacy Policy, Contact Developers) are untouched and the reset dialog gets its own copy.
- **New strings** (base `values/strings.xml` only — translations lag via the existing pipeline, matching this repo's current partial-translation state): `settings_reset_dialog_title`, `settings_reset_dialog_message`, `settings_reset_dialog_confirm`. Reused the existing `settings_reset_app` (row label) and `settings_dialog_cancel` (Cancel button) — both already existed; `settings_reset_app` was previously dead, referenced only by an unused legacy string-array.
- **Fifth `OptionItem` row** added to `SettingsScreen.kt`, reusing the existing `SettingsButton` composable — identical rendering/gaze/dwell behavior to the other four rows for free, no new component.
- **`SettingsViewModel`** gained an `ICategoriesUseCase` dependency (previously only took `IVocableSharedPreferences`) and:
  - `requestReset()` — opens the dialog (mirrors `requestPrivacyPolicy`/`requestContactDevs`).
  - A new `confirmDialog()` branch — the **first** branch needing `viewModelScope.launch`, since every prior branch was synchronous: `categoriesUseCase.resetToDefaults()` then `prefs.clearAll()`. `resetToDefaults()` internally also wipes/reseeds phrases (per #639), so this one call plus the prefs wipe is the complete reset.
- **Koin wiring**: `SettingsViewModel(get())` → `SettingsViewModel(get(), get())` (`AppKoinModule.kt`); `ICategoriesUseCase` was already a registered singleton.
- **`FakeCategoriesUseCase.resetToDefaults()`** filled in (was `TODO`) — resets its backing `_categories` `MutableStateFlow` to the same single-item list it starts with, so `SettingsViewModelTest` can assert the reset branch actually ran.
- **6 new `SettingsViewModelTest` cases**: `requestReset` opens the dialog, confirming it wipes both prefs and category state and clears the dialog, and cancelling after `requestReset` makes no changes — using the existing Turbine/`uiState.value` style, extending `createViewModel()` to accept both fakes.

## Why no dedicated mid-speech/onboarding guard was added

Per #640's own AC ("reset behaves correctly mid-speech/onboarding/first-install — no crash, no stuck state"), investigated whether any of these are reachable races in this app's actual architecture, rather than assuming they need new guard code:

- **Onboarding**: Settings is a route only inside `VocableNavHost`, which only exists inside `MainActivity`, which only launches after `SplashActivity` finishes its one-time preset population and posts `exitSplash = true`. There is no separate onboarding UI beyond that splash gate — Settings (and therefore this row) is structurally unreachable until first-run population has already fully completed. Not a reachable race to guard against.
- **Mid-speech TTS**: no destructive action anywhere in this codebase (including existing category/phrase delete) checks `VocableTextToSpeech.isSpeakingFlow` before running. This dialog's buttons don't pass an `accessibilityLabel` to `gazeClickable`, so they use the same 500ms non-speaking dwell-reset path as every other non-speaking button in the app (Cancel/Continue on the existing Privacy/Contact-Devs dialog use the identical path) — no new interaction with a concurrent utterance is introduced here.
- Manually verified on-device (see below) that triggering reset does not crash or leave the app in a stuck state; the Settings screen and home/presets screen both remain fully responsive immediately after.

## Verification

- `./gradlew testDebug` — full unit suite green, including 6 new `SettingsViewModelTest` cases (10 total in that file).
- `./gradlew assembleDebug` clean.
- **Manually driven on `emulator-5554`** (screenshots taken at each step): installed the debug build, opened Settings, confirmed "Reset App Settings" renders as the 5th row below Voice; tapped it and confirmed the dialog shows the exact title/message/button copy above; tapped Cancel and confirmed no visible change; reopened the dialog and tapped Reset, confirming it dismisses with no crash (checked `adb logcat` for exceptions — none) and the app remains fully usable (Settings screen and home/presets screen both render and respond normally afterward).

## Not done here (tracked elsewhere or genuinely out of scope)

- No true Compose UI/instrumented test was added confirming post-reset category/phrase state matches a fresh install exactly through the actual Settings screen interaction — #639's work-log already covers this at the use-case layer (`CategoriesUseCaseTest`/`PhrasesUseCaseTest`); adding a `SettingsScreen`-driven Compose test that also asserts full data-layer state would duplicate that coverage rather than test something new about the UI wiring itself.
- No success/failure follow-up alert (see iOS divergence above).
- Switch-input support is inherited automatically from `GazeButton`/standard focusable Compose views — this repo has no bespoke switch-access handling anywhere (documented gap in `CLAUDE.md`), so there was nothing switch-specific to add for this row/dialog beyond what every other row already gets.

## Branch note

Same stack as #638/#639: this branch (`feature/640/reset-settings-ui`) is based on `feature/639/reset-category-phrase-reseed`, which is based on `feature/638/reset-preference-wipe`, which is based on `feature/voice-selection`. PR targets `feature/639/reset-category-phrase-reseed`. Per this repo's sub-issue convention, `closes #640` won't auto-fire on merge into a non-default branch — close manually afterward with a link to the merged PR.

## Pointers

- Issue: #640 · Parent: #627 · Depends on: #638, #639
- iOS reference: `vocable-ios` `Vocable/Features/Settings/SettingsViewController.swift` (`presentAppResetPrompt`), `Vocable/CoreData/AppResetController.swift`
- Files: `ui/settings/{ExitDialogType,SettingsViewModel,SettingsScreen,SettingsState}.kt`, `ui/VocableNavHost.kt`, `di/AppKoinModule.kt`, `values/strings.xml`, `FakeCategoriesUseCase.kt`, `settings/SettingsViewModelTest.kt`
