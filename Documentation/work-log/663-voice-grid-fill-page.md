# Change Voice: fill the page height instead of leaving a dead band

**Issue:** #663 (Part of #613 — the overall Voice Selection feature; fixes a regression from #644 / PR #662)

## What was needed

#644 made the Change Voice grid responsive per breakpoint, and shipped with a visible bug: a **full**
page of voices occupied only part of the grid area, leaving a wide empty band above the pager. On
phone portrait a page showed 5 rows and 287dp of nothing under them.

The cause is a mismatch between two things #644 changed at different times. Commit `9d1eecb7` laid
the grid out as `voice_rows` × `voice_columns` slots whose rows *stretched* to divide the available
height, and picked row counts (5 / 3 / 7 / 4) to suit that. Commit `a3c42628` then switched rows to a
fixed `voice_row_height` matched to the square play chip — design preferred the compact row over
tiles that stretched to 244dp — but the row counts were never re-derived. Fixed-height rows packed
from the top of a `weight(1f)` column, so whatever the count didn't use was left at the bottom.

Measured before the fix:

| config | grid height | full page | dead space |
|---|---|---|---|
| phone portrait 393x851dp | 635dp | 5 × 72dp = 348dp | 287dp |
| phone landscape | 257dp | 3 × 56dp = 152dp | 105dp |
| tablet portrait 800x1280dp | 1024dp | 7 × 96dp = 656dp | 368dp |
| tablet landscape 1280x800dp | 544dp | 4 × 76dp = 268dp | 276dp |

## What changed

- **`voice_rows` is gone from all six `integers.xml` dirs.** The row count is now derived at layout
  time. `voice_columns` still comes from resources, unchanged (1 / 2 per the iOS size-class matrix).
- **`voice_row_height` → `voice_row_min_height`** in the four `dimens.xml` dirs, same values
  (60 / 48 / 80 / 64dp). It is now a target/minimum rather than an exact height, and it is what the
  row count is derived against — so it now also decides how many voices a page holds.
- **`VoiceSelectionScreen` wraps its content in `BoxWithConstraints`** and computes
  `voiceRowCount(gridHeight, rowMinHeight, rowSpacing)` — the largest n whose n rows plus (n−1) gaps
  still leave every row at least the minimum, floored at 1.
- **Grid rows fill by weight** (`Modifier.weight(1f)`) instead of taking a fixed height, so a full
  page always consumes the grid area exactly.
- **`voiceRowCount` is `internal`, not inlined**, so the arithmetic is unit-testable without
  Robolectric (there is none in this repo).
- **Tests:** `VoiceGridResourcesTest` reworked — columns and `voice_row_min_height` matrices, plus an
  assertion that no dir defines `voice_rows` (see below). New `VoiceRowCountTest` covers the derived
  count. `CLAUDE.md`'s grid-dimensions bullet gained a sub-bullet recording the exception.

## Key decisions, and why

### Why the row count is derived rather than retuned per breakpoint

The obvious minimal fix is to bump the `voice_rows` integers (roughly 8 / 4 / 10 / 7 instead of
5 / 3 / 7 / 4) and leave everything else alone. It was rejected because **`sw###dp` qualifiers
constrain width and never height**, so one count per width bucket cannot fit every device in it:

- `values` (sw < 400dp portrait) contains both a 393×851dp phone (grid 635dp, 8 rows) and a
  360×640dp one (grid 424dp, 6 rows). Tuning for the first clips two rows off the second, and a
  clipped row is a lost gaze target — worse than the dead space being fixed.
- Tuning for the shorter device puts the dead band straight back on the taller one.

Android does have `-h###dp` (available height) qualifiers, which would let the count stay declarative
at the cost of multiplying the resource dirs. That was weighed and dropped: the count would still be
a step function over device height, so a device between two buckets keeps a partial band, and the
matrix to maintain grows from 6 dirs to 6 × however many height bands.

Deriving it gets the property outright — no dead space and no clipping at any height, including
multi-window and foldables, which no static matrix covers.

### The measurement pass #644 deleted is *not* what came back

Worth being precise, because #662's review specifically liked seeing the old measurement pass go.

What #644 removed was a `BoxWithConstraints` + `LaunchedEffect(maxHeight, rowSpacing)` pair that
wrote a **mutable `itemsPerPage` state** — a two-pass layout that recomposed with a different page
capacity a frame after first draw. What this change adds is a `BoxWithConstraints` whose `maxHeight`
feeds a **pure function during composition**. There is no state, no `LaunchedEffect`, no second pass
and no first-frame flash. `voice_columns` is still a resource, and page capacity is still constant
for a given configuration, so the fixed-slot contract is untouched.

### Rows fill by weight *and* the count comes from a minimum height — both, not either

The two halves are what make the result safe, and either alone is worse:

- **Weight fill alone** (keep the counts, stretch the rows) is what #644 started with. It removes the
  dead space but makes row height a function of the device: 5 rows in 635dp gives 127dp tiles, and on
  tablet portrait it gave the 244dp tiles design rejected.
- **A derived count alone** (keep fixed-height rows, just pick the right number of them) still leaves
  up to one row-pitch of dead space, since the fit is only exact when the height divides evenly.

Together, the rendered row height is bounded: **never below `voice_row_min_height`** (the count is
the largest that fits at that height) and **at most one row-pitch above it** spread across the rows
(otherwise another row would have fit). Design's chip-matched height is the floor, not an average.
The gaze-target-shrinking direction — the one that matters for this audience — is ruled out entirely.

