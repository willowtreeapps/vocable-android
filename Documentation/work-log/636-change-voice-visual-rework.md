# Change Voice screen: visual rework to match iOS

**Issue:** #636 (Part of #613 — the overall Voice Selection feature)

## What was needed

`VoiceSelectionScreen.kt` worked functionally — list, pagination, swipe gestures — but had been built against an earlier design. It was titled "Voice" rather than "Change Voice", showed selection as a plain "Selected" text label, carried a "Default" row with no iOS counterpart, and reimplemented pagination inline with icons and strings borrowed from the Presets screen.

The governing decision for this work, made while grilling the ticket, is that **the shipped iOS implementation is the source of truth for design and behavior — not Figma**. Figma had been the assumed reference, and it turned out to be wrong in three places (see "Design divergences" below). That rule is now written into `CLAUDE.md`'s cross-repo section so it applies to the sibling tickets too.

## What changed

- **Title** `voice_selection_title`: "Voice" → "Change Voice", matching iOS's `voice_picker.title`. `close_voice_selection` updated to match.
- **Selected state** is now a trailing checkmark (new `ic_check.xml`) instead of the word "Selected", matching iOS's `VocableListCellAccessory.checkmark`. The existing `selected` string was retained and repurposed as the icon's `contentDescription`.
- **"Default" row removed**, along with the `voice_default` string. iOS has no device-default affordance in its picker.
- **Pagination extracted** to a new shared `ui/components/VocablePagination.kt`, mirroring iOS's `PaginationView`: horizontally centered rather than spread `SpaceBetween`, with both buttons disabled when there is only one page.
- **Pagination content descriptions** added (`previous_page` / `next_page`) via both `GazeButton`'s `accessibilityLabel` and the `Icon`'s `contentDescription`. The previous inline arrows passed `contentDescription = null`.
- **Empty state** added, mirroring iOS's `VoicePickerEmptyStateConfiguration` — "No Voices" plus an explanatory line. Previously an empty voice list rendered as blank spacers.
- **`phrases_page_number` format bug fixed** across all 13 locale files (see below).
- **`VoiceSelectionViewModelTest.kt`** added — 9 tests. Neither this screen nor its ViewModel had any coverage before.

## Key decisions, and why

### Checkmark, not a 3px border

The ticket originally called for a 3px "Active Green" border, taken from the Figma mock. iOS uses only a trailing checkmark — `shouldSelectItemAt` returns `false` in `VoicePickerViewController`, so the cell's selection-background path never fires there at all.

