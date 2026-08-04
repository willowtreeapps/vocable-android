# Remove the old Voice entry point from Selection Mode

**Issue:** #637 (Part of #613)

## What was needed

Voice moved to Settings root (#635: Settings → Voice → Change Voice), but `SelectionModeScreen` still had its own live Voice row (`SettingsButton` + `onVoiceSelection`), and `SelectionModeViewModel` still maintained `selectedVoiceLabel` state to feed it. Left in place, that meant two reachable entry points to the same picker. This mirrors the Language-row removal already done on this same screen in #630/#631.

## What changed

- `SelectionModeScreen.kt`: removed the Voice `SettingsButton` row, its `voiceButtonRef` `ConstraintLayout` anchor, the `onVoiceSelection` callback param (on both `SelectionModeScreen` and `SelectionModeContent`), and the `selectedVoiceLabel` state collection. The `ON_RESUME` `DisposableEffect` that only existed to call `refreshLabels()` was removed entirely — `headTrackingEnabled` is already reactive via `asFlow()`/`collectAsStateWithLifecycle`, so nothing else needed it.
- `SelectionModeViewModel.kt`: removed `selectedVoiceLabel`/`refreshLabels()`/`DEFAULT_VOICE_LABEL`. This was the *only* thing `IVocableSharedPreferences` was used for in this ViewModel, so the constructor param was dropped too rather than left unused.
- `AppKoinModule.kt`: updated the `SelectionModeViewModel` factory to match the one-arg constructor.
- `VocableNavHost.kt`: removed the `onVoiceSelection = { navController.navigate(ROUTE_VOICE_SELECTION) }` wiring on `SelectionModeScreen`'s composable. `ROUTE_VOICE_SELECTION` itself stays — Settings → Voice still navigates there per #635.
- `strings.xml`: removed `settings_voice` (`"Voice\n%1$s"`), which had no other callers.
- Updated `SelectionModeViewModelTest.kt` for the one-arg constructor; no other test coverage referenced the removed state, so nothing else needed changing.

No layout-gap fix was needed for the "renders correctly with just Head Tracking" AC — Head Tracking was already the row directly above Voice with nothing anchored below Voice, so removing the last row in the `ConstraintLayout` left no dangling constraints.

## Pointers

- Issue: #637 · Parent: #613
- Precedent: #630/#631 (Language row removal from this same screen)
- Related: #635 (Settings root Voice row / Settings → Voice, the replacement entry point)
