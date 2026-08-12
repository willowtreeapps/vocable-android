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
  - Categories (`EditCategoriesScreen`) — new `ICategoriesUseCase.resetCategoriesToDefaults()`.
  - Phrases (`EditCategoryPhrasesScreen`, one per category) — new
    `IPhrasesUseCase.resetPhrasesForCategory(categoryId)`, scoped to just that category (see
    decisions below — this replaced an earlier draft that put both icons on `EditCategoriesScreen`).
  All icons use a new `ic_reset` drawable (a circular-arrow/clear icon supplied directly by the
  ticket's requester as an SVG, transcribed into a vector drawable), replacing the placeholder
  `ic_undo` used in an earlier draft of this change.
- **A new "Reset App Settings" screen** (`ui/resetsettings/`) reachable from the main Settings
  screen, with one checkbox per domain for granular reset ("Reset Selected") plus a separate
  "Reset Everything" nuclear action styled in `ErrorColor`. Both are gated behind
  `ConfirmationDialog`. The Settings screen's existing "Reset App Settings" row now navigates here
  instead of opening a dialog directly — the dialog it used to open (`ExitDialogType.RESET_APP_SETTINGS`)
  was removed. Its "Phrases" checkbox stays global-scope (`phrasesUseCase.resetToDefaults()`) —
  deliberately different scope than the per-category icon above; see decisions below.

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

### Phrases reset is per-category, on `EditCategoryPhrasesScreen` — not global, not on `EditCategoriesScreen`

An earlier draft of this change put both "Reset Categories" and "Reset Phrases" icons on
`EditCategoriesScreen`, reasoning that there's no single global "Phrases" screen in the nav graph.
Direct feedback from the ticket requester overrode that: the Categories screen should only reset
categories, and *each* category's own phrase-editing screen (`EditCategoryPhrasesScreen`) should
have its own phrase reset, scoped to that one category — not a "reset every phrase everywhere"
control living somewhere unrelated.

That requires a real category-scoped reset, which didn't exist: `PhrasesUseCase.resetToDefaults()`
is global (delete every stored + preset phrase, repopulate every preset category). The new
`resetPhrasesForCategory(categoryId)` instead: hard-deletes stored (custom + shadow) phrases for
just that category via `getPhrasesForCategoryFlow(categoryId).first()` + `deletePhrase()` per row,
then hard-deletes that category's `PresetPhrase` rows via a new
`PresetPhrasesDao.deletePresetPhrasesForCategory()` query, then calls `populateDatabase()`. The hard
delete (vs. `PhrasesUseCase.deletePhrase()`'s existing soft-delete-and-never-reinsert behavior,
which is correct for a user manually deleting one phrase) matters: `ensurePopulated()`'s
existing-row check only looks at whether a row exists at all, not its `deleted` flag, so a
soft-deleted preset phrase would never come back. Hard-deleting first, then repopulating, is what
lets a preset category's phrases actually restore to default — the same trick the global
`resetToDefaults()` already relies on (`deleteAllPhrases()` is also a hard delete). For a
user-created category with no presets, `populateDatabase()` is a no-op for that id, so the reset
correctly just empties it with nothing to restore.

The Reset App Settings screen's "Phrases" checkbox is intentionally *not* changed to per-category —
it's a different, legitimate scope (bulk reset across every category from one place), kept as the
existing global `resetToDefaults()`.

### Icon-in-header placement caused title text to overflow behind it

Every per-screen reset icon was first added to each screen's `ConstraintLayout` header, anchored
`end.linkTo(parent.end)` (or, on `EditCategoriesScreen`, one icon further from the add button).
Titles were left on their original constraints — mostly `centerHorizontallyTo(parent)` or an
`end.linkTo(parent.end, margin = backButtonSize + 16.dp)` sized for the *old*, icon-free layout —
without a `width = Dimension.fillToConstraints`. Compose's `ConstraintLayout` doesn't shrink a
composable to fit between two anchors unless told to with an explicit `Dimension`; left as
`wrapContent`, a title long enough to want the reserved space rendered past its anchor and under
the new icon. Fixed on every affected screen (`SettingsVoiceScreen`, `SensitivityScreen`,
`SelectionModeScreen`, `EditCategoriesScreen`, `EditCategoryPhrasesScreen`) by re-anchoring each
title's `start`/`end` to its two nearest neighbor buttons with `width = Dimension.fillToConstraints`,
`textAlign = Center` (where not already center-ish), and `maxLines = 1` + ellipsis as a backstop.

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
(new JVM fake-based version — a separate Room-backed `EditCategoriesViewModelTest` already existed
under `androidTest` and was updated for the reset intent, minus the phrases branch this pass
removed), `EditCategoryPhrasesViewModelTest` (new), `SelectionModeViewModelTest`,
`SettingsVoiceViewModelTest`, `SettingsViewModelTest` (all extended).
`./gradlew assembleDebug assembleDebugAndroidTest` compiles clean. androidTest cases
(`CategoriesUseCaseTest`, `PhrasesUseCaseTest`, `EditCategoriesViewModelTest`) were not run against
a device/emulator in this environment — compile-verified only.

Not done: manual verification in a running app (no device/emulator available in this session), and
no design sign-off — the original ticket explicitly gated the UI on design sign-off, which this pass
skips per direct product decision.

## Pointers

- Issue: #672. No sub-issue/implementation ticket was opened and no PR was created for this pass —
  done directly on `feature/672/reset-individual-settings` per explicit instruction, committed
  locally only.
- iOS (`willowtreeapps/vocable-ios`) has no per-domain reset today — confirmed via repo search. Only
  its nuclear-reset alert style (`GazeableAlertViewController`, `.destructive` → `ErrorRed` foreground
  text on clear background) and copy pattern ("Are you sure you want to reset ... to default settings?
  This action cannot be undone.") were prior art to mirror; the granular/per-domain mechanism itself
  is new ground for both platforms.
