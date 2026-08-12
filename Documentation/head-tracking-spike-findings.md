# Head-tracking tuning spike — findings (#629 / #676)

Draft outline for the decision discussion. Branch: `prototype/mediapipe-facelandmarker` (built on top of `prototype/mediapipe-facedetector`).

## Original question

iOS needs less head movement than Android to move the cursor across the screen, and Android has felt less smooth. #629 asked whether that's an Android tuning gap or a tracking-technology gap, and whether Android needs its own tuning UI to match.

## What we tried, in order

1. **MediaPipe FaceDetector** (bare 2D nose-tip keypoint) — built, calibrated, worked, but the signal itself (2D position) is structurally weaker for detecting head *rotation* than iOS's pose-based approach. Kept on `prototype/mediapipe-facedetector` as a demoable reference, not pursued further.
2. **MediaPipe FaceLandmarker** (full head-pose transformation matrix → yaw/pitch via atan2) — richer signal, same category as iOS's ARKit pose. Got it working and calibrated (axis signs, neutral-pose offset).
3. **ARCore `centerPose`** vs the existing `NOSE_TIP` region pose — added as a same-library, same-performance alternative once we realized ARCore already exposes a full head-pose (`getCenterPose()`), not just a narrow region pose. Cheap to test (no model/library change).
4. **Smoothing/latency investigation** — separate from all of the above; affects every tracking source equally.

## Key findings

- **MediaPipe FaceLandmarker has a real, measured throughput ceiling on this test device (Pixel 3a).** ~110-130ms inference latency, ~15fps effective update rate, vs. ARCore's near-native ~30fps. GPU delegate was tried and silently fell back to CPU (confirmed via `TfLiteXNNPackDelegate` in logs) — no improvement. This is hardware-bound, not fixable by better math.
- **The "needs less head movement" complaint is mostly a *smoothing formula* limitation, not a tracking-signal-richness problem.** Android's shipped sensitivity range (0.05–0.15, a lerp blend fraction) is a hard ceiling far below what iOS's PID controller (`Kp=3.307`) achieves. Confirmed by direct experiment: raising the blend fraction to 0.7 removed the perceived latency entirely, on the *existing* ARCore nose-tip signal — no library or signal change needed.
- **That fix trades latency for jitter** (single blend-fraction lerp can't decouple "catch up fast" from "reject noise"). We replaced the ad hoc lerp/deadzone stack with a proper **One Euro Filter** (adaptive cutoff based on estimated velocity) — structurally the right tool for this exact tradeoff, unlike a fixed fraction or a hard/soft deadzone (both of which introduced their own artifacts: jump-on-rest, boundary flicker).
- **Remaining smoothing artifact**: an occasional single-frame "jump up and back" — diagnosed as a raw-signal outlier (glitched frame) that a velocity-adaptive filter briefly lets through. An outlier-rejection prefilter was built and tested but reverted mid-session (a camera-handoff race condition from switching debug-toggle modes was initially misattributed to it — worth re-testing in isolation, without switching tracking modes mid-session, before ruling it out).
- **ARCore `centerPose` vs `NOSE_TIP`**: tested via the live 3-way debug toggle; not yet conclusively evaluated in isolation from the smoothing changes happening in parallel. Needs a clean re-test now that the One Euro Filter baseline exists.
- **Debug toggle caveat**: switching between MediaPipe and ARCore modes live can trigger a camera hand-off race (both camera stacks briefly contend for the front camera). Not a product bug — the toggle is debug-only and never ships — but worth knowing when demoing.

## Constraint that reframes the whole recommendation

This app is already deployed in care centers with real AAC users. Any change to the *type* of signal driving the cursor (position vs. orientation) or a default sensitivity/smoothing change is a **behavioral change for an existing, vulnerable user base with established muscle memory** — not a pure engineering call. This should go through user/caregiver validation before shipping as a default, regardless of which technical option wins.

## Open items before this is decision-ready

- [ ] Re-test ARCore `centerPose` vs `NOSE_TIP` cleanly against the One Euro Filter baseline (isolate from other changes)
- [ ] Re-test outlier rejection in isolation (single tracking mode, no toggle-switching) to confirm whether it actually caused the camera issue or was coincidental
- [ ] Tune final One Euro Filter constants (`minCutoff`, `beta`) to a settled, agreed-upon feel
- [ ] Decide: is smoothing-formula improvement (One Euro Filter) something we'd propose for the *existing* ARCore path regardless of the MediaPipe question — this seems separable and lower-risk than any signal-source change

## Candidate recommendation directions (not yet final)

1. **Smoothing formula**: propose replacing the shipped lerp with a One Euro Filter (or similar adaptive filter) for ARCore — independent of the MediaPipe question, addresses the latency complaint directly, low risk since it doesn't change *what* signal drives the cursor, only how it's smoothed.
2. **Signal source**: MediaPipe FaceLandmarker is not recommended for this device class given the measured throughput ceiling. ARCore `centerPose` is worth a clean follow-up test as a lower-risk alternative to `NOSE_TIP` — same performance, potentially richer signal.
3. **Any shipped behavior change** (smoothing formula or signal source) needs explicit product/accessibility sign-off and real user validation before defaulting — not an engineering-only decision, per the care-center constraint above.
