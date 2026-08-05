# Reset: category/phrase data wipe + reseed

**Issue:** #639 (Part of #627 — Reset App Settings epic)

## What was needed

#360 made preset categories user-editable/deletable via a "shadow phrase/category" mechanism (edit a preset → soft-delete the preset row, insert a stored row reusing the same id) but never built the reset path its own acceptance criteria assumed would exist. #627 requires Android to mirror iOS: delete every `Phrase`/`Category` row and re-seed from the bundled presets, exactly reversing any #360-driven edit, hide, deletion, or addition — not just the preset shadows, genuine user-added categories/phrases too.

## What changed

- **New DAO-level bulk deletes**: `deleteAllCategories()` (`CategoryDao`), `deleteAllPhrases()` (`PhraseDao`), `deleteAllPresetCategories()` (`PresetCategoryDao`), `deleteAllPresetPhrases()` (`PresetPhrasesDao`) — none of the four DAOs had any "delete everything" query before this; the closest existing thing was the stock `RoomDatabase.clearAllTables()`, which was deliberately not used here since it clears all four tables in one shot with no chance to reseed in between and isn't exercised per-repository the way the rest of this layer's tests are structured.
- **`RoomPresetCategoriesRepository` gained a public `populateDatabase()`**, wrapping the existing private `ensurePopulated()` — mirroring `RoomPresetPhrasesRepository`'s existing `populateDatabase()`. Previously `ensurePopulated()` for categories only ran implicitly (`.onStart{}` on the `getPresetCategories()` flow, or before each mutating call), so there was no way to force a deterministic reseed from outside the repository.
- **`ICategoriesUseCase.resetToDefaults()` / `IPhrasesUseCase.resetToDefaults()`** added. `PhrasesUseCase.resetToDefaults()` deletes all stored + preset phrase rows, then repopulates presets. `CategoriesUseCase.resetToDefaults()` delegates to `phrasesUseCase.resetToDefaults()` first (it already holds a `PhrasesUseCase` reference for `deleteCategory`'s existing phrase cascade, so this reuses that same orchestration seam), then deletes all stored + preset category rows and repopulates. **A single call — `categoriesUseCase.resetToDefaults()` — is the one entry point #640 needs to wire to the reset button**, matching how `deleteCategory` already cascades into phrases through the same collaborator.
- **Fakes updated**: `FakeCategoriesUseCase.resetToDefaults()` / `FakePhrasesUseCase.resetToDefaults()` added as unimplemented stubs (`TODO`/`error`, matching each fake's existing convention for methods no current ViewModel test exercises) — required just to keep both fakes compiling against the expanded interfaces.
- **Tests**: 3 new cases in `CategoriesUseCaseTest` (reset after category edit / deletion / custom addition) and 4 new cases in `PhrasesUseCaseTest` (reset after phrase edit / deletion / custom addition, plus Recents clearing) — all androidTest, real in-memory Room, following the existing file's established pattern rather than introducing a new one.

## Why "Recents resets to empty" needed its own explicit test

Recents has no dedicated table — it's derived live from `last_spoken_date` on both `Phrase` and `PresetPhrase` (`PhraseDao.getRecentPhrases`/`PresetPhrasesDao.getRecentPhrases`, both `WHERE last_spoken_date IS NOT NULL`). A full wipe-and-reseed satisfies "Recents empty" for free as long as the reseed inserts fresh rows with `lastSpokenDate = null` — which it does, since `RoomPresetPhrasesRepository`'s populate path already builds every `PresetPhraseDto` with `lastSpokenDate = null`. `reset_clears_recents` pins this explicitly rather than trusting it as an implied side effect, since a future change to the reseed path that forgot to null out spoken dates would silently break Recents without any other test catching it.

## Known gap: no Compose UI test yet

#639's acceptance criteria calls for "an instrumented UI test confirming post-reset state matches a fresh install's default category/phrase set exactly." What's here is instrumented (`androidTest`, real Room, real `Context`-backed resources) but exercises the use-case layer directly — there is no reset button or confirmation dialog to drive from Compose yet, since that's #640's scope (Settings UI entry point). A true UI-level test belongs on #640, once there's a screen action to click; this ticket verifies the underlying data operation is correct so #640 isn't debugging both the wiring and the reset logic at once.

## Not touched

- **`ensurePopulated()`'s pre-existing additive-only limitation** (documented in `arrays.xml` and CLAUDE.md — it never revives a `deleted=1` row or fixes drifted `sort_order`/`hidden` on a row that already exists) is irrelevant to this ticket's reset path specifically, because reset always starts from a fully empty table (every row hard-deleted first) — every entry is "new" on the reseed pass, so the additive/non-reconciling behavior that causes the PR #611 bug class never comes into play here. It's still a live gap for the *normal*, non-reset cold-start path; out of scope to fix in this ticket.
- **`CategoriesUseCase.moveCategoryUp`/`moveCategoryDown`'s unguarded read-then-write race** (flagged in CLAUDE.md) — untouched, unrelated to reset.
- **Preference wipe** (#638, merged into this branch) and **the reset UI entry point/confirmation dialog** (#640) — both explicitly out of scope per the ticket.

## Branch note

Same stack as #638: this branch (`feature/639/reset-category-phrase-reseed`) is based on `feature/638/reset-preference-wipe`, which is itself based on `feature/voice-selection`. PR targets `feature/638/reset-preference-wipe` to avoid rebasing/merge churn while both are in flight; per this repo's sub-issue convention, `closes #639` won't auto-fire on merge into a non-default branch, so the issue needs to be closed manually afterward with a link to the merged PR.

## Pointers

- Issue: #639 · Parent: #627 · Related: #360 (why reset isn't a pure settings-values reset) · Sibling: #638 (preference wipe, same branch stack)
- Files: `data/room/{CategoryDao,PhraseDao,PresetCategoryDao,PresetPhrasesDao}.kt`, `data/repository/{StoredCategoriesRepository,RoomStoredCategoriesRepository,StoredPhrasesRepository,RoomStoredPhrasesRepository,PresetCategoriesRepository,RoomPresetCategoriesRepository,PresetPhrasesRepository,RoomPresetPhrasesRepository}.kt`, `domain/usecase/{ICategoriesUseCase,CategoriesUseCase,IPhrasesUseCase,PhrasesUseCase}.kt`, `FakeCategoriesUseCase.kt`, `FakePhrasesUseCase.kt`, `androidTest/.../{CategoriesUseCaseTest,PhrasesUseCaseTest}.kt`