Independently of parity, a green border would have collided with two existing gaze states: `VocableButton` already uses `SelectedColor` (#00FA9A) as its **dwell-press** fill and a 4dp `ColorAccent` amber **border** on hover. A persistent green border on the selected row would read as "this row is being pressed right now" to exactly the users least able to tolerate that ambiguity.

### Wraparound paging was kept, not clamped

An earlier revision of this ticket called for clamping paging at the first and last page. That was wrong, and was corrected on the issue before any code was written. iOS's carousel is pseudo-infinite: `CarouselCollectionViewDataSourceProxy` repeats the content **100 times** (`repeatCount = 100`) and `scrollToMiddleSection` starts the user mid-list, so there is no reachable first or last page. Android's existing `% totalPages` arithmetic already matched that behavior.

Clamping would also have produced a dead control: iOS disables the buttons purely on `pageCount > 1`, not on position, so with clamping the "previous" button would sit enabled-but-inert on page 1 — a full 1000ms dwell that does nothing. iOS's two rules are only coherent together.

The wrap arithmetic was additionally hoisted into shared `goToPreviousPage` / `goToNextPage` lambdas, so the swipe gestures and the pagination buttons can no longer drift apart; each previously had its own copy.

### The empty state keeps pagination visible

iOS's `PagingCarouselViewController.isPaginationViewHidden` defaults to `false` and `VoicePickerViewController` never sets it, so iOS renders its empty state with the pager still present and disabled. Android now does the same: with zero voices `totalPages` floors at 1, so a disabled "Page 1 of 1" shows beneath the message. Slightly noisy, but consistent with every other paging screen and it avoids layout jumping once voices load.

### `phrases_page_number` had a real format bug

The string read `Page %1d of %2d`. Without the `$`, Java's formatter reads `%2d` as *"decimal, minimum width 2"*, so any page count below 10 was space-padded — the screen literally rendered `Page 1 of  6`. Confirmed directly against `String.format` before and after the fix.

All 13 locale files now use proper positional `%1$d` / `%2$d`. **This fix reaches beyond this screen**: `phrases_page_number` is shared by Presets, EditCategories and EditCategoryPhrases, all of which were showing the same artifact.

Two other strings (`saved_successfully` / `removed_successfully` with `%1s`, `edit_categories_button_number` with `%2s`) have the same malformed syntax but are harmless — a width-1 spec never pads a non-empty string — so they were deliberately left alone rather than widening the diff across another 26 entries.

### `VocablePagination` is used by only one screen so far

The component was written to be shared, but only `VoiceSelectionScreen` consumes it in this PR. Migrating Presets, EditCategories and EditCategoryPhrases onto it is deliberately deferred: `PresetsScreen` is a known-fragile, test-pinned area, and folding it into a visual-rework PR would have made the diff much harder to review. `buttonSize` is passed in as a parameter rather than resolved inside the component, so the breakpoint work in #644 doesn't have to reach into shared code.

## Design divergences flagged back to Figma (commented on #622)

1. **Selected state** — mock shows a 3px border; iOS uses a checkmark.
2. **Tablet landscape columns** — mock node `6537:3331` shows one column; iOS uses two.
3. **Voice names** — mock shows `"Aria (Enhanced)"`. Not achievable: `AVSpeechSynthesisVoice.name` gives iOS human names, whereas Android TTS returns identifiers like `en-us-x-tpf-local`, which is why `buildVoiceDisplayName()` constructs `"English (United States) – Enhanced"` from locale + quality. This also resolves #622's open "quality-label vocabulary" question as N/A — iOS shows no quality label at all, so there is nothing to reconcile.

## Scope deliberately split out

The grilling pass produced more work than one reviewable unit, so two sibling issues were opened rather than growing this one past the `/create-ticket` scope guard:

- **#643** — play/preview button per row. Previously blocked on confirming a sample phrase; iOS's is now known (`"Hello, this is %1$@"`, rate 0.5). Android can't interpolate the voice name, so the phrase needs a non-parameterized replacement — a product copy decision raised on that issue.
- **#644** — responsive 2-column grid matching iOS's size classes, replacing the hardcoded `itemsPerPage = 3/5`.

Also still outstanding and owned elsewhere: **#618** hides undownloaded voices and deletes the download-icon branch in `VoiceOptionRow`, which this PR deliberately left untouched.

**Ordering note:** an earlier plan had #618 landing before this issue since both touch the same `when` block. That was reversed — #618's final acceptance criterion needs cross-engine confirmation from the #634 hardware spike, so gating visual work on it would have stalled indefinitely. #636 goes first; #618 rebases and deletes the branch it already owns. This issue must also land **before #637**, which removes the Selection Mode entry point that is currently the only way to reach this screen.

## Known verification gap

**The selected-state checkmark could not be verified on device.** On the emulator used, every voice reports `isDownloaded == false`, so every row shows the download arrow and tapping routes to `onDownloadVoice()` rather than `onVoiceSelected()` — meaning no voice can be selected and the checkmark can never render. Most likely the emulator simply has no TTS voice data installed (#634 recorded the same limitation during #632); the alternative is that its engine over-reports `KEY_FEATURE_NOT_INSTALLED`, which is the exact inconsistency #618/#634 exist to pin down. Either way it is useful empirical input for those tickets.

The checkmark and the disabled-pagination state were verified via Compose previews (`VoiceSelectionScreenPreview`, `VocablePaginationSinglePagePreview`) instead. **Someone with a physical device carrying real installed voices should confirm the checkmark renders and behaves before #620's final QA.**

Unit-test coverage has a matching limitation, recorded in the test class's KDoc: `VocableTextToSpeech` is a global object over the real Android engine, so `getAvailableVoices()` always returns empty under JVM tests. Selection and persistence are fully covered; the voice list only for its empty/no-crash path. Closing that would need an injected seam on the ViewModel, deliberately out of scope here.

## Pre-existing issues found but not fixed

Noted here so they aren't rediscovered as regressions from this PR:

- **Screen title is ~36dp right of center.** The header row is `[close button 72dp][spacer][title][spacer]`, so the title centers in the leftover width rather than the full width. Diverges from both iOS (nav-bar centered) and Figma (title dead-center). Untouched by this PR.
- **Bottom system bar overlaps content.** `MainActivity.kt:103` applies `windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))` — horizontal only — while `VocableTheme` sets `setDecorFitsSystemWindows(window, false)`. In landscape the nav bar draws over the pagination row, so part of a dwell target is obscured. Affects every screen; fixing it globally is well outside this ticket.
- **`onVoiceSelected: (String?) -> Unit` is now never called with null from the UI.** The null path is still load-bearing for the stale-voice revert in `MainActivity`/`PresetsViewModel`/`KeyboardViewModel`, so the signature was left as-is and a test pins the behavior. Narrowing the type would be a drive-by change and #643 reworks this call path anyway.
- **`settings.gradle.kts` still declares the `maven.pkg.github.com/willowtreeapps/FuelIXLate` repository**, first in both `pluginManagement` and `dependencyResolutionManagement`, despite #630 removing the FuelIX pipeline. With no `gpr.user`/`gpr.token` configured, every dependency resolution attempt hits it, is rejected, and falls through. Tolerated rather than fatal, but dead config that slows resolution and may destabilize IDE sync.

## Build environment note

`./gradlew` fails with "Unable to locate a Java Runtime" unless `JAVA_HOME` is set — there is no JDK on the shell `PATH`. Homebrew's `openjdk@21` works and matches the `toolchainVersion=21` in the (untracked) `gradle/gradle-daemon-jvm.properties`; Android Studio's bundled JBR 21 is the other option.

## Pointers

- Issue: #636 · Parent: #613
- Siblings created from this work: #643 (play/preview), #644 (2-column grid)
- Design divergences: comment on #622
- iOS reference: `Vocable/Features/Settings/VoiceSettings/VoicePickerViewController.swift`, `VocableListContentConfiguration+VoiceProfileItem.swift`, `Vocable/Common/Views/PaginationView.swift`, `Vocable/Common/CarouselCollectionViewDataSourceProxy.swift`
