# Typography parity with iOS + consolidate hardcoded text sizes

**Issue:** #681 ("Match Android typography to iOS font-size scale, consolidate hardcoded text
sizes").

## What was needed

vocable-ios uses `UIFont.systemFont` (San Francisco, the OS default) everywhere, sized in a
compact/regular scale (22/24/28/40/48pt, almost always bold). Android's `Type.kt` already uses
`FontFamily.Default` (Roboto, the OS default) — the font family already matched, no change needed
there. But a few Android per-breakpoint sizes drifted from their iOS compact/regular equivalents
more than unit/density rounding explains, and a number of hardcoded `.sp` literals bypassed this
app's own `dimens.xml` per-breakpoint convention entirely, so those spots didn't scale on tablet
the way the rest of the app does.

## What changed

### An accidental Compose-rewrite regression, found via git blame — not just an iOS mismatch

Several screens used `MaterialTheme.typography.headlineLarge`/`headlineMedium` with no `fontSize`
override, silently falling back to Material3's flat, non-scaling defaults (32sp/28sp) instead of a
deliberate per-breakpoint size. `git log -S "keyboard_text_size"` showed this dimen (and its
sibling `keyboard_input_text_size`) dates to the pre-Compose View-based keyboard layouts
(`e9a6f96d`, `bcf61a58`), while the Compose rewrite (`f5b06b93`, "Refactor with jetpack compose
commit#1") introduced the keyboard key `Text(...)` fresh and never reconnected it to that dimen —
it sat in `dimens.xml`, fully unused, ever since. Fixed by wiring the already-designed (just
orphaned) dimens back in, rather than inventing new values:

- **Keyboard letter keys** (`KeyboardScreen.kt`) — the actual key glyphs, now use
  `keyboard_text_size` (48sp phone/land, 34sp tablet portrait, 40sp tablet landscape — all
  pre-existing breakpoint values, untouched).
- **Keyboard typed-input display** (`KeyboardScreen.kt`, both the head-tracking `Text` and the
  touch-fallback `TextField` + placeholder, in both the landscape/tablet and portrait layout
  branches — 6 spots total) — now use `keyboard_input_text_size` (34sp across all breakpoints).

Two similarly-orphaned dimens, found the same way, revived for their evident original targets:

- **`EditCategoryMenuScreen`'s category title** — now uses `settings_edit_individual_category_text_size`
  (30sp phone; added a 42sp tablet override, since none existed — sized in proportion to
  `edit_categories_title_text_size`'s 34→48sp ratio, the sibling list-screen's title).
- **`SensitivityScreen`'s "Hover Time"/"Cursor Sensitivity" section labels** — now use
  `timing_subtitle_text_size` (24sp phone, 34sp tablet). Its **dwell-time value display** ("1
  second", "2.5 seconds") now uses `hover_time_text_size` (24sp phone; added a 34sp tablet override
  to match `timing_subtitle_text_size`'s ratio, since none existed). The screen's own main title
  ("Timing and Sensitivity") was left alone — it already uses `BasicText`/`TextAutoSize.StepBased`
  for responsive sizing, a different (and already-adequate) strategy than the breakpoint-dimens
  approach used everywhere else.
- **`VoiceSelectionScreen`'s "Change Voice" header** — this is the screen's own title, playing the
  same role as every other Settings-adjacent screen's title, so it now reuses
  `settings_title_text_size` directly rather than getting a new near-duplicate `voice_*` dimen.

### `settings_title_text_size` phone value corrected to match iOS

iOS's `VocableNavigationBar` title is 28pt compact / 48pt regular. Android's tablet value already
matched exactly (48sp); only the phone value was off (34sp) beyond what unit/density differences
explain. Changed to 28sp. This dimen backs most Settings-adjacent screen titles (`SettingsScreen`,
`SettingsVoiceScreen`, `SelectionModeScreen`, `ResetSettingsScreen`, and now `VoiceSelectionScreen`
above), so the fix applies consistently everywhere in one change.

### Hardcoded `.sp` sweep

A broad literal grep (`[0-9]+\.sp`) initially suggested ~50 hardcoded sizes, but nearly all of them
turned out to be `minFontSize`/`maxFontSize`/`stepSize` values inside already-responsive
`TextAutoSize.StepBased(...)` configs, or `letterSpacing`/`lineHeight` (spacing, not size), or the
base `Typography` scale definition in `Type.kt` itself (not a screen bypassing anything — it's the
canonical place that scale lives, and per-screen dimens already layer on top of it, as this whole
pass confirmed). None of those represent an actual "doesn't scale on tablet" bug: auto-sizing
already adapts to available space at runtime regardless of breakpoint. Filtering those out left
exactly 6 genuine fixed, non-scaling hardcoded sizes, all coincidentally `18.sp`:

- `EditCategoryMenuScreen`'s four action-button labels (Rename/Toggle/Edit Phrases/Remove) — new
  `edit_category_menu_action_text_size` (18sp phone, 24sp tablet).
- `EditCategoryPhrasesScreen`'s phrase-row text — new `edit_category_phrases_item_text_size` (18sp
  phone, 24sp tablet).
- `FaceTrackingScreen`'s "Head tracking is paused" message — new `head_tracking_paused_text_size`
  (18sp phone, 24sp tablet).

Three `PresetsScreen.kt` instances of `fontSize = 20.sp` were investigated and deliberately left
alone: each sits inside a `TextStyle` immediately followed by
`TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 20.sp, ...)` — the base `fontSize`
exactly matches `maxFontSize`, meaning auto-sizing already governs the actual rendered size at
runtime regardless of screen breakpoint. Moving these to a dimen would be cosmetic churn with zero
behavior change.

## Key decisions, and why

### No font-family change on either platform

iOS uses `.systemFont` (San Francisco) everywhere; Android's `Type.kt` already uses
`FontFamily.Default` (Roboto). Both are already using their OS's default font, which is correct —
bundling one platform's typeface onto the other would look foreign to users of that platform, and
neither app has ever shipped a custom font asset. Out of scope by design, not an oversight.

### `timing_title_text_size` restored after initially removing it

Confirmed via code search that this dimen (and `SensitivityScreen`'s main title in general) wasn't
used by any Compose screen — the title uses `BasicText`/`TextAutoSize` instead. Removed it as dead
weight, which broke `processDebugResources`: `styles.xml`'s `TimingSensitivityTitle` style (a
leftover from the pre-Compose, XML-View era — this app is 100% Compose today, no XML layouts) still
references it via `autoSizeMaxTextSize`. That style is itself unreferenced by any layout or Kotlin
code, but Android's resource linker doesn't care whether a *referencing* resource is itself dead —
it only cares that the reference resolves. Restored the dimen rather than also hunting down and
deleting the legacy XML style tree, which is a separate, broader "dead pre-Compose resource
cleanup" concern outside this ticket's scope.

### Screen-scoped dimen names over one shared generic one

For the three genuine `18.sp` hardcodes (three unrelated screens, coincidentally the same value),
named each dimen after its own screen/role (`edit_category_menu_action_text_size`,
`edit_category_phrases_item_text_size`, `head_tracking_paused_text_size`) rather than introducing
one shared `action_text_size`-style constant. Matches this codebase's existing naming convention
(`edit_categories_title_text_size`, `settings_button_text_size`, etc., all screen-prefixed) and
keeps each screen independently tunable later without an unrelated screen's needs colliding.

### Added tablet overrides even where sibling dimens don't have one

`settings_button_text_size` and `edit_categories_list_text_size` (pre-existing, untouched) have no
`values-sw600dp` override either — the codebase's existing tablet-scaling coverage is itself
incomplete. Chose to add tablet overrides for every dimen touched or created in this pass anyway:
the point of this ticket is closing tablet-scaling gaps, so shipping a new dimen that reproduces the
same gap (mechanically compliant with "use dimens.xml," but not actually fixing the user-facing
issue) would defeat the purpose. Pre-existing dimens with the same gap were left alone — fixing
every instance of that separate, smaller issue across the whole app is out of scope here.

## Verification

`./gradlew compileDebugKotlin testDebugUnitTest` and
`./gradlew assembleDebug assembleDebugAndroidTest` all pass. Not done: manual verification in a
running app/emulator (none available in this session) and no design sign-off.

## Pointers

- Issue: #681, branch `feature/681/typography-parity` (linked via `createLinkedBranch`, branched
  from `main`).
- iOS reference: `willowtreeapps/vocable-ios`, `Vocable/Extensions/UIFont+Helpers.swift`,
  `Vocable/Common/VocableNavigationBar.swift`, `Vocable/Common/PresetItemCollectionViewCell.swift`,
  `Vocable/Common/Views/EmptyStateView.swift`.
