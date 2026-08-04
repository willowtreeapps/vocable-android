# Change Voice: responsive 2-column grid matching iOS size classes

**Issue:** #644 (Part of #613 — the overall Voice Selection feature)

## What was needed

`VoiceSelectionScreen.kt` was single-column at every breakpoint, and it sized itself with two mechanisms nothing else in this repo uses:

- `itemsPerPage` started as a hardcoded `if (isLandscape) 3 else 5`, then got re-derived at runtime by a `BoxWithConstraints` + `LaunchedEffect(maxHeight, rowSpacing)` measurement pass against a fixed `rowHeight = 60.dp`.
- Padding, row spacing, close-button size, paging-button size and the row's internal spacing were all inline `if (isLandscape) X.dp else Y.dp` ternaries.

iOS's `VoicePickerViewController.updateLayoutForCurrentTraitCollection()` uses `fixedCount(2)` columns for every size class **except** `hCompact_vRegular` (phone portrait), which gets `fixedCount(1)`. And this repo's own convention — `CLAUDE.md`, `PresetsScreen.kt` — is that grid dimensions come from `integerResource`s per screen-size breakpoint dir, with fixed tile positions as a deliberate accessibility contract for gaze users rather than a layout preference.

## What changed

- **`voice_columns` / `voice_rows` integers** added to all six breakpoint dirs. `phrases_columns`/`phrases_rows` are defined in every dir even where the value equals its fallback, so these follow suit rather than relying on resource fallback.
- **Twelve `voice_*` dimens** added to the four dirs that have a `dimens.xml` (`values`, `values-land`, `values-sw600dp`, `values-sw600dp-land`; the `sw400dp` dirs correctly inherit `values`/`values-land`), replacing every hardcoded dp in the screen. Nine cover margins, spacing and button sizes; three cover icon sizes (see below).
- **The runtime measurement pass is gone** — no `BoxWithConstraints`, no `rowHeight` constant, no mutable `itemsPerPage`. `itemsPerPage` is now `voice_columns * voice_rows`, read from resources, so it recomputes on configuration change for free.
- **Grid renders as a fixed row×column nest**, the same shape as `PresetsScreen.kt`, with `Spacer(Modifier.weight(1f))` for absent items.
- **`VoiceOptionRow` lost its `isLandscape: Boolean` param** — its only two uses were spacing values, now `voice_row_content_spacing` and `voice_row_text_padding`.
- **Voice name auto-sizes to a single line** (`BasicText` + `TextAutoSize`, `maxLines = 1`) and the **checkmark's slot is reserved on every row**, so names never clip, never orphan their trailing index, and fit identically whether or not the row is selected.
- **Page reset re-keyed** from `LaunchedEffect(isLandscape)` to `LaunchedEffect(itemsPerPage)`. The #618 out-of-range page clamp is unchanged.
- **`VoiceGridResourcesTest`** added — 4 tests pinning the resource matrix.
- **Previews** now cover all four grid shapes via `@Preview(device = "spec:…")` and use 7 voices so every breakpoint's last page is a partial one.

## Final row/column matrix (device-verified)

| dir | iOS size class | cols × rows | measured tile height |
|---|---|---|---|
| `values` | `hCompact_vRegular` | 1 × 5 | 121.5dp |
| `values-sw400dp` | `hCompact_vRegular` | 1 × 5 | 129.9dp |
| `values-land` | `hCompact_vCompact` | 2 × 3 | 80.0dp |
| `values-sw400dp-land` | `hCompact_vCompact` | 2 × 3 | 86.5dp |
| `values-sw600dp` | `hRegular_vRegular` | 2 × **7** | 133.0dp |
| `values-sw600dp-land` | `hRegular_vCompact` | 2 × 4 | 127.0dp |

## Key decisions, and why

### Rows fill their slot; the play chip is capped

iOS uses `numberOfRows = .flexible(minHeight:)` — 100pt for `hRegular_vRegular`, 64pt elsewhere — so its rows grow with the available height. The Android equivalent that also satisfies the ticket's "no leftover dead space" criterion is `weight(1f)` per row, which is what `PresetsScreen` already does.

The catch is `VoiceOptionRow`'s play chip, which is square (`aspectRatio(1f)`). Left to follow the row height it would have rendered as an 80–244dp square. It's capped with `heightIn(max = voice_play_chip_max_size)` placed **outside** `fillMaxHeight()`, so the cap applies to the incoming constraint before the fill resolves, and the row's `Alignment.CenterVertically` centers the capped chip against a taller name tile.

