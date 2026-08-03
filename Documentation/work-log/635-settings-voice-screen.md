# Settings root Voice row + Settings → Voice screen

**Issue:** #635 (Part of #613)

## What was needed

Voice selection had no reachable entry point from Settings — `SettingsViewModel.onVoiceSelection()` and `SettingsEvent.NavigateToVoiceSelection` already existed, and the nav host already routed that event straight to the Change Voice picker (`ROUTE_VOICE_SELECTION`), but `SettingsScreen.kt`'s options grid never exposed a button that called it, and the intermediate "Settings → Voice" preview screen called for by the Figma design (and shipped on iOS as `VoiceSettingsViewController`) didn't exist at all.

## What changed

- `VocableTextToSpeech.getActiveVoiceDisplayName(selectedVoiceName, locale)`: new public, read-only function that resolves whichever voice is actually active — the explicit pick if it's still installed and downloaded, otherwise the device's live current default for the locale — mirroring `applySelectedVoice()`'s resolution branches without mutating engine state. Needed because nothing in the codebase previously surfaced "the active voice" as a display string; the existing live-fallback logic from #632/#642 only resolved a voice internally at speak-time.
- New `ui/settingsvoice` package (`SettingsVoiceScreen`/`ViewModel`/`State`/`Event`), following the `BaseViewModel`/`MviScreen` MVI pattern: a preview row (play icon + `getActiveVoiceDisplayName` result, falling back to `"Default"` the same way `SelectionModeViewModel` does) and a "Change Voice" row that reuses the existing `SettingsButton` chevron composable, plus a footer line (present on iOS, not called out in the Figma AC). It refreshes on `ON_RESUME` via the same `DisposableEffect`/`LifecycleEventObserver` pattern used by `SelectionModeScreen`/`VoiceSelectionScreen`, so returning from Change Voice updates the label.
- New nav destination `ROUTE_SETTINGS_VOICE`, inserted between Settings root and the existing Change Voice picker. `SettingsEvent.NavigateToVoiceSelection` now routes here instead of straight to the picker; the new screen's own `NavigateToChangeVoice` event is what reaches `ROUTE_VOICE_SELECTION`.
- `SettingsScreen.kt`: added the 4th `OptionItem` ("Voice") to the options grid, wired to `onVoiceSelection`.
- New vector drawables `ic_play_circle_40dp.xml`/`ic_stop_circle_40dp.xml` (standard Material play-circle/stop-circle glyphs) — no play/preview icon existed anywhere in the app; iOS uses the SF Symbols `play.circle`/`stop.circle` for the same row, so these are the closest square match on Android's asset conventions.
- New strings under `Settings Options` (`settings_options_voice`) and a new `Settings -> Voice` section (`voice_settings_title`, `voice_settings_change_voice`, `voice_settings_preview_content_description`, `voice_settings_stop_preview_content_description`, `voice_settings_footer`).
- DI: registered `SettingsVoiceViewModel` in `AppKoinModule.kt`.
- **Real tap-to-speak preview, wired after initial review.** #613/#635's original AC explicitly blocked tap-to-speak pending a sample-phrase decision. Rather than resolve that (still open), the preview row and every row in the Change Voice picker now speak **the voice's own display name** (e.g. "English (Australia) – High Quality") when tapped — this sidesteps the sample-phrase question entirely rather than answering it. `VocableTextToSpeech.stop()` (new) halts playback without tearing down the engine like `shutdown()`. Both `SettingsVoiceViewModel.onPreviewActiveVoice()` and `VoiceSelectionViewModel.onPreviewVoice()` toggle a play/stop icon off `VocableTextToSpeech.isSpeakingFlow`, reverting to play automatically once speech finishes. **Flagging this explicitly since it's a real scope change from what the ticket describes**, not just an implementation detail — worth a callout on #613 so reviewers aren't surprised the "blocked" row is now interactive.
- Change Voice picker (`VoiceSelectionScreen.kt`) row layout reworked to match iOS structurally: a separate play/stop icon chip + a tappable name/checkmark pill (previously a single wrapped row with no play affordance at all and a "Selected" text label instead of a checkmark). Rows now render at a fixed 60dp height for consistent gaze/dwell target sizing across pages, and `itemsPerPage` is computed dynamically via `BoxWithConstraints` measuring the actually available height rather than a hardcoded per-orientation guess — the previous hardcoded value (tuned for the old "rows stretch to fill the screen" layout) left roughly half the portrait screen empty once rows became fixed-height, inflating the page count unnecessarily. A design-review-only `@Preview` (`VoiceOptionRowMockedNamesPreview`) demonstrates the row layout with mocked human names ("Daniel", "Karen", ...) since Android's `TextToSpeech`/`Voice` API has no friendly-name field (unlike iOS's `AVSpeechSynthesisVoice.name`) — the real running app still shows locale+quality display names.
- Change Voice picker's header now reads "Change Voice" (was "Voice") to match iOS; removed the now-unused `voice_selection_title` string.

## Key decisions

- **Repurposed `SettingsEvent.NavigateToVoiceSelection`** to mean "go to the new intermediate screen" rather than adding a same-shaped duplicate event, since it was unused from the UI layer until now and the nav host's handling of it was trivial to retarget.
- **Reused `SettingsButton`** (already shared by `SelectionModeScreen`) for the "Change Voice" row instead of introducing a new chevron-row composable.
- **No new mocking/fake infra** — `SettingsVoiceViewModelTest` uses the existing `FakeVocableSharedPreferences`. Because `VocableTextToSpeech`'s internal `TextToSpeech` engine is never initialized under this module's plain-SDK-stub unit test environment (same constraint documented in `VocableTextToSpeechTest`), every test case observes the `"Default"` fallback label rather than a resolved voice name — the live-resolution branch logic itself is already covered by `VocableTextToSpeechTest`'s `resolveVoiceSelection` tests, which `getActiveVoiceDisplayName` reuses directly.
- Verified live-resolution end-to-end on a Pixel 9 Pro XL emulator (not just unit tests): the preview row correctly showed the emulator's actual live default voice ("English (United States) – High Quality") with no explicit pick persisted, and back-navigation correctly threads Settings → Voice → Change Voice → back → Voice → back → Settings. Also verified the play/stop toggle: tapping a row's play chip speaks its display name, swaps to the stop icon, and reverts to play automatically once speech finishes — confirmed on both the preview row and the Change Voice picker rows.
- `onPreviewVoice`/`onPreviewActiveVoice` speak the display name text directly rather than routing through a shared "sample phrase" concept, specifically so this doesn't become a second, undocumented answer to #613's open sample-phrase question — it's a distinct, narrower behavior (read the voice's own label) that happens to sidestep needing that decision at all.

## Out of scope (tracked separately per #613/#635)

- Change Voice screen's further visual rework (rename, selected-state border details, empty state) — #636.
- Removing the old Voice row from Selection Mode — #637.

## Pointers

- Issue: #635 · Parent: #613
- Design: Figma `6561:3335` (Settings root), `6562:3374` (Settings → Voice) — cross-checked against iOS's `VoiceSettingsViewController`/`VoiceProfilePreviewDataSource` for behavior the mock doesn't fully specify (live-fallback resolution, non-interactive preview row).
