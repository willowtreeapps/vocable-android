# Hide undownloaded/unavailable voices from the Change Voice picker

**Issue:** #618 (Part of #613)

## What was needed

The Change Voice picker listed voices the device didn't actually have installed, giving them a download-arrow affordance that deep-linked to the OS's TTS voice-data installer (`ACTION_INSTALL_TTS_DATA`). Per iOS parity, undownloaded voices should not appear at all.

iOS confirms the intent: `AVSpeechSynthesisVoice.speechVoices()` only ever reports installed voices, and `VoicePickerViewController` has no download affordance anywhere — the concept of "a voice you could get but don't have" simply doesn't exist in its picker. Android's `TextToSpeech.getVoices()` does surface those, hence the divergence.

## What changed

- **`getAvailableVoices()` now filters through the same predicate as the speak path.** It previously ran its own inline filter checking only language match and `isNetworkConnectionRequired`, while `applySelectedVoice()`/`getActiveVoiceDisplayName()` filtered on the stricter `isVoiceSupportedForLocale() && isVoiceDownloaded()`. Both now call a single `isVoiceUsable()`.
- **Extracted a pure, testable `isVoiceSupportedForLocale(voiceLanguage, targetLanguage, isNetworkConnectionRequired, languageAvailability)`**, following the same convention already used for `resolveVoiceSelection()` and `isVoiceDownloaded(features)` in this file. Folded away the old `isVoiceUnavailable()` helper.
- **Removed the download-icon treatment from `VoiceOptionRow`** and the `if (voice.isDownloaded)` fork in its `onClick`, plus the now-unused `onDownloadVoice` screen parameter, its NavHost wiring, and `R.string.voice_download`.
- **Clamped `pageIndex` when the page count shrinks** in `VoiceSelectionScreen`.
- **11 new unit tests** on the extracted predicate.

## Key decisions

**The download plumbing was kept, deliberately.** The ticket's AC called for removing `onDownloadVoice()`/`ACTION_INSTALL_TTS_DATA` outright, but the call on the ticket thread was to hide the undownloaded voices "for now" without tearing out the logic. So `VoiceSelectionViewModel.onDownloadVoice()`, `VoiceSelectionEvent.LaunchTtsSettings`, the NavHost intent, `VoiceOption.isDownloaded`, and the existing ViewModel test all remain — the path is simply unreachable from the UI now. Both `onDownloadVoice()` and the `isDownloaded` field carry KDoc saying so, because otherwise they read as deletable dead code to the next person (or the next `/simplify` pass).

**Why route the picker through the speak path's predicate rather than just adding an `isDownloaded` filter.** The narrow fix would have been one extra `.filter { it.isDownloaded }`. Using the shared predicate also closes a latent divergence: the picker could list a voice that `applySelectedVoice()` would immediately classify as `STALE_FALLBACK_TO_LIVE_DEFAULT`, i.e. a user could pick a voice and silently get a different one. The `isLanguageAvailable()` cross-reference matters for the same reason the ticket asked for it — `KEY_FEATURE_NOT_INSTALLED` is inconsistently populated across OEM engines, so `LANG_MISSING_DATA` has to disqualify a voice on its own.

**`isVoiceSupportedForLocale` and `isVoiceUsable` are kept as two predicates, not merged.** `applyLiveDefaultVoice()` intentionally checks locale support *without* the download check — it applies whatever the engine reports as its own default rather than second-guessing the engine's install state. Merging them would have changed that fallback's behavior, which is outside this ticket.

**Network-required voices stay excluded unconditionally,** not just while offline. The AC says "treated as unavailable offline," but Vocable is offline/local-first, and a voice that can drop out mid-conversation isn't something to hand a user who depends on it to speak. This was already the pre-existing behavior; it's unchanged.

**`languageAvailability` is passed as a lambda, and checked last.** Routing the picker through the speak path's predicate means `isLanguageAvailable()` — a synchronous binder call into the TTS service — now runs per entry in `getVoices()`, which is several hundred voices on Google TTS, on the main thread for every `speak()`. Reordering `isVoiceUsable()` to check the local install flag first and deferring the availability call behind a `() -> Int` keeps the IPC to only the voices that already match the target language and are local. Three tests count invocations so a refactor back to an eager `Int` parameter — which Kotlin would evaluate for every candidate — fails loudly instead of silently reintroducing N IPCs per call.

**The pagination clamp.** `onRefreshVoices()` re-reads installed voices on every `ON_RESUME`, so uninstalling a voice in system Settings and returning to Vocable shrinks the list. `currentPageItems` uses `getOrElse { emptyList() }`, so a stale out-of-range `pageIndex` rendered a blank page. Pre-existing, but hiding undownloaded voices makes the list shrink more often, so it's fixed here rather than left to be rediscovered.

## Testing

`./gradlew testDebug` — green, 75 tests overall, 19 in `VocableTextToSpeechTest` (11 new). `./gradlew assembleDebug assembleDebugAndroidTest` — green.

Unit coverage is on the extracted pure predicate only. The voice-list side of `VoiceSelectionViewModel` still can't be asserted against real data: `VocableTextToSpeech` is a global object wrapping the real Android engine, so under JVM tests it never initializes and `getAvailableVoices()` returns empty. That limitation is already documented in `VoiceSelectionViewModelTest`'s header comment; closing it would need an injected seam on the ViewModel.

**AC5 ("filter behavior confirmed on 2+ engines/OEMs") is not met.** #634, the spike that AC defers to, closed with second-engine/OEM validation *explicitly deferred* — its own finding was that no Google-engine divergence broke the fallback logic, so per that ticket's rules there was nothing to file, and multi-engine testing was left to "revisit only if a future report suggests engine-specific breakage." The `isLanguageAvailable()` cross-reference is in place precisely because per-engine `KEY_FEATURE_NOT_INSTALLED` behavior is untrusted, but that reasoning has not been confirmed against a non-Google engine on real hardware.

Also worth noting: #634's comment cites `.claude/skills/tts-voice-validation/SKILL.md` as the durable record of the `getDefaultVoice()` observability limit, but that file was never committed on any branch — so that finding currently lives only in the GitHub comment.

## Pointers

- Issue: #618 · Parent: #613
- Related: #634 (cross-engine validation spike), #636 (visual rework), #642/#645 (`isVoiceDownloaded`, reused here)
- iOS reference: `Vocable/Features/Settings/VoiceSettings/VoiceProfilePreviewDataSource.swift` + `…+Filter.swift`
