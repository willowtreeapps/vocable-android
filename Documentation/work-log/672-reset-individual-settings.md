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

## Follow-up pass (2026-08-18): per-category reset, remove the per-screen icons, accessible Reset App Settings screen

The five per-screen reset icons above ended up duplicating the centralized Reset App Settings
screen's per-domain checkboxes — the same action (e.g. "reset Voice") was reachable two ways, with
two separate confirmation dialogs. Direct follow-up feedback: keep one path per action. This pass
removes the per-screen icons and instead adds a category-scoped reset where it's actually distinct
from anything the centralized screen offers — resetting a single non-custom category's phrases from
its own menu.

### `Reset Category` button on `EditCategoryMenuScreen`, gated to non-custom categories

Added next to `Remove Category`, same full-width/icon+bold-text/`ErrorColor` treatment, behind a
`ConfirmationDialog` (title/button text: new `reset_category` string; message: the existing
`reset_category_phrases_dialog_message`, reused as-is since it already said exactly this — "reset
the phrases in this category to their defaults and remove any you've added or edited"). Wired to
the same `IPhrasesUseCase.resetPhrasesForCategory(categoryId)` the old `EditCategoryPhrasesScreen`
icon used.

Gating criterion is `category !is Category.StoredCategory` — the same check
`EditCategoryPhrasesScreen` already used for `isCustomCategory` (inverted), kept for consistency
rather than introducing a second way to ask the same question. This means a preset category that's
been renamed (which converts it to a `StoredCategory` shadow row, see `CategoriesUseCase.updateCategoryName`)
loses the Reset Category option going forward — a pre-existing limit of how "custom" is detected
app-wide, not something new to this pass.

`EditCategoryMenuScreen`'s action list was already conditionally hidden top-down when the screen is
too short to fit all of them (`visibleActionCount`, capped at 4: Rename, Toggle Show, Edit Phrases,
Remove). `Reset Category` is a 5th slot for non-custom categories (`maxActionCount` is 5 only then),
rendered *above* `Remove Category` — a less-destructive action (restores defaults) reads better
placed before the fully-destructive one (deletes the category outright). The reveal priority moved
to match the visual order: `showReset` is now the 4th slot's condition and `showRemove` the 5th (for
non-custom categories only — for custom categories, which never show Reset, Remove stays the 4th
slot exactly as before), so a cramped screen still reveals top-to-bottom in the order buttons are
actually drawn, rather than revealing Remove before the Reset button positioned above it.

### Removed: the five per-screen reset icons, their dialogs, and their now-dead ViewModel/state code

`EditCategoriesScreen` (all-categories reset), `EditCategoryPhrasesScreen` (per-category phrases —
superseded by the button above), `SettingsVoiceScreen`, `SensitivityScreen`, `SelectionModeScreen`.
Each screen's header `ConstraintLayout` had its title re-anchored back to a single neighbor (back
button, or back+add button) now that there's only one or two header buttons instead of two or three.
Deleted the now-fully-unused strings (`reset_voice_title`/`_dialog_message`,
`reset_sensitivity_title`/`_dialog_message`, `reset_selection_mode_title`/`_dialog_message`,
`reset_categories_title`/`_dialog_message`, `reset_phrases_title`) — verified via grep that nothing
else referenced them before removing. `EditCategoryPhrasesViewModelTest` was deleted outright: every
test in it existed solely to cover the removed reset intent: the same coverage now lives in
`EditCategoryMenuViewModelTest` against the new intent, and the underlying
`resetPhrasesForCategory` use-case behavior was already independently covered by
`PhrasesUseCaseTest` (androidTest).

### `ResetSettingsScreen` accessibility rework

Three asks, addressed together since they interact:

- **Checkmarks → bullet dots, rows read as gaze buttons.** Swapped the Material `Checkbox` for a
  small custom outlined/filled circle (`ResetSelectionBullet`) and made each domain row's whole
  background flip to `SelectedColor`/`ColorPrimaryDark` when checked — the same selected-state
  pattern `SensitivityButton`'s low/medium/high picker already uses elsewhere in this app. A tiny
  checkbox off to the side is a small, separate-feeling target; a full-row color flip plus a bullet
  reads as one big gaze-dwellable toggle, consistent with how the rest of the app signals "this is
  selected."
