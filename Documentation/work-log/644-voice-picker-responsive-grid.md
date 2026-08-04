# Change Voice: responsive 2-column grid matching iOS size classes

**Issue:** #644 (Part of #613 — the overall Voice Selection feature)

## What was needed

`VoiceSelectionScreen.kt` was single-column at every breakpoint, and it sized itself with two mechanisms nothing else in this repo uses:

- `itemsPerPage` started as a hardcoded `if (isLandscape) 3 else 5`, then got re-derived at runtime by a `BoxWithConstraints` + `LaunchedEffect(maxHeight, rowSpacing)` measurement pass against a fixed `rowHeight = 60.dp`.
- Padding, row spacing, close-button size, paging-button size and the row's internal spacing were all inline `if (isLandscape) X.dp else Y.dp` ternaries.

iOS's `VoicePickerViewController.updateLayoutForCurrentTraitCollection()` uses `fixedCount(2)` columns for every size class **except** `hCompact_vRegular` (phone portrait), which gets `fixedCount(1)`. And this repo's own convention — `CLAUDE.md`, `PresetsScreen.kt` — is that grid dimensions come from `integerResource`s per screen-size breakpoint dir, with fixed tile positions as a deliberate accessibility contract for gaze users rather than a layout preference.

## What changed

- **`voice_columns` / `voice_rows` integers** added to all six breakpoint dirs. `phrases_columns`/`phrases_rows` are defined in every dir even where the value equals its fallback, so these follow suit rather than relying on resource fallback.
- **Nine `voice_*` dimens** added to the four dirs that have a `dimens.xml` (`values`, `values-land`, `values-sw600dp`, `values-sw600dp-land`; the `sw400dp` dirs correctly inherit `values`/`values-land`), replacing every hardcoded dp in the screen.
- **The runtime measurement pass is gone** — no `BoxWithConstraints`, no `rowHeight` constant, no mutable `itemsPerPage`. `itemsPerPage` is now `voice_columns * voice_rows`, read from resources, so it recomputes on configuration change for free.
- **Grid renders as a fixed row×column nest**, the same shape as `PresetsScreen.kt`, with `Spacer(Modifier.weight(1f))` for absent items.
- **`VoiceOptionRow` lost its `isLandscape: Boolean` param** — its only two uses were spacing values, now `voice_row_content_spacing` and `voice_row_text_padding`.
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

## Verification

`./gradlew testDebug` — 79 tests green, including the 4 new ones. `./gradlew assembleDebug` clean.

**The resource test was mutation-checked**, not just observed passing: flipping `values-sw600dp`'s `voice_columns` to 1 failed two of its assertions as intended.

All six breakpoints were verified on `emulator-5554` by overriding `wm size` / `wm density` / `user_rotation` and measuring the real view hierarchy via `uiautomator dump`, rather than trusting previews:

- **Column counts** match the table above — 1 column for exactly the two phone-portrait buckets, 2 for the other four.
- **No clipping, no dead band**: at every breakpoint the pagination row's bottom edge landed exactly at `screenHeight − voice_screen_margin`, and the grid consumed the full height between the header and the pager. Row gaps measured equal to `voice_row_spacing` in each dir.
- **Fixed positions hold on a partial page**: with 9 voices installed and 5 per page, page 2's four tiles sat at y = 112.0 / 254.1 / 396.2 / 538.3dp — byte-identical to page 1's first four origins, with the fifth slot left blank instead of the content reflowing or centering.
- **Rotating while on page 2** landed on a valid "Page 1 of 2" rather than a blank page, exercising the `itemsPerPage`-keyed reset alongside #618's clamp.

Unlike #636's verification gap, this emulator does report installed voices (9 of them, all `English (United States) – High Quality`), so the grid could be exercised with real data. It still can't distinguish per-voice names, but that doesn't affect layout.

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
