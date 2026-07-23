# CLAUDE.md

Guidance for Claude Code (claude.ai/code) working in the **Vocable AAC for Android** repo.

Vocable is an open-source AAC (augmentative & alternative communication) app: it lets people
with conditions such as ALS, MS, stroke, or spinal-cord injuries communicate hands-free via
ARCore head-tracking (or by touch). Real users rely on it daily — treat changes with care.

## The three Vocable repos (this is one of them)

- **`vocable-android`** (here) — the main target for the current work; behind iOS on features.
- **`vocable-ios`** — mature, the reference implementation. **Do not modify it.** It's a separate
  repo, cloned as a sibling for reference only.
- **`vocable-api`** — Node/TypeScript backend, IT-access-gated. Not clonable without access.

## Build, test, run

JDK 17+. Everything is Gradle (Kotlin DSL) via the wrapper — always use `./gradlew`.

```bash
./gradlew testDebug                              # unit tests (JVM) — the CI gate
./gradlew assembleDebug                          # build the debug APK
./gradlew assembleDebugAndroidTest               # build the instrumentation-test APK
./gradlew lint                                   # Android Lint
./gradlew :app:testDebug --tests "com.willowtree.vocable.SomeTest"   # a single test class
```

- **Unit tests** live in `app/src/test` and run on the JVM (`testDebug`) — this is what CI
  (`.github/workflows/build.yml`) runs; keep them green.
- **Instrumentation / UI tests** use the custom runner `com.willowtree.vocable.utility.VocableTestRunner`
  and run on-device. CI runs them on **Firebase Test Lab** (`.github/workflows/device-tests.yml`),
  not on a local emulator by default.
- Run the app from Android Studio (open the repo root) on an **ARCore-supported device** for
  head-tracking; touch mode works on any device/emulator.

## Layout

Gradle modules (`settings.gradle.kts`): **`:app`** (the application) and **`:basetest`** (shared
test scaffolding). **`build-logic/`** is an included build holding convention plugins — edit
build logic there, not by copy-pasting across module `build.gradle.kts` files.

App code is under `app/src/main/java/com/willowtree/vocable/`:

- `ui/` — Jetpack **Compose** screens/components (the app is Compose-first; Navigation Compose).
- `domain/` — use-cases / business logic.
- `data/` — repositories, **Room** database (KSP-generated; schemas in `app/schemas/`), Moshi models.
- `di/` — **Koin** modules (dependency injection).
- `core/` — cross-cutting utilities.
- `MainActivity.kt`, `VocableApp.kt` — entry points.

Stack: Kotlin 2.3.x, AGP 9.x, `compileSdk`/`targetSdk` 36, Jetpack Compose, Koin, Room (KSP),
Navigation Compose, Moshi, Coroutines, ARCore, Firebase (distribution/Crashlytics).

## Contributing workflow (from CONTRIBUTING.md)

- Branch from and **open PRs against `develop`** (not `main`).
- **CODEOWNERS review is required** — the core maintainers gate merges. For this project
  specifically: **do not merge without a maintainer (Chris) review.** A past contribution added
  un-WCAG-vetted theming that broke usability for colorblind users — the review gate exists for
  a reason.
- Issues use `bug` / `enhancement` labels; the project board is
  https://github.com/orgs/willowtreeapps/projects/50/views/1.

## Things that look wrong but are intentional ("don't poke the bear")

- **Head-tracking engine** (`Documentation/HeadTracking.md`) is a custom PID/robotics focus engine
  parallel to Android's — it has no concept of "which view is focused." It's fragile and
  hard-won; don't refactor it casually, and assume UI-automation of it needs test infrastructure
  first.
- **Accessibility color palette ≠ brand palette.** Colors are chosen for contrast / color-spectrum
  accessibility. Do not "fix" them to match branding without WCAG validation.
- **Localization inflection quirks** (Crowdin, `crowdin.yml`, `Documentation/Crowdin.md`) can look
  like bugs but are intentional. Confirm before "correcting" translation behavior.

## Reference docs in-repo

`README.md`, `ROADMAP.md`, and `Documentation/{HeadTracking,Firebase,Crowdin,ReleaseProcess}.md`.
