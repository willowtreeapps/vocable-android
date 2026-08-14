package com.willowtree.vocable.core

import kotlin.math.abs

/**
 * Kotlin port of iOS's shipped cursor smoothing (Vocable-ios
 * `HeadGazeTrackingInterpolator.swift` / `PIDControlledTrackingInterpolator`, via the vendored
 * [Pulse](https://github.com/cieslakdawid/Pulse) library) - NOT the same technique as
 * [OneEuroFilter]. A PID controller reacts to *error* (distance between the current followed
 * value and the latest raw target) using three terms - proportional (react to the gap now),
 * integral (correct persistent bias), derivative (damp overshoot) - and treats the combined
 * output as a driving force/velocity that moves the followed value toward the target over time,
 * rather than filtering the raw signal directly based on its own velocity the way OneEuroFilter
 * does.
 *
 * This is a full port of Pulse's `calculateOutput`/`tick`, not just its three gain constants:
 * - Per-tick integral damping ([integralDamper]) so accumulated integral error decays instead of
 *   overshooting/oscillating. Note the damping is per TICK, not per second, faithful to Pulse:
 *   at a higher display refresh rate the integral decays proportionally faster in wall-clock
 *   terms. iOS has the identical property on ProMotion displays, so parity argues for leaving
 *   it - but all Android tuning to date was on a 60Hz Pixel 3a; re-verify feel on a
 *   high-refresh-rate device before changing anything here.
 * - Quiescence/deadband detection ([minimumValueStep]): once error, integral, and the per-tick
 *   derivative delta are all below this threshold, output freezes at the current value and the
 *   integral resets, instead of micro-jittering at rest.
 * - dt capping/chunking ([maxTimeDelayDurationSeconds]): a gap larger than this (e.g. the app
 *   was backgrounded, or a face was briefly lost) is processed as multiple fixed-size steps
 *   rather than one large, unstable step.
 *
 * [kp]/[ki]/[kd] default to iOS's actual production constants (`PIDInterpolator.swift`) - that
 * PID is genuinely live in shipped iOS builds today (only its *tuning UI* is unreachable), so
 * these aren't a guess.
 *
 * Note one faithful-to-Pulse quirk: the derivative term is `+Kd * d(pv)/dt` - the followed
 * value's own velocity with a POSITIVE sign, i.e. momentum, not the textbook damping term
 * (derivative-of-error, which would be the negative of this when the target is stationary).
 * This is why the cursor glides slightly past/into its target and settles rather than braking
 * hard - it's the same motion character iOS ships, so don't "fix" the sign without expecting
 * the feel to diverge from iOS. Pinned by the momentum test in PIDFilterTest.
 *
 * [minimumValueStep] was initially assumed to need rescaling from iOS's literal `0.010`, since
 * iOS's Pulse runs on on-screen point values (hundreds of points wide/tall) where `0.010` is a
 * negligible sub-pixel dead zone, while this filter runs on ARCore's raw `zAxis` vector
 * components (usable range roughly `±0.1`-`0.3`, not hundreds). On-device testing initially
 * seemed to confirm this - `0.010` felt laggy - but that turned out to be two other bugs
 * masquerading as a threshold problem: an imprecisely-timed tick loop injecting noise into the
 * integral/derivative terms (fixed by ticking on `Choreographer`'s vsync callback instead of an
 * approximated `delay()` loop), and `FaceTrackingViewModel` doubling the y-axis signal's noise
 * floor by scaling it *before* smoothing instead of after. With both fixed, iOS's literal
 * `0.010` performs correctly at this filter's actual (now-verified, ~0.013 peak-to-peak)
 * per-axis noise floor - keep this in sync with iOS rather than rescaled, unless on-device
 * testing says otherwise again.
 *
 * Quiescence uses hysteresis ([wakeThresholdMultiplier]), not a single shared threshold: sensor
 * noise sitting right at [minimumValueStep] would otherwise cross it back and forth on its own,
 * repeatedly freezing/unfreezing output right before it actually settles (visible as a jitter at
 * rest, confirmed on-device) - a single-threshold flicker the #629 spike already ran into with a
 * hard deadzone on the lerp-based prototype. Once frozen, [error] has to clear a wider threshold
 * ([minimumValueStep] * [wakeThresholdMultiplier]) to wake the filter back up than it took to
 * freeze it, so noise oscillating near the freeze threshold doesn't also cross the wake one.
 *
 * Rest is a "leaky" freeze, not a hard one ([quiescentCatchUpRate]): while quiescent, a small
 * fraction of the remaining error is still absorbed each tick instead of output holding perfectly
 * rigid. A hard freeze leaves whatever residual error existed at freeze time (up to
 * [minimumValueStep]) parked below the wake threshold - if the user's head then settles a little
 * further, that residual grows until it finally crosses the wake threshold and gets corrected all
 * at once, which reads on-screen as pause-then-snap right at the end of a movement (confirmed
 * on-device). With the leak, zero-mean sensor noise still averages out to sub-pixel wobble (the
 * leak acts as a heavy low-pass), but a genuine settling residual is gently absorbed within a few
 * hundred ms, so no deferred correction is left to snap later.
 *
 * Waking also requires the wake threshold to be cleared for [wakeConfirmationTicks] consecutive
 * DISTINCT SAMPLES, not just one - and not merely consecutive filter() calls. A single noisy
 * sample that pokes past the wake threshold used to be treated as a real, intentional movement
 * immediately - the filter would run a full active PID episode on that one sample, settle at
 * wherever the (possibly short, noise-driven) excursion ended, and re-freeze there. Repeated
 * over many such pokes, this let net position drift accumulate at rest without ever looking
 * like a single big jump (confirmed on-device: at-rest drift traced back to exactly this
 * pattern, distinct from the flicker [wakeThresholdMultiplier] alone fixes). Distinct samples
 * matter because the caller ticks at display refresh while the camera samples slower: the same
 * raw sample is re-presented across ~2 vsync ticks at 60Hz/30fps and ~4 at 120Hz/30fps, so
 * counting *calls* would let one noisy sample satisfy the whole confirmation on a high-refresh
 * display - the exact failure mode this exists to prevent. Callers signal freshness via
 * [filter]'s `isNewSample` parameter.
 */
