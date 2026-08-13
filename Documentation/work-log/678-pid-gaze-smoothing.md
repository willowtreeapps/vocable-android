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
  - Derivative is computed on the *followed value* (`pv - previousValue`), not on the raw
    error — this is why Pulse's own derivative term doesn't need a noise low-pass the way the
    spike's hand-rolled version (which differentiated `error`, i.e. raw signal noise) did. The
    followed value is already smooth by construction, so its derivative is too.
  - Per-tick integral damping (`integral *= 0.9`), applied unconditionally every step.
  - Quiescence/deadband: once `|error|`, `|integral|`, and `|derivative·dt|` are all under
    `minimumValueStep` (0.010, matching iOS), output freezes at the current value and the
    integral resets, instead of micro-jittering at rest.
  - dt capping/chunking at 0.05s (`MaxTimeDelayDuration`, matching iOS): a gap larger than that
    (app backgrounded, face briefly lost) runs as several fixed-size steps instead of one large,
    unstable one.
  - `kp`/`ki`/`kd` default to iOS's real production constants (3.307 / 0.365 / 0.690) — this PID
    is genuinely shipping on iOS today, only its tuning UI is unreachable in a shipped build.
  - `Vector3PIDFilter` applies an independent `PIDFilter` per axis, same shape as the spike's
    prototype wrapper.
- `FaceTrackingViewModel`: `onSceneUpdate()`'s tracking block now calls `pidFilter.filter(...)`
  instead of `Vector3.lerp(...)`. This is the only production code path changed — `Vector3.lerp`
  and `oldVector` are gone entirely, no debug toggle gates it, matching the ticket's requirement
  to ship this as the default behavior.
- Preserved one existing quirk on purpose rather than "fixing" it as a drive-by: the very first
  tracking sample of a session was, and still is, passed through unscaled (no `y *= 2F` tablet
  correction) before smoothing kicks in — `hasTrackedFirstVector` replaces the old
  `oldVector == null` check to gate that. `PIDFilter.filter()`'s own first-sample pass-through
  behavior means this stays a no-op for the smoothing algorithm itself.
- `app/src/test/java/com/willowtree/vocable/core/PIDFilterTest.kt` (new): first-sample
  pass-through, convergence to a held set point, integral damping bounding a sustained error,
  quiescence freezing output once within threshold, dt-chunking on a large time gap staying
  finite/bounded, and `reset()`/`Vector3PIDFilter` per-axis independence.

## Known gap — the Settings sensitivity control is now orphaned

`SensitivityViewModel`/the Settings sensitivity screen still read/write
`VocableSharedPreferences`'s sensitivity value, but nothing consumes it anymore —
`FaceTrackingViewModel` no longer reads `getSensitivity()` at all, since PID uses fixed gains
(matching iOS, which also doesn't expose these as a real user-facing setting). This ticket's
scope was the smoothing algorithm only, so the Settings screen itself was deliberately left
alone rather than guessing at a fix (e.g. mapping sensitivity to a PID gain) that iOS doesn't do
either. **Needs a product decision** — likely a follow-up ticket — on whether to remove/repurpose
that screen or leave it as a currently-inert setting.

## Known verification gap — needs on-device testing

This PID port was validated with unit tests on the algorithm in isolation (`PIDFilterTest`) and
a full `assembleDebug`/`assembleDebugAndroidTest`/`testDebugUnitTest` pass, but the ticket's last
acceptance criterion — an on-device feel/latency/jitter assessment confirming parity with iOS's
smoothing feel — could not be done in this session (no physical device/ARCore-capable emulator
available). The spike (#629) already confirmed these exact gains feel right on-device for the
hand-rolled prototype; this port is algorithmically closer to iOS (integral damping, quiescence,
dt-chunking added), so a regression is unlikely, but **someone with an ARCore-capable device
needs to confirm the on-device feel before this ships**, not just before merge.

## Pointers

- Issue: #678 · Parent (spike, not an integration branch): #629
- Branch: `feature/678/pid-gaze-smoothing` off `main` (this is a normal production change, not a
  sub-issue of an in-progress parent feature branch — #629 is a completed research spike, its
  scope explicitly excluded implementation)
- Superseded: the hand-rolled `PIDFilter.kt` on `prototype/mediapipe-facelandmarker` (a separate,
  uncommitted exploration branch not touched by this change)
