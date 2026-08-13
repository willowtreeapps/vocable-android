# Gaze cursor smoothing: PID controller ported from iOS's Pulse library

**Issue:** #678 (Part of #629, the head-tracking tuning spike)

## What was needed

`FaceTrackingViewModel.onSceneUpdate()` smoothed the ARCore nose-tip signal with a single
fixed-fraction `Vector3.lerp(oldVector, target, sensitivity)` (blend 0.05–0.15, tied to the
Settings sensitivity screen). The #629 spike confirmed this is a structural ceiling, not a
tuning gap: a fixed blend fraction can't be both fast (low lag) and stable (low jitter) at the
same time. iOS avoids this with a real PID controller — a vendored copy of
[Pulse](https://github.com/cieslakdawid/Pulse) (`Vocable/HeadTracking/Interpolation/PulseController/Pulse.swift`),
wrapped by `PIDInterpolator.swift` and driven from `HeadGazeTrackingInterpolator.swift`. The
spike's own hand-rolled prototype (`PIDFilter.kt` on `prototype/mediapipe-facelandmarker`) got
the three P/I/D gain constants and a derivative low-pass fix right, but never implemented
Pulse's integral damping or quiescence detection.

## What changed

- `core/PIDFilter.kt` (new): a from-scratch Kotlin port of Pulse's actual `calculateOutput`/
  `tick` logic, read directly from the vendored iOS source (not just its constants):
  - Derivative computed on the *followed value* (`pv - previousValue`), not on raw error — this
    is why Pulse's D-term doesn't need the noise low-pass the spike's hand-rolled version did.
    Note the faithful-to-Pulse sign quirk documented in the class KDoc: the D-term is
    `+Kd·d(pv)/dt` — velocity *momentum*, not the textbook damping term. It's why the cursor
    glides into its target rather than braking hard; don't "fix" the sign without expecting the
    feel to diverge from iOS.
  - Per-tick integral damping (`integral *= 0.9`), matching Pulse.
  - Quiescence/deadband (`minimumValueStep = 0.010`, iOS's literal value — see tuning history
    below), with dt capping/chunking at 0.05s (`MaxTimeDelayDuration`, matching iOS), plus a
    1s total catch-up cap so an arbitrarily long gap can't run an arbitrary number of chunks.
  - `kp`/`ki`/`kd` are iOS's real production constants (3.307 / 0.365 / 0.690).
- **Additions beyond Pulse, each fixing an on-device-confirmed failure mode Pulse's design
  didn't cover at ARCore's noise level** (ARCore's raw signal is noisier than what iOS's Pulse
  operates on; each was diagnosed from captured raw-vs-smoothed logcat data, not feel alone):
  - *Wake hysteresis* (`wakeThresholdMultiplier = 2`): a single shared freeze/wake threshold
    flickered in/out of quiescence right at the boundary (visible jitter just before rest).
  - *Wake confirmation* (`wakeConfirmationTicks = 3`, counted at most once per sample so
    dt-chunking after a frame hitch can't satisfy it with one noisy sample): single-tick noise
    spikes used to run a full PID episode and re-freeze slightly displaced — accumulating into
    visible at-rest drift ("the dot moves while my head is still").
  - *Leaky freeze* (`quiescentCatchUpRate = 3/s`): a hard freeze parked up to
    `minimumValueStep` of residual error below the wake threshold; when the user's head kept
    settling, that residual eventually crossed the wake threshold and corrected all at once —
    a visible pause-then-snap at the end of every movement. The leak absorbs it gradually
    (~1/3s time constant); zero-mean noise still averages out to sub-pixel wobble.
- `FaceTrackingViewModel` rework:
  - Smoothing runs on a `Choreographer` vsync frame callback (Android's `CADisplayLink`
    equivalent) toward the latest raw target, not once per ARCore frame — ARCore delivers
    ~30fps, under display refresh, and iOS's Pulse ticks on `CADisplayLink` for exactly this
    reason. A `delay()`-loop version was tried first and rejected: its timing jitter feeds
    straight into the dt-dividing integral/derivative terms (confirmed on-device as
    overshoot/bounce before settling).
  - The `!isTablet` `y *= 2` reachability scaling moved from *before* smoothing to *after* —
    pre-scaling doubled y's noise floor relative to x's, which made y (and only y) drift at
    rest (confirmed from logged raw data: y's stationary noise band was ~2× x's).
  - Filter resets on face-tracking loss, matching iOS's `needsResetOnNextUpdate` — otherwise
    the cursor swoops in from its stale position with garbage integral history on re-acquire.
  - Frozen-output ticks skip StateFlow emission (sceneview's `Vector3` has no `equals`, so
    every 60Hz tick otherwise emits a distinct-but-identical object and recomposes the cursor
    at rest; Pulse pauses its display link at quiescence for the same reason).
  - Replaces `Vector3.lerp` and `oldVector` entirely; no debug toggle, per the ticket's AC.
- **Sensitivity setting re-wired to iOS semantics.** The old lerp consumed the stored
  sensitivity (0.05/0.10/0.15) as its blend fraction, so replacing it orphaned the Settings
  control. On iOS, sensitivity is *not* a smoothing knob: `CursorSensitivity.swift` maps
  Low/Medium/High to screen-mapping scale ranges (midpoints 3.0/4.0/5.25) and the PID constants
  never change. Android now does the same: the stored value maps to a cursor-travel amplitude
  multiplier (0.75× / 1.0× / 1.3×, mirroring iOS's ratios) applied in `convertCoordSystems`,
  after smoothing. **Note for PR/product: this silently changes what existing users' saved
  setting does** — "High" used to mean less smoothing (snappier, jitterier); it now means more
  cursor travel per head movement. Same stored value, new (iOS-parity) behavior.
- `app/src/test/java/com/willowtree/vocable/core/PIDFilterTest.kt` (new, 13 tests): first-sample
  pass-through, convergence, integral damping, quiescence freeze, hysteresis, single-tick-spike
  rejection, frame-hitch chunking rejection, sustained-wake acceptance, gradual residual
  absorption, dt-chunking stability, reset, per-axis independence.

## Tuning history worth keeping (so it isn't re-litigated)

- `minimumValueStep` was initially rescaled down from iOS's 0.010 on the theory that iOS
  operates in screen points (hundreds) while we operate on ARCore zAxis components (±0.1–0.3).
  Testing seemed to confirm it ("0.010 feels laggy") — but that verdict was contaminated by the
  two then-unfixed bugs (delay-loop dt jitter, pre-filter y scaling). With those fixed, iOS's
  literal 0.010 works at our measured (~0.013 peak-to-peak per axis) noise floor and is what
  ships. Don't rescale it again without re-measuring.
- **The "swoop" investigation, and why tracking is now position-based, not orientation-based.**
  On-device, the cursor swooped vertically during horizontal head turns (absent on iOS). Fixes
  tried against the ORIENTATION signal, in order, all failed: `centerPose` instead of the
  nose-region pose (no change), yaw/pitch angle decomposition via `atan2`/`asin` to cancel the
  `sin(yaw)·cos(pitch)` component coupling (no change), yaw-velocity-gated pitch trust (no
  change), and finally a faithful port of iOS's actual projection math — camera-relative
  ray-plane intersection from `HeadGazeTrackingInterpolator.swift`, which Android had never
  ported (reduced but did not eliminate it). Conclusion: the artifact lives in ARCore's face
  *orientation estimate itself* — a mesh fit to flat RGB bends under yaw in a way iPhone's
  TrueDepth-sensed orientation doesn't. Directly *observed positions* don't have the artifact:
  MediaPipe FaceDetector's image-space landmark position showed zero swoop and "perfect"
  horizontal feel in the engine comparison. The shipped path now uses that same signal shape
  from ARCore: the nose-tip's *position* in the camera's display-oriented frame, depth-
  normalized (distance-invariant), relative to a neutral averaged over the first ~0.7s of
  tracking (a single-first-frame neutral rested visibly off-center). All experiments preserved
  on `experiment/678-swoop-investigation`.
- **Known characteristics of position tracking, flagged for product before ship:** (a) moving
  or tilting the *device* moves the cursor — inherent to camera-relative tracking with no
  world tracking in ARCore front-camera sessions, and iOS behaves the same way (ARKit face
  config defaults to no world tracking); mounted-device usage makes this a non-issue in
  practice, and breaking tracking for ~1s recalibrates the neutral. (b) Users with very
  limited neck rotation get less signal than orientation-based tracking gave them (the nose
  travels on a lever arm) — the accessibility population question product should weigh in on.

## Debug-only tracking-engine comparison toggle

To complete the ticket's on-device assessment AC - and to answer "which tracking source best
feeds this PID" with a live A/B instead of separate builds - the branch also adds an engine
selector (ARCore / MediaPipe FaceDetector / MediaPipe FaceLandmarker) to the Timing &
Sensitivity screen, gated on `BuildConfig.DEBUG`. All three engines feed the same PID pipeline,
so the comparison isolates exactly one variable: the tracking source. Note #678's Out-of-Scope
originally listed MediaPipe FaceDetector; it's pulled in here strictly as evaluation tooling,
not shipped behavior - whether the menu stays long-term is a question for product once the
engine decision is made.

**The isolation is artifact-level, not just a runtime flag.** The MediaPipe/CameraX libraries
are `debugImplementation`; the trackers, comparison screens, and model assets (~230KB
FaceDetector `.tflite`, ~3.7MB FaceLandmarker `.task`) live in `app/src/debug/`; MainActivity
bridges to the debug screens via a source-set-split composable
(`DebugEngineTrackingScreen` - real host in `src/debug`, empty stub in `src/release`); and
`FaceTrackingViewModel` takes engine input through an engine-agnostic
`onDebugEngineUpdate(x, y)` so no MediaPipe type appears in main source. Verified: release
compiles with **zero** MediaPipe/CameraX entries on `releaseRuntimeClasspath` (13 in debug).

Mechanics worth knowing:
- Engine choice persists via a debug-only pref (`KEY_DEBUG_TRACKING_ENGINE`); switching resets
  the PID filter and raw target (engines' signals aren't in the same coordinate space - carrying
  filter history across a switch would swoop the cursor between unrelated positions).
- Each engine's calibration/remap constants live with its adapter in the debug source set, with
  y-amplitudes halved vs. the #629 spike's values because the PID tick loop now applies the
  phone `y * 2` reachability scaling to every engine's output (the spike's paths never had it).
- `onSceneUpdate` ignores frames when a non-ARCore engine is selected (and vice versa), so a
  tracker being torn down mid-switch can't fight the new engine for the cursor.
- The spike's known caveats still apply: FaceLandmarker hit a ~15fps hardware ceiling on Pixel
  3a-class devices, and live engine switching exercises a camera hand-off (ARCore/SceneView vs
  CameraX both want the front camera) that had a suspected race in the spike.

## Known verification gap

Validated with unit tests, full `testDebugUnitTest`/`assembleDebug`/`assembleDebugAndroidTest`,
and extensive on-device iteration on a Pixel 3a (feel/latency/jitter assessed against iOS
side-by-side through multiple tuning rounds — responsive, stable at rest, smooth settling).
Not yet validated on a tablet (`is_tablet` path skips the y×2 scaling) or on other phone
form factors.

## Pointers

- Issue: #678 · Parent (spike, not an integration branch): #629
- Branch: `feature/678/pid-gaze-smoothing` off `main`
- Supersedes: the hand-rolled `PIDFilter.kt` on `prototype/mediapipe-facelandmarker`
- Comparison branch (FaceDetector signal source + this PID): see `prototype/pid-facedetector`