class PIDFilter(
    private val kp: Float = 3.307f,
    private val ki: Float = 0.365f,
    private val kd: Float = 0.690f,
    private val minimumValueStep: Float = 0.010f,
    private val wakeThresholdMultiplier: Float = 2f,
    private val wakeConfirmationTicks: Int = 3,
    // Fraction of remaining error absorbed per SECOND while quiescent (dt-scaled per tick, so
    // behavior is refresh-rate independent). 3.0 ~= a third of a second to absorb ~63% of a
    // settling residual.
    private val quiescentCatchUpRate: Float = 3f,
    private val integralDamper: Float = 0.9f,
    private val maxTimeDelayDurationSeconds: Float = 0.05f,
) {
    private companion object {
        private const val MAX_CATCH_UP_SECONDS = 1f
        private const val NANOS_PER_SECOND = 1_000_000_000f
    }

    private var value: Float? = null
    private var integral = 0f
    private var previousValue = 0f
    private var lastTimeNanos: Long? = null
    private var isQuiescent = false
    private var consecutiveWakeTicks = 0
    private var wakeCountedThisCall = false

    /**
     * Advances the filter to [timestampNanos] and returns the smoothed value.
     *
     * Timestamps are nanoseconds (e.g. `Choreographer`'s `frameTimeNanos`) rather than
     * milliseconds on purpose: the PID math divides by dt every tick, and at 120Hz a
     * millisecond-truncated dt alternates 8/9ms - a ±6% dt jitter injected into exactly the
     * terms the vsync-driven tick loop exists to keep clean.
     *
     * [isNewSample] must be false when [setPoint] is a re-presentation of a sample the caller
     * already passed in (the tick loop runs at display refresh, above the camera sample rate) -
     * it gates the wake-confirmation counting described in the class doc.
     */
    fun filter(setPoint: Float, timestampNanos: Long, isNewSample: Boolean = true): Float {
        val currentValue = value
        if (currentValue == null) {
            value = setPoint
            previousValue = setPoint
            lastTimeNanos = timestampNanos
            return setPoint
        }

        wakeCountedThisCall = false

        // Cap the total gap a single call will catch up on (e.g. head tracking was paused, or
        // the app was backgrounded) - without this, maxTimeDelayDurationSeconds-sized chunking
        // of an arbitrarily large gap would still mean an arbitrarily large number of chunks in
        // one call. Anything beyond the cap is simply dropped rather than caught up on.
        var dtSeconds =
            (lastTimeNanos?.let { (timestampNanos - it).coerceAtLeast(0L) / NANOS_PER_SECOND } ?: 0f)
                .coerceAtMost(MAX_CATCH_UP_SECONDS)
        lastTimeNanos = timestampNanos

        // Chunk large gaps into fixed-size steps, matching Pulse's tick() - one big unstable
        // step is worse than several capped ones.
        while (dtSeconds > maxTimeDelayDurationSeconds) {
            step(setPoint, maxTimeDelayDurationSeconds, isNewSample)
            dtSeconds -= maxTimeDelayDurationSeconds
        }
        if (dtSeconds > 0f) {
            step(setPoint, dtSeconds, isNewSample)
        }

        return value ?: setPoint
    }

    private fun step(setPoint: Float, dtSeconds: Float, isNewSample: Boolean) {
        val pv = value ?: setPoint
        val error = setPoint - pv

        if (isQuiescent) {
            // Stay frozen until error clears the wider wake threshold - a plain re-check
            // against minimumValueStep would let noise near that threshold flicker the filter
            // in and out of quiescence every tick.
            if (abs(error) < minimumValueStep * wakeThresholdMultiplier) {
                // Only a fresh sample carries evidence about the wake streak - a re-presented
                // one already voted.
                if (isNewSample) {
                    consecutiveWakeTicks = 0
                }
                // Leaky freeze - see class doc. Absorbs a settling residual gradually instead
                // of leaving it parked to be corrected in one visible snap once it eventually
                // crosses the wake threshold. Runs on stale ticks too: absorption is a
                // time-based process, not a per-sample one.
                value = pv + error * (quiescentCatchUpRate * dtSeconds).coerceAtMost(1f)
                previousValue = pv
                return
            }
            // Require sustained clearance before actually waking - otherwise a single-tick
            // spike runs a full PID episode and can leave the filter settled somewhere slightly
            // off from where it started (see class doc). Counted at most once per DISTINCT
            // sample: dt-chunking runs several step()s on the same sample within one filter()
            // call, and the vsync tick loop re-presents the same sample across multiple calls -
            // letting either count would wake the filter off one noisy sample, the exact thing
            // this confirmation exists to prevent.
            if (isNewSample && !wakeCountedThisCall) {
                wakeCountedThisCall = true
                consecutiveWakeTicks++
            }
            if (consecutiveWakeTicks < wakeConfirmationTicks) {
                previousValue = pv
                return
            }
            consecutiveWakeTicks = 0
            isQuiescent = false
        }

        integral += error * dtSeconds
        val derivative = (pv - previousValue) / dtSeconds

        val reachedQuiescence = abs(error) < minimumValueStep &&
            abs(integral) < minimumValueStep &&
            abs(derivative * dtSeconds) < minimumValueStep

        val newValue = if (reachedQuiescence) {
            isQuiescent = true
            integral = 0f
            pv
        } else {
            val outputControl = kp * error + ki * integral + kd * derivative
            pv + outputControl * dtSeconds
        }

        // Damp accumulated integral every tick, matching Pulse - helps reach quiescence faster,
        // especially in the last moment when output is very close to setPoint.
        integral *= integralDamper

        previousValue = pv
        value = newValue
    }

    /** Clears internal state so the next [filter] call is treated as a fresh first sample. */
    fun reset() {
        value = null
        integral = 0f
        previousValue = 0f
        lastTimeNanos = null
        isQuiescent = false
        consecutiveWakeTicks = 0
    }
}

/**
 * An (x, y) gaze signal - either a raw tracking sample or a smoothed cursor position.
 *
 * A data class (with equality) on purpose: `StateFlow` dedups equal values, so emitting an
 * unchanged smoothed position is a no-op instead of a 60Hz recomposition of the cursor at rest
 * (sceneview's `Vector3` has no `equals`, which is why the old code needed manual
 * last-emitted-value tracking).
 */
data class GazePoint(val x: Float, val y: Float)

/**
 * Applies an independent [PIDFilter] per screen axis. Gain defaults live in [PIDFilter] alone -
 * this class deliberately declares none, so the two can't drift apart.
 */
class GazePIDFilter(
    private val xFilter: PIDFilter = PIDFilter(),
    private val yFilter: PIDFilter = PIDFilter(),
) {
    fun filter(x: Float, y: Float, timestampNanos: Long, isNewSample: Boolean = true): GazePoint =
        GazePoint(
            xFilter.filter(x, timestampNanos, isNewSample),
            yFilter.filter(y, timestampNanos, isNewSample),
        )

    fun reset() {
        xFilter.reset()
        yFilter.reset()
    }
}
