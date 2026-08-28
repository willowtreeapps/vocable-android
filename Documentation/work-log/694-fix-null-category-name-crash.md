# #694 — Fix crash on Category rows with NULL/degenerate localized_name

## What and why

Play Console reported a production crash in 1.6.0 (build 607):
`java.lang.IllegalStateException` in
`CategoryDao_Impl.getAllCategoriesFlow$lambda$0` (4 users, 11 events). Since
`PresetsScreen`/`PresetsViewModel` is the app's start destination and calls
`CategoriesUseCase.categories()` (which combines this exact query), the crash
happens on every launch for affected users — the app never gets past the home
screen for them.

`CategoryDto.localizedName` is declared non-null (`LocalesWithText`), but two
independent legacy migration paths can leave the physical `localized_name`
column in a state that breaks that promise:

- `MIGRATION_3_4` (`VocableDatabaseMigrations.kt:105`) inserts a literal SQL
  `NULL` for the `preset_user_favorites` ("My Sayings") category.
- `MIGRATION_5_6` reads that value back with
  `categoriesCursor.getString(...)` (returns Kotlin `null`) and interpolates
  it into a new `INSERT` via `'$localizedName'` — which for a null Kotlin
  value produces the literal 4-character string `"null"`, not a real SQL
  `NULL`. This is a distinct, non-null-but-degenerate value that survives
  into later schemas untouched.

Initial investigation assumed a type-converter-level null check was the
culprit (`Converters.languagesWithTextToStringMap` returning null was seen as
crashing a non-null field). That's real but incomplete: decompiling the
`androidx.sqlite` classes Room 2.8 actually uses here
(`SupportSQLiteStatement$RowSQLiteStatement`) showed that for a column backed
by a **non-null** Kotlin field, Room's generated code skips any `isNull()`
check and calls `getText()` directly, which wraps `Cursor.getString()` in
Kotlin's `Intrinsics.checkNotNullExpressionValue` — this throws
`IllegalStateException` (matching the reported crash exactly) *before* any
type converter runs. So a genuine SQL `NULL` in `localized_name` can never be
fixed at the converter/read layer for a non-null field; it has to be repaired
in the data itself.

## What changed

- **`VocableDatabaseMigrations.kt`**: added `MIGRATION_7_8`, a one-time data
  repair — `UPDATE Category SET localized_name = '{}' WHERE localized_name IS
  NULL` — for the genuine-SQL-NULL case (`MIGRATION_3_4`'s row).
- **`VocableDatabase.kt`**: bumped `version` 7 → 8 and registered
  `MIGRATION_7_8`. No entity/column changes, so `schemas/8.json` is
  structurally identical to `schemas/7.json` (verified via diff) — this is a
  pure data migration.
- **`Converters.kt`**: added `languagesWithTextToStringMapNonNull`, used by
  Room for `CategoryDto.localizedName` specifically, falling back to
  `LocalesWithText(emptyMap())` instead of null. This covers the *other*
  case — a non-null column value that's degenerate JSON (the literal `"null"`
  string from `MIGRATION_5_6`, or any other unparseable content) — which
  *does* reach the converter, since `getText()` only throws for an actual SQL
  `NULL`, not for a non-null-but-nonsense string.
- Tests:
  - `MigrationTest.migrate7to8_repairsNullLocalizedName` — seeds a v7 DB with
    a genuine `NULL` `localized_name` row, runs `MIGRATION_7_8`, and asserts
    the DAO reads it back as an empty `LocalesWithText` instead of crashing.
  - `RoomStoredCategoriesRepositoryTest.getAllCategories_treatsLiteralNullLocalizedNameAsEmpty`
    — seeds a fresh (already-v8) DB with the literal `"null"` string and
    asserts the same fallback, independent of any migration.

## Notes

- There is no `migrate6to7` test in this repo, and per a compiler warning
  surfaced while adding the new migration test, this file's
  `MigrationTestHelper` is constructed with a deprecated overload that
  **cannot** validate auto-migrations at all — so `Version7Migration` (the
  `@DeleteColumn` auto-migration between 6 and 7, per a `VocableDatabase.kt`
  TODO noting "we never released 6") has never actually been exercised by a
  test. That's a real gap but out of scope here per #694's acceptance
  criteria; flagging in case v6→v7 needs its own follow-up.
- The androidTest additions weren't run against a local emulator in this
  session (none configured on this machine) — verified via
  `./gradlew :app:compileDebugAndroidTestKotlin` (compiles cleanly) and
  `./gradlew :app:testDebugUnitTest` (existing suite still green). Per
  `CLAUDE.md`, this repo's androidTest suite runs in CI via Firebase Test
  Lab, which will exercise both new tests on the PR.
- Root-causing exactly which of the two legacy paths hit the 4 affected
  production users, and any backfill of a real (non-empty) category name for
  them, is out of scope per #694 and would need Play Console's full device
  breakdown.