### Tablet portrait is 7 rows, not the 4 the ticket suggested

The ticket offered 4 rows for `sw600dp` as a starting point "to validate, not a requirement". On device it measured **244dp per tile** — roughly double every other breakpoint, and visually a near-empty box with one line of text floating in it (screenshot taken during verification). Because rows stretch, row count is effectively the tile-height control.

Solving `(1024 - 16 × (n-1)) / n` for the 1024dp of grid height available at `sw600dp` portrait gives 244dp at n=4, 133dp at n=7, 114dp at n=8. **7 was chosen** so tile height stays in the same 80–133dp band as the other five breakpoints — consistent tile size across configurations matters more here than matching the ticket's placeholder, since predictable target size is the whole point of the fixed-position contract. It also keeps closer to iOS's 100pt `minHeight` for this size class than 244dp does.

Worth a second opinion from design, since it changes voices-per-page on tablet portrait from 8 to 14. Flagged on the PR.

### `voice_rows` differs between tablet portrait and tablet landscape

7 vs 4 for the same `sw600dp` device. Both come out at ~130dp per tile because portrait has 1024dp of grid height and landscape has 544dp. The asymmetry is the point: the row count is what holds tile height roughly constant across orientations.

### Names are auto-sized to a single line, and the checkmark's slot is always reserved

A two-column tile is only ~269dp wide on tablet portrait, leaving ~213dp for the name. Two problems showed up in review of the first build:

1. The name wrapped to two lines with no fitting logic, so a longer locale name in another language would have clipped.
2. The selected row's trailing checkmark consumed ~40dp of the text's width, so **the selected tile wrapped at a different point than its neighbours** — visible raggedness on an otherwise uniform grid.

Fixed by switching the name to `BasicText` + `TextAutoSize.StepBased(12sp..16sp)` with `maxLines = 1` and ellipsis overflow — the same treatment `PresetsScreen` gives its phrase tiles — and by wrapping the checkmark in a fixed-size `Box` that is present whether or not the row is selected, so the fit no longer depends on selection.

**`maxLines = 1` is deliberate.** Names now arrive from `buildVoiceDisplayNames()` as `"English (United States) Voice 1"`, `"… Voice 2"` and so on, where the trailing index is the *only* thing distinguishing one row from the next. Allowing two lines orphaned that digit alone on line two — so the text shrinks instead. At the 12sp floor the string needs ~190dp, comfortably inside the tightest breakpoint's 213dp; verified un-truncated at all six at default font scale.

#### Font scale: `MiddleEllipsis`, not `Ellipsis`

Raised in review by Michael Gonzalez — *"the UI will most likely wrap when the User increases font size; you just want to ensure that it is readable regardless of text length and font size."* That turned out to be a real defect, not a caveat.

`TextAutoSize`'s bounds are `sp`, so they scale with the user's font-size setting: at `font_scale 2.0` the 12sp floor becomes 24sp effective, the string needs ~380dp against 213dp, and no step can fit it. With plain trailing `TextOverflow.Ellipsis` every row rendered as **`"English (United S…"` — nine identical, unusable labels**, with the distinguishing index truncated away. For a picker whose whole job is telling voices apart, that's an accessibility failure at exactly the setting an accessibility-minded user is most likely to change.

Fixed with `TextOverflow.MiddleEllipsis` (available since Compose 1.8; this project is on ui-text 1.10.5). It eats the *redundant* half — the locale prefix, identical on every row in this picker since the list is already filtered to the current language — and preserves the tail: `"English…Voice 1"` … `"English…Voice 9"` at 2x. Verified on device at `font_scale` 1.0 and 2.0 across all six breakpoints, and visually at the tightest (`values-sw600dp` portrait, 213dp).

**Testing note:** `uiautomator dump` is useless for detecting this. Its `text` attribute reports the source string, so an ellipsized label still dumps in full — the automated check reported `ellipsized=0/9` while the screen was visibly truncated. Truncation has to be confirmed from a screenshot.

### Icons are sized per breakpoint, not left at intrinsic size

Raised in review by Mansimran Singh — *"you can update the icon size and gaze button size based on landscape and portrait."* The button sizes already were (`voice_close_button_size`, `voice_paging_button_size`, `voice_play_chip_max_size`), but the icons inside them were not: every drawable was rendering at its intrinsic size (`ic_close` 48dp, `ic_play_circle_40dp`/`ic_stop_circle_40dp`/the pager arrows 40dp). That reads as overfull in the tightest bucket — a 48dp icon in `values-land`'s 48dp close button — and undersized in the roomiest, a 40dp arrow in `sw600dp`'s 80dp pager button.

