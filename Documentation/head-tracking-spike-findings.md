# Head-tracking tuning spike — presentation outline (#629 / #676)

Branch: `prototype/mediapipe-facelandmarker` (built on top of `prototype/mediapipe-facedetector`). Live doc — iterate here.

## 1. The question

iOS needs less head movement than Android to move the cursor, and feels smoother. #629 asked: is this an Android tuning gap, or a tracking-technology gap? Does Android need a tuning UI to match?

## 2. MediaPipe / Project GameFace — pros and cons

**Why we looked at it**: Google's own current reference app for this exact accessibility use case (Project GameFace) uses MediaPipe, not ARCore.

**Pros**:
- Richer per-frame data available (full 468-point face mesh, blendshapes, head-pose transformation matrix) vs. ARCore's single-region pose.
- No ARCore device-support gate — could reach devices ARCore can't.
- Free, open source, actively maintained by Google for this exact use case.

**Cons — what we found building and measuring it, not assuming it**:
- Real, measured throughput ceiling on our test device (Pixel 3a) **for FaceLandmarker specifically**: ~110–130ms inference latency, ~15fps effective update rate, vs. ARCore's near-native ~30fps.
- Tried the GPU delegate specifically to fix this — it silently fell back to CPU (confirmed via `TfLiteXNNPackDelegate` in logs), no improvement. Not a quick fix.
- Required significant custom calibration math to get usable output at all (axis sign correction, neutral-pose offset measured per-device) — the "richer data" isn't free just because a different library provides it.

**Note: MediaPipe is two different APIs, not one** — this cons list is FaceLandmarker's (the 468-point-mesh, richer-data path). FaceDetector (the lighter, single-stage, bare-nose-tip-keypoint model) doesn't hit the same throughput ceiling — see section 5.

**Verdict**: FaceLandmarker not recommended for this device class today — the throughput ceiling is hardware-bound, not something more tuning fixes. FaceDetector is a separate story (section 5).

## 3. The comparison toggles we built

The investigation kept conflating two different questions — *which signal drives the cursor* vs. *how that signal is smoothed*. We built two independent, live, no-rebuild debug toggles to separate them and A/B test any combination on-device:

