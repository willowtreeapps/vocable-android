# Head-tracking tuning — comparable apps reference

Background research for #629 (Head-Tracking Tuning spike), moved here from the issue body since it's durable reference material for whichever outcome the spike lands on, not a live task list.

## Baseline context

There is no existing PID/tuning logic on Android at all — gaze smoothing today is a single hardcoded linear interpolation (`Vector3.lerp`) plus two hardcoded scaling constants in `FaceTrackingViewModel.kt`. iOS, by contrast, has a genuine PID controller. The underlying algorithms are not equivalent, independent of whether either platform has a tuning UI reachable in a shipped build (iOS's currently isn't).

## Comparable Android apps

iOS isn't the only relevant precedent. Two real, shipped Android apps solve this exact problem — front-camera/face-tracking cursor control for motor-impaired users — and neither uses a hidden debug menu:

- **[Eva Facial Mouse](https://github.com/cmauri/eva_facial_mouse)** (GPL, long-running, real-world used, same population as Vocable: ALS, cerebral palsy, spinal cord injury). Exposes five separate tuning parameters as ordinary end-user settings, no debug gating at all ([`preference_fragment.xml`](https://github.com/cmauri/eva_facial_mouse/blob/master/eviacam/src/main/res/xml/preference_fragment.xml)): Horizontal Speed, Vertical Speed (1-25, default 8, with a "lock speeds together" option), Acceleration (0-5, default 2, non-linear curve), Motion Smoothing (0-8, default 2 — a low-pass filter, `weight = log10(smoothness + 1)`, [`PointerControl.java:146`](https://github.com/cmauri/eva_facial_mouse/blob/master/eviacam/src/main/java/com/crea_si/eviacam/common/PointerControl.java#L146)), and Motion Threshold (0-5, default 1, a pixel deadzone). Also has a re-runnable first-run wizard step ([`SpeedSettingsWizardStep.java`](https://github.com/cmauri/eva_facial_mouse/blob/master/eviacam/src/main/java/com/crea_si/eviacam/wizard/SpeedSettingsWizardStep.java)) where the live cursor is active while adjusting speed.
- **[Google Project Gameface](https://github.com/google/project-gameface/tree/main/Android)** — Google's own current MediaPipe-based reference implementation for face/head-gesture control on Android. A dedicated `CursorSpeed` activity, reached from a normal button in `MainActivity` ([`MainActivity.java:88`](https://github.com/google/project-gameface/blob/main/Android/app/src/main/java/com/google/projectgameface/MainActivity.java#L88), no debug gate), shows 7 stock `SeekBar` widgets with live numeric labels: 4 independent directional speeds (Up/Down/Left/Right), Smooth Pointer, Smooth Blendshapes (gesture-detection smoothing, kept separate from cursor smoothing), and Hold Delay. Same raw-int-times-multiplier pattern Vocable already uses, persisted via plain `SharedPreferences` ([`CursorMovementConfig.java`](https://github.com/google/project-gameface/blob/main/Android/app/src/main/java/com/google/projectgameface/CursorMovementConfig.java)).
- **Google Look to Speak** (closed source, gaze-direction based, not cursor-based) — different mechanism, but same UX instinct: onboarding calibration for mount position/viewing angle, ongoing settings for gaze-hold duration and off-screen distance. Settings screen is touch-only, not gaze-operable — same as iOS's tuner.
- **Tobii Dynavox** — different product category (dedicated infrared eye-tracker hardware, not front-camera face tracking), weaker analogy. Guided multi-point calibration flow plus a persistent "Gaze Interaction Settings" screen. Closer in spirit to iOS's calibration model than to Eva/Gameface's slider-based approach.

**Takeaway:** the two real Android apps in Vocable's own category both treat this as a normal, always-visible, user-facing settings screen — never a hidden engineer/QA tool — because the "right" smoothing/speed genuinely varies per person and per mounting setup for this population. That's a materially different philosophy than iOS Vocable's (never-shipped) hidden debug menu, and arguably better-precedented for what Android should actually build. This is directly relevant to outcome (d) in #629's spike.

## Related

- #629 — Head-Tracking Tuning spike