Added `voice_close_icon_size`, `voice_play_icon_size` and `voice_paging_icon_size` per dimens dir, scaled to their buttons. The pager arrows required an `iconSize` parameter on the shared `VocablePagination`, defaulted to 40dp so the component's other (future) callers are unaffected — consistent with how `buttonSize` is already passed in rather than resolved inside.

One detail worth knowing: **`BasicText` does not read `LocalContentColor`, unlike `Text`.** `VocableButton` signals its dwell-press state by flipping the Material `contentColor` to `ColorPrimaryDark`, so a hardcoded color here would have silently killed the press feedback on the name — the primary confirmation a gaze user gets that a dwell registered. The style therefore pulls `LocalContentColor.current` in explicitly. (`PresetsScreen`'s `BasicText` calls hardcode `TextColor` and so appear to have already lost this on phrase tiles; not changed here, but worth a look.)

The reserved slot is 24dp, not the vector's 40dp intrinsic size, because every dp reserved is a dp off the name's width. An earlier revision of this change reserved the full 40dp and so *narrowed* the text on the eight unselected tiles (237dp → 197dp) in order to equalise the wrap — a bad trade that was caught on device and reduced to 24dp (213dp of text).

#### Why this needed the upstream name change first

Worth recording, because the same wall will come back if names ever lengthen again.

Against the *previous* name format — `"English (United States) – High Quality"`, 37 characters, produced by the old `buildVoiceDisplayName()` — one line was genuinely unreachable. It needed ~303dp against 213dp available, so fitting would have meant ~11sp, worse for this audience than wrapping. The row's overhead is 80dp play chip + 32dp tile padding + 24dp checkmark + 12dp spacing = 148dp of the 361dp cell, and none of it is cheap to reclaim:

- **Fold the play chip inside the name tile**, matching iOS's single-cell structure (`VocableListContentConfiguration(title:, actions: [sampleAction], trailingAccessory: .checkmark)` — iOS really does put the preview control *inside* the cell, so Android's separate chip column is a genuine divergence). This is **width-neutral**: the chip's 80dp moves inside rather than disappearing, so `32 + 80 + 12 + 213 + 24 = 361`, the same 213dp of text. Still worth doing on structural-parity grounds, but as its own ticket — it does nothing for text width.
- Even stripping to a 48dp chip, 12dp padding and no reserved checkmark only reaches ~273dp, and 48dp is the accessible floor for a gaze target.

What actually unblocked it was **#643 landing `buildVoiceDisplayNames()`** on the integration branch, which replaced the quality suffix with a per-locale index: `"English (United States) Voice 1"`, 31 characters, ~254dp at 16sp. That fits 213dp at ~13.5sp, inside the auto-size floor. The two changes are only jointly sufficient — this ticket rebased onto #643 before the single-line result was reachable, and the earlier revision of this branch shipped a two-line wrap because it predated that merge.

Corollary: the numbered format is now load-bearing for layout, not just for telling voices apart. If the copy changes again, re-check the fit at `values-sw600dp` portrait first — it's the tightest of the six at 213dp.

### Row counts deliberately do NOT match iOS's, pending the name-copy decision

iOS does not hand-pick row counts at all. `CarouselGridLayout.computeRowCount()` derives them for the `.flexible` case: `n = floor(height / minHeight)`, then decrement while `(height − (n−1) × spacing) / n < minHeight`, where `VoicePickerViewController` supplies `minHeight` 100pt for `hRegular_vRegular` and 64pt for the other size classes. Applied to the grid heights measured here, that yields **9 / 3 / 8 / 7** rows (phone portrait / phone landscape / tablet portrait / tablet landscape) at 67–114dp per row, against this PR's 5 / 3 / 7 / 4 at 80–133dp. Only phone landscape happens to agree.

**Parity was not adopted, on purpose.** iOS can afford 64pt rows because its labels are short proper names ("Karen", "Daniel") — `AVSpeechSynthesisVoice.name`. Android TTS has no equivalent field, so our rows carry a 37-character two-line string plus a square play chip; packing them into a 67dp row would have forced the text down toward 12sp on phone portrait and tablet landscape. In an AAC app for motor- and speech-impaired users, legibility of the target beats matching a row count.

So the density gap is a **downstream consequence of the name copy, not an independent layout choice**. The team is currently discussing changing voice names; shortening them (e.g. dropping the locale prefix, which is identical on every row in the picker since the list is already filtered to the current language) would make iOS's derived counts viable. **When that copy decision lands, revisit this matrix** — the numbers live entirely in `voice_rows` across the six `integers.xml` files plus the expected map in `VoiceGridResourcesTest`, so it's a resource-only change.