- **Tracking source**: ARCore (nose-tip, shipped default) / ARCore (`centerPose`) / MediaPipe FaceLandmarker / MediaPipe FaceDetector
- **Smoothing algorithm**: Shipped lerp (real production baseline) / One Euro Filter / PID (iOS's actual shipped constants)

The smoothing toggle defaults to the real shipped behavior, so every comparison has an actual baseline, not a memory of "how it used to feel."

## 4. Why smoothing — not tracking source — turned out to be the bigger lever

- Shipped lerp's blend-fraction range (0.05–0.15) is a hard ceiling — confirmed directly, not assumed: raising the blend fraction to 0.7 on the *existing* ARCore signal removed the latency complaint entirely. No signal or library change needed to prove this.
- That fix alone trades latency for jitter — a single fixed fraction can't be both fast and noise-resistant at once.
- Tried a hard deadzone → fixed jitter, caused a new "jump when resting" artifact (drift accumulates while frozen, then snaps). Tried a soft/ramped deadzone → reduced but didn't eliminate flicker right at the threshold boundary.
- **One Euro Filter** (adaptive smoothing based on estimated *velocity*, not a fixed fraction or threshold): solved both problems at once — this is the structurally correct tool for "fast when moving, stable at rest" without a boundary to flicker across.
- **PID** (iOS's literal technique, tested with iOS's actual constants `Kp=3.307, Ki=0.365, Kd=0.690`) — first attempt was bouncy/unstable, but we confirmed the *cause* rather than assuming PID itself is wrong: the P/I terms alone are tame (`Kp·dt ≈ 0.11` at our ~33ms cadence, comparable to shipped Medium sensitivity) — the instability traced specifically to the derivative term amplifying sensor noise (a textbook PID pitfall: differentiating a noisy signal amplifies the noise by ~1/dt). Applied the standard fix — low-pass filter the derivative estimate before using it — **re-tested on-device and it now works well.** Confirmed, not just theorized.

## 5. Where tracking source landed

- MediaPipe FaceLandmarker → hardware ceiling on this device class, not recommended now.
- MediaPipe FaceDetector (lighter single-stage model, bare nose-tip keypoint, no head-pose transform) + One Euro Filter → **feels instant and smoother than FaceLandmarker/ARCore in on-device testing.** Lighter model likely sidesteps FaceLandmarker's throughput ceiling. Worth treating as a real candidate alongside ARCore, not just a "more hardware-friendly fallback" — needs the same latency/fps measurement pass FaceLandmarker got before relying on this further.
- ARCore `centerPose` → same performance as today's shipped `NOSE_TIP`, potentially richer signal (full head pose vs. one region) since ARCore already exposes it — worth a clean, isolated follow-up validation, but not required to ship the smoothing win below.

## 6. Decision — implement PID

Presented this spike; decision made to move forward with **PID**, not One Euro Filter, as the smoothing replacement.

**Reference implementation**: iOS's `PIDInterpolator` (`Vocable/HeadTracking/Interpolation/PIDInterpolator.swift`) wraps a vendored copy of [Pulse](https://github.com/cieslakdawid/Pulse) (`Vocable/HeadTracking/Interpolation/PulseController/Pulse.swift`) — not a package dependency, a vendored single file. Confirmed by reading the actual source, not just the constants:
- Standard P + I + D terms (`Kp=3.307, Ki=0.365, Kd=0.690`).
- **Integral damping**: `integral *= 0.9` applied every tick — decays accumulated integral error so it doesn't overshoot/oscillate, distinct from the derivative-noise fix we already made in our hand-rolled version.
- **Quiescence/deadband**: `minimumValueStep = 0.010` — once error, integral, and derivative are all below this, the controller freezes output and zeroes the integral rather than micro-jittering at rest.
- **dt handling**: driven by `CADisplayLink` (~frame rate), with a `MaxTimeDelayDuration = 0.05s` cap — larger gaps get chunked into multiple fixed-size steps rather than one big unstable step.

**Android path**: researched published third-party Android/Kotlin PID libraries (FTCLib, WPILib, MiniPID-Java, others) as an alternative to hand-rolling — none are a true drop-in match for Pulse's specific damping/quiescence behavior; the closest maintained option (FTCLib, Maven Central) is robotics-shaped and would need a wrapper anyway. **Decision: port Pulse's actual algorithm to Kotlin directly** (small, single-file, straightforward port) rather than adopting a robotics library — this gets an exact behavioral match with iOS (same damping factor, same quiescence threshold, same dt-chunking), not just the same three gain constants, which is what our first hand-rolled attempt was missing.

This supersedes our first hand-rolled PID (which fixed the derivative-noise bug but didn't implement integral damping or quiescence detection) — the port should replace it, not run alongside it.

## 7. Recommendation

- **Ship-track (decided)**: keep ARCore, replace the shipped lerp with a **Kotlin port of iOS's Pulse-based PID** (see section 6) — matches iOS's proven feel and tuning exactly, not just the same three constants. Low risk — doesn't change *what* movement drives the cursor, only how cleanly it's tracked, so no governance/caregiver-validation requirement.
- **Dark horse, needs more validation**: MediaPipe FaceDetector + One Euro Filter felt instant and smoother than everything else tested, including ARCore. Not dismissing MediaPipe outright anymore — FaceLandmarker hit a hardware ceiling, but the lighter FaceDetector model may not. Needs the same latency/fps measurement rigor before it can graduate from "felt great" to "confirmed," and it's still a signal-source change (new engine), so it carries the governance/validation requirement below.
- **Reject for now**: MediaPipe FaceLandmarker specifically (hardware ceiling on this device class) — FaceDetector is not tarred with the same brush.
- **Follow-up, not blocking**: ARCore `centerPose` as a possible signal-quality upgrade — separate ticket, own validation.
- **Governance note**: changes to *what signal* drives the cursor (nose-tip vs. `centerPose` vs. MediaPipe) need product/caregiver validation before shipping, since this app is already deployed with real AAC users who have muscle memory for the current behavior. **Smoothing-formula changes (PID/One Euro) do not carry that requirement** — same movement, same direction, just better-tracked, so this can proceed as a normal engineering improvement.

## Open items

- [ ] Port Pulse's algorithm (integral damping, quiescence, dt-chunking) to Kotlin and replace the current hand-rolled `PIDFilter.kt` — tracked in the new implementation issue (see below)
- [ ] Clean, isolated `centerPose` vs. `NOSE_TIP` re-test (separate from smoothing-algorithm changes)
- [ ] Measure FaceDetector + One Euro's actual latency/fps before treating it as more than a promising lead
- [ ] Re-test outlier-rejection prefilter in isolation (a camera hand-off race from switching the debug toggle live was possibly conflated with it earlier — toggle-switching itself is debug-only and never ships, not a product concern)
