# Reset Individual Settings — per-domain resets + granular Reset App Settings screen

**Issue:** #672 ("Feature: Reset Individual Settings") — implemented directly per product decision,
without a follow-up implementation ticket or PR (see Pointers).

## What was needed

Before this change the only reset affordance was a single "Reset App Settings" action (from #627)
that wiped every setting *and* every user-added phrase/category at once. #672 asked for a way to
reset one thing without losing unrelated content, and left the exact shape as an open design
decision (two competing options in the ticket, deferred to design). The decision was made directly
by the ticket author: ship both proposed mechanisms together rather than picking one.

## What changed

- **A reusable `ConfirmationDialog` composable** (`ui/components/ConfirmationDialog.kt`), extracted
  from the dialog that used to live inline in `SettingsScreen.kt`. Every reset flow below uses it.
  It takes an `isDestructive` flag that tints the confirm action `ErrorColor` (`0xFFAD006C`) instead
  of `ColorPrimaryDark` — chosen because that hex already matches iOS's `ErrorRed` token exactly, so
  no new color was needed.
- **Five per-screen reset icons**, each with its own confirmation dialog, each scoped to only its
  own domain:
  - Voice (`SettingsVoiceScreen`) — clears the selected voice name (falls back to system default).
  - Timing and Sensitivity (`SensitivityScreen`) — new `IVocableSharedPreferences.resetSensitivity()`.
  - Selection Mode (`SelectionModeScreen`) — new `IFaceTrackingPermissions.resetToDefault()`.
  - Categories and Phrases (`EditCategoriesScreen`) — two icons, "Reset Categories" and "Reset
    Phrases" (see decisions below for why both live on this one screen).
- **A new "Reset App Settings" screen** (`ui/resetsettings/`) reachable from the main Settings
  screen, with one checkbox per domain for granular reset ("Reset Selected") plus a separate
  "Reset Everything" nuclear action styled in `ErrorColor`. Both are gated behind
  `ConfirmationDialog`. The Settings screen's existing "Reset App Settings" row now navigates here
  instead of opening a dialog directly — the dialog it used to open (`ExitDialogType.RESET_APP_SETTINGS`)
  was removed.
- **`ICategoriesUseCase.resetCategoriesToDefaults()`** — new, categories-only reset (see below for
  why `resetToDefaults()` couldn't be reused as-is).

## Key decisions, and why

### Recents has no separate control

The original ticket callout ("Category = 'Recents', reset removes all") is handled for free:
Recents is derived from `lastSpokenDate` with no dedicated table, so resetting Categories or Phrases
already empties it, matching how the existing nuclear reset already behaves. Confirmed with the
user rather than building a distinct control.

### Why `resetCategoriesToDefaults()` is a new method, not a reuse of `resetToDefaults()`

`CategoriesUseCase.resetToDefaults()` (the nuclear-reset path) calls `phrasesUseCase.resetToDefaults()`
first — it was always a combined categories+phrases wipe. That's wrong for a "Categories only" domain
reset: it would also silently wipe every phrase, including ones in preset categories the user never
touched. The new method instead: deletes phrases belonging to *removed* user-added categories only
(so nothing is orphaned), then resets preset category order/hidden state and deletes user-added
categories. Phrases in categories that remain — preset or shadowed — are left alone. `resetToDefaults()`
itself is unchanged and still backs the "Reset Everything" nuclear option, matching prior behavior
exactly.

### Why Categories and Phrases resets both live on `EditCategoriesScreen`

The domain list in the ticket ("phrases just resets phrases... categories resets categories") assumes
one screen per domain, but there is no single "Phrases" screen in the nav graph — phrases are only
ever viewed scoped to one category, via `EditCategoryPhrasesScreen`. Putting a "reset every phrase
everywhere" icon on a single-category phrase editor would be surprising. Both icons were placed on
`EditCategoriesScreen` instead (reached via the "Categories and Phrases" Settings row), since that's
the closest existing entry point representing the combined domain. They use the same `ic_undo` icon
and are only distinguished by position/accessibility label/dialog title — a real rough edge, flagged
for a design pass rather than solved here (see Pointers).

### Selection Mode reset goes through `IFaceTrackingPermissions`, not raw prefs

`SelectionModeViewModel` never depended on `IVocableSharedPreferences` directly — it depends on
`IFaceTrackingPermissions`, which already had a documented comment noting `permissionState` stays in
sync with the preference "even when it's changed by something other than enableFaceTracking() /
disableFaceTracking() — e.g. a settings reset." The new `resetToDefault()` on that interface writes
the pref and emits the new state directly (mirroring `enableFaceTracking()`/`disableFaceTracking()`),
rather than re-triggering the camera permission request flow that `requestFaceTracking()` would
trigger — a reset icon shouldn't pop a permission dialog.

### `ResetSettingsViewModel` had to be Activity-scoped, not a plain `viewModel {}`

Koin's `IFaceTrackingPermissions` binding only has a real implementation inside
`scope<MainActivity> { ... }`; everywhere else resolves to a no-op fallback (`factory<IFaceTrackingPermissions>`
at module top level). `SelectionModeViewModel` already existed inside that scope and is resolved via
`mainActivity.getViewModel()` rather than `koinViewModel()`. `ResetSettingsViewModel` was placed in
the same scope and resolved the same way in `VocableNavHost` — otherwise its Selection Mode checkbox
would silently no-op.

## Verification

`./gradlew testDebugUnitTest` — all suites pass, including new/updated ones:
`SensitivityViewModelTest` (new), `ResetSettingsViewModelTest` (new), `EditCategoriesViewModelTest`
(new, JVM fake-based — a separate Room-backed `EditCategoriesViewModelTest` already existed under
`androidTest` and was updated for the new constructor param and reset intents),
`SelectionModeViewModelTest`, `SettingsVoiceViewModelTest`, `SettingsViewModelTest` (all extended).
`./gradlew assembleDebug assembleDebugAndroidTest` compiles clean. androidTest cases (`CategoriesUseCaseTest`,
`EditCategoriesViewModelTest`) were not run against a device/emulator in this environment — compile-verified only.

Not done: manual verification in a running app (no device/emulator available in this session), and
no design sign-off — the original ticket explicitly gated the UI on design sign-off, which this pass
skips per direct product decision. The dual `ic_undo` icon ambiguity on `EditCategoriesScreen` is the
most likely thing a design pass would change.

## Pointers

- Issue: #672. No sub-issue/implementation ticket was opened and no PR was created for this pass —
  done directly on `feature/672/reset-individual-settings` per explicit instruction, committed
  locally only.
- iOS (`willowtreeapps/vocable-ios`) has no per-domain reset today — confirmed via repo search. Only
  its nuclear-reset alert style (`GazeableAlertViewController`, `.destructive` → `ErrorRed` foreground
  text on clear background) and copy pattern ("Are you sure you want to reset ... to default settings?
  This action cannot be undone.") were prior art to mirror; the granular/per-domain mechanism itself
  is new ground for both platforms.
