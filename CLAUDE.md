# Vocable Android

AAC (Augmentative and Alternative Communication) app for users with motor/speech impairments. Primary input is ARCore head/face tracking rendered as a gaze cursor with dwell-click, with touch as fallback. Fully offline/local-first — no backend, no network layer.

See `Documentation/architecture-diagrams.md` for the user-journey and tech-stack diagrams that accompany this baseline.

## Cross-repo: vocable-ios

This repo has a sibling iOS repo, `willowtreeapps/vocable-ios` (default branch `develop` — not `main`). **iOS development is ahead of Android** — new features typically land on iOS first. When a ticket asks Android to add or match a feature that already exists on iOS, check the Swift implementation for the intended UX/behavior/edge cases before designing the Android version, rather than reinventing it from scratch.

**The shipped iOS implementation is the source of truth for design and behavior — not Figma.** Where a Figma mock and the iOS code disagree, follow iOS and flag the divergence back to design on the relevant design ticket rather than building the mock. Figma is a useful reference for anything iOS doesn't cover, but it lags and has been wrong: on #636 the mock called for a 3px selected-state border where iOS uses a trailing checkmark, showed one column on tablet landscape where iOS uses two, and showed per-voice proper names (`"Aria (Enhanced)"`) that Android TTS cannot produce at all. Also confirm parity is even *achievable* before committing to it — platform APIs differ, and where they do, say so on the ticket instead of forcing a match.

**Reading the iOS source.** Clone locations vary by machine — it's often cloned alongside this repo at `../vocable-ios`, but don't assume that. Check for a local clone first and use it if present. If there isn't one, the repo is public, so read it without cloning rather than asking someone to clone it:

```
gh api "repos/willowtreeapps/vocable-ios/git/trees/develop?recursive=1" --jq '.tree[].path' | grep -i <feature>
gh api "repos/willowtreeapps/vocable-ios/contents/<path>?ref=develop" --jq '.content' | base64 -d
```

Either way, start with the iOS repo's own `CLAUDE.md` for orientation, and note that localized copy lives in `Vocable/Supporting Files/Localizable.xcstrings` — it's JSON, so parse it rather than grepping.