That bound is why the chrome subtraction (`2 × margin + close button + 2 × section spacing + pager
button`) is allowed to be an approximation. At large font scales the header title or pager label can
outgrow the buttons they sit next to, so the real grid is a little shorter than the estimate; because
the rows then divide the *actual* measured height by weight, the cost is a few dp of row height, not
a clipped row. Verified at `font_scale 2.0`.

### Consequence: fewer pages, and it moves with the row height

Page capacity roughly doubled — phone portrait went from 5 voices per page to 8. With the 30 voices
on a real device that is 4 pages instead of 6, which is 2 fewer paging dwells to reach the last voice.

The knob is now `voice_row_min_height`: raising it makes rows taller and pages shorter, lowering it
does the reverse. If design ever wants iOS-like fatter rows (iOS derives its own counts from a
`minHeight` of 100pt on regular-width and 64pt elsewhere — the same idea), that is a one-dimen change
per breakpoint, and `VoiceRowCountTest`'s reference cases will need their expected counts updated.

### `voice_rows` coming back is a test failure, deliberately

`VoiceGridResourcesTest` asserts no values dir defines a `voice_rows` integer. That reads like an odd
thing to assert until you notice the repo convention (`CLAUDE.md`, `PresetsScreen`) is that grid
dimensions live in `integerResource`s per breakpoint — so reintroducing the integer is exactly the
"tidy this up to match the convention" move a future session would make, and it is the bug. The test
message says why. `CLAUDE.md`'s grid bullet now records the exception too.

The dir-scan test also grew to check `dimens.xml`, not just `integers.xml`: with the count derived
from `voice_row_min_height`, a stray override of that dimen in a locale-qualified dir (e.g.
`values-de`, which exists to widen the keyboard) would silently change how many voices a page holds.

## Verification

`./gradlew testDebug` — 87 tests green (79 before; the 4 old resource tests became 5, plus 4 new
row-count tests). `./gradlew assembleDebug` clean.

**Both new test groups were mutation-checked**, not just observed passing:

- Dropping the `+ rowSpacing` term from `voiceRowCount` (a plausible off-by-one) failed
  `no page has room for another row` and `reference devices fill their page`.
- Re-adding `<integer name="voice_rows">5</integer>` to `values/integers.xml` failed
  `row height is a per-breakpoint minimum, not a row count`.

Verified on `emulator-5554` (9 installed en-US voices) by measuring the real view hierarchy with
`uiautomator dump` at each of the four `dimens.xml` buckets, and confirming the grid's bottom edge
plus one `voice_section_spacing` lands on the pager's top edge — i.e. the rows consume the whole grid
area, with nothing left over and nothing pushed off:

| config (app window) | grid | rendered row height | grid bottom + spacing | pager top |
|---|---|---|---|---|
| phone portrait 393×851dp | 1 × 8 | 69dp | 2101px | 2098px |
| phone landscape 532×393dp | 2 × 4 | 58dp | 924px | 926px |
| tablet portrait 800×1280dp | 2 × 10 | 88dp | 2352px | 2352px |
| tablet landscape 1280×800dp | 2 × 7 | 67.5dp | 1393px | 1392px |

(The few-px residuals are dp→px rounding of the measured icon centres, not layout gaps.)

Also confirmed:

- **Fixed slot positions still hold on a partial page.** With 9 voices and 8 per page, page 2's lone
  tile sat at exactly page 1's first-row bounds — `[106,348][216,458]` for the chip, `[333,370]
  [904,436]` for the name, byte-identical.
- **`font_scale 2.0`** — count unchanged, nothing clipped, pager still fully on screen. Names still
  truncate at 2x; that is #644's documented copy-length limitation, unchanged here (rows are the same
  height at that breakpoint as before this fix).
- **The two `sw400dp` dirs** inherit their dimens from `values` / `values-land` and differ only in
  column count, which `VoiceGridResourcesTest` pins; `VoiceRowCountTest` covers the 915dp-tall
  `sw400dp` case in its reference set.

### Note on the verification method

`adb shell settings put system user_rotation`, `wm set-user-rotation` and `cmd window
set-user-rotation` were all no-ops on this emulator — #644's log flagged the first as unreliable; the
other two do not exist on this image at all. Landscape was reached instead by passing a
*portrait-shaped* `wm size` (the emulator's display is held at a 90° rotation, so the app window
comes out as the swap of whatever is passed) and asserting the orientation from the dump's own root
bounds. Anyone repeating the sweep should check root bounds rather than trusting the rotation call.

## Pre-existing issues, not fixed here

Unchanged from #644's log: the inverted-reading `horizontalPageSwipe` argument names, the landscape
pager sitting under the system nav bar (global inset issue, `MainActivity` applies horizontal-only
insets), the title sitting right of centre, and voice names truncating at large font scales.

## Pointers

- Issue: #663 · Parent: #613 · Regressed by: #644 (PR #662) · Siblings: #636, #643, #618
- Previous log: `Documentation/work-log/644-voice-picker-responsive-grid.md` — its "Final row/column
  matrix" table and its "Rows are a fixed height matching the play chip" decision are superseded by
  this one; the rest still stands.
- iOS reference: `CarouselGridLayout.computeRowCount()` derives row counts from a `minHeight` the
  same way this change now does, with `VoicePickerViewController` supplying 100pt for
  `hRegular_vRegular` and 64pt elsewhere.
