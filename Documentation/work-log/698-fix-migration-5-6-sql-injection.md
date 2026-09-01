# #698 — Fix SQLiteException crash in MIGRATION_5_6 from unescaped apostrophes

## What and why

Live crash reported 2026-09-01: `android.database.sqlite.SQLiteException` thrown
from `VocableDatabaseMigrations$MIGRATION_5_6$1.migrate` (`VocableDatabaseMigrations.kt:173`),
reached via `SQLiteOpenHelper.getWritableDatabase` during app startup. Because
this throws inside Room's `onUpgrade`, the database never finishes opening —
the app is unusable for any affected user until fixed.

`MIGRATION_5_6` built its `Phrase_New`/`Category_New` `INSERT` statements by
interpolating Kotlin values directly into a SQL string literal, e.g.:

```kotlin
db.execSQL("INSERT INTO Phrase_New (...) VALUES ('$parentID', $creationDate, $lastSpokenDate, '$localizedUtterance', $sortOrder)")
```

`localizedUtterance` comes from `Phrase WHERE is_user_generated=1` — real
user-authored text. Any apostrophe in that text ("I'm hungry", "It's my
turn") closes the SQL string literal early and corrupts the statement,
throwing `SQLiteException` instead of inserting a row. The same pattern
existed for `localized_name` in the `Category_New` insert.

This is a sibling bug to the one fixed in #694 / PR #695: that fix addressed
a different symptom of the same root cause (a null Kotlin value
interpolating into the SQL literal as the 4-character string `"null"`,
producing a degenerate-but-non-null value that crashed later on *read*).
This ticket fixes the *write-time* SQL-syntax-error symptom, and fixes the
underlying pattern for both symptoms at once.

## What changed

Both `execSQL` calls in `MIGRATION_5_6` now use parameterized queries
(`?` placeholders + a bind-args array) instead of string interpolation.
This removes the escaping problem entirely, and as a side effect binds a
true SQL `NULL` for a null Kotlin value instead of the literal string
`"null"` — closing the root cause the #694 data-repair migration works
around downstream. `MIGRATION_7_8`'s existing repair logic is left in place
since it's still needed for any device whose data already has the literal
`"null"` string from before this fix.

Added `migrate5to6_preservesApostropheInUserGeneratedText` to
`MigrationTest.kt`, seeding a v5 database with a user-generated phrase and
category name that each contain an apostrophe, then running `MIGRATION_5_6`
and asserting both the migration succeeds and the text round-trips exactly.
Prior to this fix, that test's `runMigrationsAndValidate` call throws the
same `SQLiteException` as the field report. The test's own v5 fixture data
is seeded via bind args rather than string interpolation, for the same
reason — its own setup would otherwise hit the identical class of bug.

## Verification

No local Firebase config (`google-services.json`) or connected
device/emulator was available in this environment, so the new test wasn't
executed instrumented — it will run via Firebase Test Lab in CI along with
the rest of `androidTest`. `compileDebugKotlin`, `compileDebugAndroidTestKotlin`,
and `testDebug` (all JVM unit tests) were verified to pass locally (a
throwaway placeholder `google-services.json` was used only to unblock the
Gradle Google Services plugin for these compile/unit-test tasks, then
deleted — it was never committed, and `app/.gitignore` already excludes the
real file).

## Pointers

- Issue: #698
- Related: #694 / PR #695 (prior fix for the null-literal symptom of the same
  root cause), #696 (separate v6→v7 auto-migration test-coverage gap)
