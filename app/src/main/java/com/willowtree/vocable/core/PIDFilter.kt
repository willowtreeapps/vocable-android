package com.willowtree.vocable.core

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
 *   overshooting/oscillating.
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
 */
class PIDFilter(
    private val kp: Float = 3.307f,
    private val ki: Float = 0.365f,
    private val kd: Float = 0.690f,
    private val minimumValueStep: Float = 0.010f,
    private val integralDamper: Float = 0.9f,
    private val maxTimeDelayDurationSeconds: Float = 0.05f,
) {
    private var value: Float? = null
    private var integral = 0f
    private var previousValue = 0f
    private var lastTimeMs: Long? = null

    fun filter(setPoint: Float, timestampMs: Long): Float {
        val currentValue = value
        if (currentValue == null) {
            value = setPoint
            previousValue = setPoint
            lastTimeMs = timestampMs
            return setPoint
        }

        var dtSeconds = lastTimeMs?.let { (timestampMs - it).coerceAtLeast(0L) / 1000f } ?: 0f
        lastTimeMs = timestampMs

        // Chunk large gaps into fixed-size steps, matching Pulse's tick() - one big unstable
        // step is worse than several capped ones.
        while (dtSeconds > maxTimeDelayDurationSeconds) {
            step(setPoint, maxTimeDelayDurationSeconds)
            dtSeconds -= maxTimeDelayDurationSeconds
        }
        if (dtSeconds > 0f) {
            step(setPoint, dtSeconds)
        }

        return value ?: setPoint
    }

    private fun step(setPoint: Float, dtSeconds: Float) {
        val pv = value ?: setPoint
        val error = setPoint - pv

        integral += error * dtSeconds
        val derivative = (pv - previousValue) / dtSeconds

        val isQuiescent = kotlin.math.abs(error) < minimumValueStep &&
            kotlin.math.abs(integral) < minimumValueStep &&
            kotlin.math.abs(derivative * dtSeconds) < minimumValueStep

        val newValue = if (isQuiescent) {
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
        lastTimeMs = null
    }
}

/** Applies an independent [PIDFilter] per axis to a 3-component signal. */
class Vector3PIDFilter(kp: Float = 3.307f, ki: Float = 0.365f, kd: Float = 0.690f) {
    private val xFilter = PIDFilter(kp, ki, kd)
    private val yFilter = PIDFilter(kp, ki, kd)
    private val zFilter = PIDFilter(kp, ki, kd)

    fun filter(x: Float, y: Float, z: Float, timestampMs: Long): Triple<Float, Float, Float> {
        return Triple(
            xFilter.filter(x, timestampMs),
            yFilter.filter(y, timestampMs),
            zFilter.filter(z, timestampMs)
        )
    }

    fun reset() {
        xFilter.reset()
        yFilter.reset()
        zFilter.reset()
    }
}