### Empty trailing slots on a partial page match iOS

Worth recording because it looks like a bug and isn't. With 9 installed voices and 14 slots on tablet portrait, two rows render empty at the bottom. iOS behaves identically: in `frameForCell`, the `.flexible` branch sizes the block from `numberOfOccupiedRows` (derived from the items actually on that page, not `itemsPerPage`) and positions it via `alignment`, which defaults to `.top` and is never overridden by `VoicePickerViewController`. So a partial page anchors to the top and leaves its gap at the bottom on both platforms.

## Verification

`./gradlew testDebug` — 79 tests green, including the 4 new ones. `./gradlew assembleDebug` clean.

**The resource test was mutation-checked**, not just observed passing: flipping `values-sw600dp`'s `voice_columns` to 1 failed two of its assertions as intended.

All six breakpoints were verified on `emulator-5554` by overriding `wm size` / `wm density` / `user_rotation` and measuring the real view hierarchy via `uiautomator dump`, rather than trusting previews:

- **Column counts** match the table above — 1 column for exactly the two phone-portrait buckets, 2 for the other four.
- **No clipping, no dead band**: at every breakpoint the pagination row's bottom edge landed exactly at `screenHeight − voice_screen_margin`, and the grid consumed the full height between the header and the pager. Row gaps measured equal to `voice_row_spacing` in each dir.
- **Fixed positions hold on a partial page**: with 9 voices installed and 5 per page, page 2's four tiles sat at y = 112.0 / 254.1 / 396.2 / 538.3dp — byte-identical to page 1's first four origins, with the fifth slot left blank instead of the content reflowing or centering.
- **Rotating while on page 2** landed on a valid "Page 1 of 2" rather than a blank page, exercising the `itemsPerPage`-keyed reset alongside #618's clamp.

Unlike #636's verification gap, this emulator does report installed voices — 9 of them, labelled `English (United States) Voice 1` through `Voice 9` once #643's naming landed — so the grid was exercised with real data. **Names render on a single un-ellipsized line at all six breakpoints**, re-verified after the rebase onto #643.

### Note on the verification method

`adb shell settings put system user_rotation` is unreliable — it silently no-ops often enough that a first pass produced two configs that reported portrait labels while still rendering landscape. Orientation was subsequently asserted from the root node's own bounds in the `uiautomator` dump instead of from `wm size` (which only echoes the override, not the current rotation). Anyone repeating this sweep should verify orientation from the dump rather than assuming the `settings put` took effect.

## Pre-existing issues, not fixed here

- **`horizontalPageSwipe(onSwipeLeft = goToPreviousPage, onSwipeRight = goToNextPage)`** reads inverted — a leftward swipe conventionally advances a pager. Untouched by this PR since it's orthogonal to layout; raised on #613.
- **The landscape pagination row still sits under the system nav bar** — the global inset issue documented in #636's log (`MainActivity.kt` applies horizontal-only insets). Measured again here: the pager's bottom edge is correct relative to the app's own bounds, so this is purely the inset gap, not a grid regression.
- **The screen title is still ~36dp right of center**, also from #636's log — the header row is `[close][spacer][title][spacer]`, so the title centers in the leftover width.

## `CLAUDE.md` correction

The Testing section cited `app/src/test/java/com/willowtree/vocable/presets/{KeypadPhraseOrderTest,ResourceXml}.kt` as pinning the keypad grid shape. **Neither file exists** anywhere in the repo — confirmed by searching both `src/test` and `src/androidTest`. The bullet was rewritten to say the keypad grid shape is currently unpinned, and to point at `VoiceGridResourcesTest` as the working example of asserting a per-breakpoint resource matrix from a JVM test (there's no Robolectric here, so raw XML parsing is the only route).

This matters beyond tidiness: the old text implied `values-*` grid changes were already regression-guarded, which would have made it reasonable to skip adding a test on a ticket like this one.

## Pointers

- Issue: #644 · Parent: #613 · Siblings: #636 (visual rework), #643 (sample-phrase preview), #618 (hide undownloaded voices)
- iOS reference: `Vocable/Features/Settings/VoiceSettings/VoicePickerViewController.swift` — `updateLayoutForCurrentTraitCollection()`
- Figma divergence (tablet landscape shown as one column, node `6537:3331`) was already flagged to design on #622; iOS wins per the parent ticket, so this PR ships two columns there.
