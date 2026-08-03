# Reset App Settings — iOS reference behavior

Background research for #627 (Reset App Settings — Android), moved here from the issue body since it's durable reference material, not a live task list.

## Confirmed iOS reference behavior (`AppResetController.performReset()`)

An engineer inspected the shipped iOS implementation directly. Reset is a **blanket wipe-and-reseed**, not a curated restore:

1. **Wipes every key in `UserDefaults` unconditionally** — not a curated list, the whole store. Every preference falls back to its Swift-level default the next time it's read: head tracking (device-dependent), dwell duration (1s), cursor sensitivity (medium), compact QWERTY keyboard (off), Listening Mode/smart-assist/hot-word toggles (unset/default).
2. **Deletes all Core Data `Phrase` and `Category` rows**, then re-runs the preset migration from bundled `presets.json` for the current language — all user-added phrases/categories and any edits to preset phrases (renames, hidden state, etc.) are permanently deleted. The app ends up in a fresh-install state.
3. **Voice selection is an accidental side effect, not deliberate behavior:** `selectedVoiceIdentifier` simply becomes `nil` because it's stored in `UserDefaults` like everything else — there is no explicit "restore to device-captured voice" logic. Whatever the voice-selection code does when the identifier is unset is a fallback nobody designed on purpose.

## Why this matters for Android's version

The original assumption was that reset would deliberately "restore to the device voice." That's not what iOS does — it's an unset value that happens to fall back to something. Android should not silently inherit that as intentional design.

**Open decision (PM):** should Android's reset (a) mirror iOS's accidental nil-fallback for true behavioral parity, or (b) deliberately restore to a defined default voice — arguably better UX, but a deliberate platform divergence from iOS? This decision depends on the voice-selection feature (#613) shipping first, since it determines what "the voice preference" even looks like on Android.

## Related

- #627 — Reset App Settings epic
- #613 — Voice Selection (sequencing dependency)
- #360 — Convert Preset to Custom Categories (why reset isn't a pure settings-values reset)
