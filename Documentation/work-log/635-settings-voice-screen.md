# Settings root Voice row + Settings → Voice screen

**Issue:** #635 (Part of #613)

## What was needed

Voice selection had no reachable entry point from Settings — `SettingsViewModel.onVoiceSelection()` and `SettingsEvent.NavigateToVoiceSelection` already existed, and the nav host already routed that event straight to the Change Voice picker (`ROUTE_VOICE_SELECTION`), but `SettingsScreen.kt`'s options grid never exposed a button that called it, and the intermediate "Settings → Voice" preview screen called for by the Figma design (and shipped on iOS as `VoiceSettingsViewController`) didn't exist at all.

## What changed

- `VocableTextToSpeech.getActiveVoiceDisplayName(selectedVoiceName, locale)`: new public, read-only function that resolves whichever voice is actually active — the explicit pick if it's still installed and downloaded, otherwise the device's live current default for the locale — mirroring `applySelectedVoice()`'s resolution branches without mutating engine state. Needed because nothing in the codebase previously surfaced "the active voice" as a display string; the existing live-fallback logic from #632/#642 only resolved a voice internally at speak-time.
- New `ui/settingsvoice` package (`SettingsVoiceScreen`/`ViewModel`/`State`/`Event`), following the `BaseViewModel`/`MviScreen` MVI pattern: a preview row (play icon + `getActiveVoiceDisplayName` result, falling back to `"Default"` the same way `SelectionModeViewModel` does) and a "Change Voice" row that reuses the existing `SettingsButton` chevron composable. The preview row is a plain non-interactive `Row` — no `GazeButton`/dwell wiring — since tap-to-speak is explicitly blocked pending the sample-phrase decision called out in #613. It refreshes on `ON_RESUME` via the same `DisposableEffect`/`LifecycleEventObserver` pattern used by `SelectionModeScreen`/`VoiceSelectionScreen`, so returning from Change Voice updates the label.
- New nav destination `ROUTE_SETTINGS_VOICE`, inserted between Settings root and the existing Change Voice picker. `SettingsEvent.NavigateToVoiceSelection` now routes here instead of straight to the picker; the new screen's own `NavigateToChangeVoice` event is what reaches `ROUTE_VOICE_SELECTION`.
- `SettingsScreen.kt`: added the 4th `OptionItem` ("Voice") to the options grid, wired to `onVoiceSelection`.
- New vector drawable `ic_play_circle_40dp.xml` (standard Material play-circle glyph) — no play/preview icon existed anywhere in the app; iOS uses the SF Symbol `play.circle` for the same row, so this is the closest square match on Android's asset conventions.
- New strings under `Settings Options` (`settings_options_voice`) and a new `Settings -> Voice` section (`voice_settings_title`, `voice_settings_change_voice`, `voice_settings_preview_content_description`).
- DI: registered `SettingsVoiceViewModel` in `AppKoinModule.kt`.

## Key decisions

- **Repurposed `SettingsEvent.NavigateToVoiceSelection`** to mean "go to the new intermediate screen" rather than adding a same-shaped duplicate event, since it was unused from the UI layer until now and the nav host's handling of it was trivial to retarget.
- **Reused `SettingsButton`** (already shared by `SelectionModeScreen`) for the "Change Voice" row instead of introducing a new chevron-row composable.
- **No new mocking/fake infra** — `SettingsVoiceViewModelTest` uses the existing `FakeVocableSharedPreferences`. Because `VocableTextToSpeech`'s internal `TextToSpeech` engine is never initialized under this module's plain-SDK-stub unit test environment (same constraint documented in `VocableTextToSpeechTest`), every test case observes the `"Default"` fallback label rather than a resolved voice name — the live-resolution branch logic itself is already covered by `VocableTextToSpeechTest`'s `resolveVoiceSelection` tests, which `getActiveVoiceDisplayName` reuses directly.
- Verified live-resolution end-to-end on a Pixel 9 Pro XL emulator (not just unit tests): the preview row correctly showed the emulator's actual live default voice ("English (United States) – High Quality") with no explicit pick persisted, the play icon was confirmed inert, and back-navigation correctly threads Settings → Voice → Change Voice → back → Voice → back → Settings.

## Out of scope (tracked separately per #613/#635)

- Play/preview tap-to-speak behavior — blocked on sample-phrase confirmation.
- Change Voice screen's visual rework (rename, selected-state border, pagination) — #636.
- Removing the old Voice row from Selection Mode — #637.

## Pointers

- Issue: #635 · Parent: #613
- Design: Figma `6561:3335` (Settings root), `6562:3374` (Settings → Voice) — cross-checked against iOS's `VoiceSettingsViewController`/`VoiceProfilePreviewDataSource` for behavior the mock doesn't fully specify (live-fallback resolution, non-interactive preview row).