Confirmed architecture differences (don't port 1:1): iOS is UIKit+Combine+singletons+Core Data with no ViewModel layer and no DI framework — nothing like Koin/Compose/MVI. iOS solves "editing a preset phrase" by mutating one row in place (`isUserRenamed` flag) instead of Android's shadow-phrase duplicate-row approach, so iOS has no analog to Android's PR #611 shadow-phrase sort-order bug class. iOS's dwell-click is *not* gated on TTS completion the way Android's `GazeClickable` is — treat that as a platform-behavior question to confirm with product, not an automatic bug on either side.

## Modules

- `:app` — the entire application (single module: UI, data, domain, DI). `com.willowtree.vocable` package.
- `:basetest` — thin shared test-fixture module (depends on `:app`, which is unusual but intentional). Currently only 2 files; most test fakes actually live inline under `app/src/test`.
- `build-logic` (included build) — Gradle convention plugins (`vocable.application`, `vocable.library`) centralizing compileSdk 36 / minSdk 24 / Java+Kotlin 17. Only toolchain config is centralized here — Compose/KSP/Koin/signing are configured per-module in `app/build.gradle.kts`.

No product flavors. Just `debug` (has `USE_HEAD_TRACKING` BuildConfig flag, toggle with `-PUSE_HEAD_TRACKING`) and `release`.

## Architecture

- **DI**: Koin (not Hilt/Dagger). One flat module: `di/AppKoinModule.kt`. ViewModels injected via `by viewModel()` (Activities) / `koinViewModel()` (Compose). `MainActivity`/`SplashActivity` have Koin activity-scopes for things like `FaceTrackingManager`/`PermissionsChecker`.
- **UI**: 100% Jetpack Compose, no XML layouts, no Fragments. Single `NavHost` in `ui/VocableNavHost.kt` using raw string routes with manual `URLEncoder`/`URLDecoder` for args (no typed nav-args).
- **Presentation pattern**: MVI via `ui/base/BaseViewModel.kt` (`StateFlow` for state + buffered `Channel` for one-shot events) and `ui/base/MviScreen.kt`. **Not all ViewModels follow this** — `PresetsViewModel`, `SplashViewModel`, `SensitivityViewModel` use ad hoc `StateFlow`+`LiveData` instead. When touching those, prefer migrating to `BaseViewModel` over adding more ad hoc state, but don't do a drive-by rewrite unrelated to the ticket.
- **Package layout**: `core/` (cross-cutting: prefs, TTS, face tracking, permissions, locale), `data/room/` + `data/repository/` (Room DAOs/DTOs + repos), `domain/model/` + `domain/usecase/` (interface+impl pairs, e.g. `ICategoriesUseCase`/`CategoriesUseCase`), `di/`, `ui/<feature>/` (one folder per screen).
- **Data**: Room DB (`data/room/VocableDatabase.kt`, currently v7). Four entities: `CategoryDto`/`PhraseDto` (user/stored) vs `PresetCategoryDto`/`PresetPhraseDto` (seeded, built-in). Migrations are hand-written SQL (`VocableDatabaseMigrations.kt`) plus one `@AutoMigration` for 6→7.

## Known fragile areas — read before touching

- **"Shadow phrase" mechanism**: editing a preset phrase soft-deletes the `PresetPhraseDto` row and inserts a `PhraseDto` ("shadow") reusing the same `phraseId` (an arrays.xml resource name, not a UUID — genuine custom phrases get real UUIDs). This has caused a whole recent bugfix chain (PR #611: `f715a87a`, `a865f6be`, `8ad4ea03`, `c93f8f95`) around sort-order getting stranded between the preset row and its shadow. `RoomPresetPhrasesRepository.ensurePopulated()` resyncs sort order against the resource-array index on every cold start (guarded by an in-process `Mutex`, not a real Room migration).
- **Explicitly unresolved edge case** (documented in `app/src/main/res/values/arrays.xml`): the resync algorithm handles additions/reorders within a preset array but **not removals or moves between arrays**. If a ticket touches preset category arrays, check whether it can trigger this.
- **Keypad grid shape** (`ui/presets/PresetsScreen.kt`, `PresetCategories.USER_KEYPAD`) is **not** currently pinned by any test. This file previously claimed it was covered by `app/src/test/java/com/willowtree/vocable/presets/{KeypadPhraseOrderTest,ResourceXml}.kt`; neither file exists anywhere in the repo (verified on #644 across `src/test` and `src/androidTest`), so treat keypad row/column changes as unguarded. A past version of a test in this area had false-negative gaps (hardcoded breakpoint list, partial-row assertions) that let a real layout regression ship — the lesson stands: don't assume existing tests are exhaustive, verify the assertion actually covers the new breakpoint/shape. For the working pattern, see `app/src/test/java/com/willowtree/vocable/voiceselection/VoiceGridResourcesTest.kt` (#644), which asserts a full per-breakpoint `integers.xml` matrix by parsing the XML off disk — there's no Robolectric in this repo, so a JVM test can't go through a real `Resources`.
- **`CategoriesUseCase.moveCategoryUp/Down`** reads-then-writes sort order with no lock (unlike the phrase repos' `Mutex`-guarded `ensurePopulated`). Categories don't have a shadow concept yet, but the stored/preset split is structurally identical to phrases — the same race class could recur here.
- Grid dimensions (`phraseColumns`/`phraseRows`/`maxPhrases`, etc.) come from `integerResource`s per screen-size breakpoint dir (`values`, `values-land`, `values-sw400dp`, `values-sw600dp`, ...) — fixed tile positions are a deliberate accessibility contract for gaze/motor-impaired users, not just a layout choice. Don't casually switch to a fluid/lazy grid.
  - **Known limit of that convention, and the one deliberate exception.** `sw###dp`/`-land` qualifiers constrain *width*, never height, so a hardcoded row count is only ever right for the screen heights it was tuned on. Where rows are a fixed height rather than weighted, that shows up as dead space at the bottom of a *full* page on a taller device and clipped rows on a shorter one — both in the same values dir. This shipped in #644 and was fixed in #667 (`ui/voiceselection/VoiceSelectionScreen.kt`): the Change Voice picker keeps `voice_columns` in resources but derives its row count from the measured page height against a `voice_row_min_height` dimen, then fills by weight. Columns still come from a resource, tile positions are still fixed for a given config, and page capacity is still identical across pages. If you touch that screen, don't "restore the convention" by reintroducing a `voice_rows` integer — `VoiceGridResourcesTest` fails if you do, on purpose.

## Accessibility / input subsystem

- Head tracking: ARCore `AugmentedFace` NOSE_TIP pose → `FaceTrackingViewModel` → smoothed cursor. Dwell-click lives in `ui/modifiers/GazeClickable.kt` (default 1000ms dwell, then holds "selected" until TTS finishes speaking or a 500ms fallback — dwell visuals are coupled to TTS completion, don't decouple without checking both).
- `core/GazeInteractionManager.kt` is the global registry of gaze targets/dwell state; `ui/components/GazePointer.kt`/`GazeButton.kt` render the cursor.
- TTS: `core/VocableTextToSpeech.kt`, wraps Android `TextToSpeech`, exposes `isSpeakingFlow`, has region→language-only locale fallback.
- No switch-access/scanning support exists despite being common in AAC. No high-contrast or font-scale settings; theme is a fixed dark Material3 scheme (`ui/theme/VocableTheme.kt`).
- Gaze hover feeds TTS announcements as a screen-reader substitute (`FaceTrackingViewModel` checks `AccessibilityManager.isEnabled`) rather than relying purely on native TalkBack focus — be aware of this when changing hover/focus behavior.

## Testing

- Unit tests: `app/src/test/` (JVM). Instrumented/Compose UI tests: `app/src/androidTest/` (Firebase Test Lab in CI, single device/locale). No MockK/Mockito anywhere — this repo uses **hand-written fakes only** (e.g. `FakeCategoriesUseCase`, `FakePhrasesUseCase`). Follow that convention; don't introduce a mocking library.
- Flow assertions use Turbine (`app.cash.turbine.test`).
- `RoomStoredCategoriesRepository`/`RoomStoredPhrasesRepository` (the custom-phrase data layer) have **zero** test coverage. Several ViewModels are untested too (`EditCategoriesViewModel`, `EditCategoryPhrasesViewModel`, `FaceTrackingViewModel`, `KeyboardViewModel`, `SensitivityViewModel`, `SplashViewModel`). If a ticket touches these, add tests rather than assuming there's a baseline to run regression against.
- Run unit tests: `./gradlew testDebug`. Assemble debug + androidTest APK (what CI does pre-device-tests): `./gradlew assembleDebug assembleDebugAndroidTest`.
- No lint/detekt/ktlint gate and no code-coverage tool exists in this repo currently — don't assume static analysis will catch style issues in CI.

## Docs that are stale — don't trust without checking code

- `CONTRIBUTING.md` describes a `develop` branch workflow that doesn't exist; all work actually merges to `main`.
- `ROADMAP.md` (untouched since 2020) is not a reliable status source — several items on it are already shipped (e.g. custom category create/edit/delete).
- `Documentation/HeadTracking.md` is accurate and current (2026) — this is the one architecture doc that matches reality; it correctly describes the Compose-based gaze stack (no legacy Fragment/View based tracking remains).
- Nothing in `Documentation/` covers Koin, the Room/repository layer, or Navigation-Compose — treat those as undocumented; this file is the source of truth for them until it's expanded.

## Conventions to follow when doing ticket work

- Match `BaseViewModel`/`MviScreen` MVI pattern for new/touched ViewModels unless there's a specific reason not to.
- Use `koinViewModel()` in Compose for DI, not activity-scope workarounds (see `SelectionModeViewModel` for an existing inconsistency to avoid replicating).
- Add hand-written fakes for new test doubles, not a mocking framework.
- If a fix touches the shadow-phrase/sort-order resync path, add or extend a test in `RoomPresetPhrasesRepositoryTest` (androidTest) or `CategoriesUseCaseTest` rather than assuming existing coverage is sufficient — this area has a track record of tests that looked sufficient but weren't. Note there is no `KeypadPhraseOrderTest` despite earlier versions of this file citing one; if a fix needs keypad-order coverage, it has to be written from scratch.

## Starting new work — standard workflow

This repo has a history of tickets (#613, #617, #622) that grew into sprawling, hard-to-follow documents — day-by-day plans embedded in the ticket body that went stale, duplicate "resolved/unresolved decisions" tables, inline strikethrough history. The workflow below exists to keep new work from repeating that.

1. **Always open a GitHub issue first**, before writing any code. Draft it with the `/create-ticket` skill (`.claude/skills/create-ticket/SKILL.md`), which enforces a tight Why/Scope/Acceptance-Criteria/Out-of-Scope/Links template and flags scope creep (>~8 acceptance criteria, or multiple unrelated concerns) before the issue is created — don't freehand a ticket body instead of using it.
2. **Branch naming** (applies going forward only — doesn't retroactively rename already-merged branches like `613/remove-language-library`): `feature/<issue-number>/<short-description>`, e.g. `feature/617/tts-live-voice-fallback`.
3. **One PR per issue.** For large/heavy parent tickets, break the work into sub-issues per discrete unit of work (`Part of #<parent>`) rather than landing the whole ticket as one PR — open each sub-issue with the same `/create-ticket` skill. PR body uses `.github/PULL_REQUEST_TEMPLATE.md` — `## Summary` (bullet points), `## Ticket` (e.g. `Part of #<parent> — closes #<issue>`), `## Type of Change`/`## Testing`/`## Checklist` checkboxes. For sub-issues of a large ticket, PRs target the parent's own integration branch (e.g. `feature/voice-selection`), not `main`, until the whole parent ticket is ready. This keeps the parent ticket's comment thread as a running index of completed sub-issues rather than one sprawling, hard-to-review diff.
   - **`closes #N` does not auto-fire on these merges.** GitHub only auto-closes a linked issue when the merging PR targets the repo's *default* branch (`main`). Since these sub-issue PRs target a parent's integration branch instead, the linked issue stays open even after merge — confirmed the hard way when #630 sat open for a while after PR #631 merged. **Manually close the linked issue right after the PR merges**, with a comment pointing to the merged PR — don't rely on the `closes #N` text to do it.
4. **Write a work-log doc as part of the same PR**: `Documentation/work-log/<issue-number>-<slug>.md`, covering what the task was and why it was needed, what was actually changed, key decisions and why (especially anything not obvious from reading the code alone), and pointers to the issue/PR numbers. This is the durable, skimmable record for onboarding new engineers and for future Claude sessions — git/GitHub history is the source of truth for exact detail, but the *reasoning* shouldn't only live there.