- **Column count degrades gracefully, one full-width button per row by default.** The old layout was
  a single scrolling `Column` — workable in portrait, but a short landscape phone (e.g. 800×400dp)
  couldn't fit 5×72dp rows plus two action buttons without scrolling, and this app avoids
  scroll-driven gaze interaction (dwell-clicking through a moving list isn't a pattern used anywhere
  else here). Went through two intermediate designs before landing here: first, 2 columns in
  landscape only; then, following feedback that dropped the per-row description (it only repeated
  what the confirmation dialog already says), a fixed 3-column grid in both orientations. Final
  feedback: prefer one full-width button per row (more readable) and only add columns when there
  isn't vertical room for that — so the screen now measures available height and tries 1 column,
  then 2, then 3, picking the smallest column count where all 5 domains still fit on one page; a
  local `columns` value (not `ResetSettingsState` — purely a derived layout choice, same pattern
  `EditCategoryMenuScreen`'s `visibleActionCount` already uses) drives both the grid and each row's
  internal layout: a full-width row lays the label and bullet out side by side (reads more
  naturally at that width), while a narrower 2-or-3-column cell stacks a shrink-to-fit label
  (`BasicText`/`TextAutoSize.StepBased`, the same narrow-column approach `SensitivityButton` already
  uses) above the bullet instead, since truncating a two-word label like "Selection Mode" with
  ellipsis at ⅓ width reads worse than shrinking it to two lines. The two action buttons
  (`Reset Selected`, `Reset Everything`) still sit side-by-side in landscape instead of stacked,
  independent of the domain-grid column count.
- **Pagination as the fallback, not the default.** Even the densest grid (3 columns) doesn't
  guarantee every domain fits on very short screens, so pagination only engages when none of 1/2/3
  columns fit everything on one page — it reserves room for the page-control bar only at that point
  (avoiding a circular measurement dependency between "do I need paging" and "how much space does
  the paging control take") and always renders the 3-column grid in that case, matching the same
  measured-height approach `EditCategoriesScreen`/`EditCategoryPhrasesScreen` already use
  (`onSizeChanged` + a `LaunchedEffect` that computes rows-that-fit). When one page is enough
  (`totalPages == 1`), no page-control row is rendered at all — unlike
  `EditCategoriesScreen`/`EditCategoryPhrasesScreen`, which always show theirs. That's a deliberate
  difference: those screens' content size is user-data-driven (could always grow past one page),
  while this screen's 5 domains are fixed, so showing "Page 1 of 1" would just be static clutter on
  every device that already fits them. Pagination state (`currentPage`/`itemsPerPage`/`totalPages`)
  was added to `ResetSettingsState` and `ResetSettingsViewModel` following the same
  `updateItemsPerPage`/`nextPage`/`prevPage` shape as `EditCategoriesViewModel`.

## Second follow-up pass: scope the Phrases domain to non-custom phrases, rename the screen

Direct feedback after the pass above caught that the Reset App Settings screen's standalone
"Phrases" checkbox was still calling `IPhrasesUseCase.resetToDefaults()` — the same method that
backs the "Reset Everything" nuclear option — which deletes every stored phrase outright, including
genuinely custom ones (phrases added to a preset category, and every phrase in a user-created
category). That's the wrong scope for a domain reset that's supposed to mean "put back what has a
default to go back to": a custom phrase has no default, so "resetting" it doesn't make sense -
the only sensible operation on one is delete, and this domain reset shouldn't be silently deleting
content the user authored.

### New `IPhrasesUseCase.resetPresetPhrasesToDefaults()`

Added specifically for this domain, distinct from both `resetToDefaults()` (global, still backs
"Reset Everything" — unchanged) and `resetPhrasesForCategory(categoryId)` (single category, still
deletes that category's custom phrases too — unchanged, since resetting one category the user
picked is a different, already-understood scope). The new method:

1. Reads every phraseId `PresetPhrasesRepository.getAllPresetPhrases()` knows about (deliberately
   including soft-deleted rows, so an individually-deleted preset's id is still recognized as
   preset-derived).
2. Deletes only the stored ("shadow") phrases whose phraseId is in that set — per the invariant
   documented in `RoomPresetPhrasesRepository.ensurePopulated()`, a stored row keyed by a preset's
   array-entry-name id is always a shadow, since genuinely custom phrases get a random UUID and can
   never collide with one. Everything else in the stored table (custom phrases added to a preset
   category, and every phrase in a user-created category) is left completely untouched.
3. Hard-deletes and repopulates every `PresetPhrase` row (`presetPhrasesRepository.deleteAllPhrases()`
   + `populateDatabase()`) — safe unconditionally, since preset-category rows only ever exist for
   actual `PresetCategories` entries; a custom category never has any to begin with.

Needed a new `StoredPhrasesRepository.getAllPhrases()` (there was no way to enumerate stored phrases
across every category before this — only per-category or per-id lookups existed). New tests in
`PhrasesUseCaseTest` (androidTest, against real Room) cover restoring an edited preset phrase,
restoring a deleted preset phrase, leaving a custom phrase added to a preset category alone, and
leaving a custom category's phrases alone entirely. `FakePhrasesUseCase` can't model the
preset-vs-custom distinction itself (it's a flat category→phrases map with no concept of which
phrases are preset-derived), so it just tracks which of the two reset methods was called
(`resetToDefaultsCalled`/`resetPresetPhrasesToDefaultsCalled`) — good enough for
`ResetSettingsViewModelTest` to confirm the PHRASES domain wires to the new, narrower method instead
of the nuclear one; the real preset/custom scoping behavior is what `PhrasesUseCaseTest` verifies
against actual data.

### `Reset App` → `Reset Vocable`

The Settings menu row and the Reset App Settings screen's own header both read the same
`settings_reset_app` string. Renamed from "Reset App" to "Reset Vocable" — reuses the app's own
existing phrasing (`settings_reset_dialog_message` already says "reset Vocable to default
settings") rather than introducing a second way of naming the same action, and reads less
ambiguously than a bare "Reset" sitting in a list of noun-phrase settings rows ("My Sayings",
"Voice", "Selection Mode").

### Reset Category button reordered above Remove Category

Also merged in `feature/681/typography-parity` (issue #681) so this branch picks up the
newly-established per-breakpoint font-size conventions, and moved `EditCategoryMenuScreen`'s
`Reset Category` button above `Remove Category` — the less-destructive action (restores defaults)
now reads before the fully-destructive one (deletes the category outright) — swapping their reveal
priority to match, so a cramped screen still reveals top-to-bottom in drawn order. The Reset
Category button itself picked up a hardcoded `fontSize` in the process (it was added after the font
branch diverged, so the earlier merge didn't touch it) — fixed to use the same
`edit_category_menu_action_text_size` dimen its sibling buttons already use.

## Verification

`./gradlew testDebugUnitTest` — full suite passes, including the updated/added tests above
(`EditCategoryMenuViewModelTest` reset cases, `ResetSettingsViewModelTest` pagination and
PHRASES-domain-wiring cases, `PhrasesUseCaseTest`'s new `resetPresetPhrasesToDefaults` cases) and the
trimmed-down `SettingsVoiceViewModelTest`/`SensitivityViewModelTest`/`SelectionModeViewModelTest`/
`EditCategoriesViewModelTest` (unit + androidTest) with their reset-icon cases removed.
`./gradlew compileDebugKotlin compileDebugUnitTestKotlin compileDebugAndroidTestKotlin` and
`./gradlew assembleDebug assembleDebugAndroidTest` all pass. Not done: manual verification in a
running app/emulator (none available in this session) and no design sign-off, same as the passes
above.
