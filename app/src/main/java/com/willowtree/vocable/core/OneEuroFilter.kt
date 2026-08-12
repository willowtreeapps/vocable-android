package com.willowtree.vocable.core

import kotlin.math.PI
import kotlin.math.abs

/**
 * One Euro Filter (Casiez, Roussel, Vogel 2012 - http://cristal.univ-lille.fr/~casiez/1euro/).
 * Adaptive low-pass filter for noisy real-time signals (mouse/gaze/gesture cursors): cutoff
 * frequency scales with an internally-tracked velocity estimate, so slow/stationary input
 * (jitter while resting) gets heavy smoothing while fast input (real intentional movement)
 * gets very little - unlike a fixed blend fraction, which can only pick one tradeoff point for
 * both cases, or a threshold/deadzone, which has a discontinuity to flicker across.
 *
 * [minCutoff] (Hz): cutoff frequency at zero velocity - lower means more smoothing at rest.
 * [beta]: how much cutoff increases per unit of velocity - higher means less lag when moving.
 * [dCutoff] (Hz): cutoff for the internal velocity estimate itself, rarely needs tuning.
 */
class OneEuroFilter(
    private val minCutoff: Float,
    private val beta: Float,
    private val dCutoff: Float = 1.0f
) {
    private val xFilter = LowPassFilter()
    private val dxFilter = LowPassFilter()
    private var lastTimeMs: Long? = null

    fun filter(x: Float, timestampMs: Long): Float {
        val prevX = xFilter.lastValue()
        val dtSeconds = lastTimeMs?.let { (timestampMs - it).coerceAtLeast(1L) / 1000f } ?: (1f / 30f)
        lastTimeMs = timestampMs

        val dx = if (prevX == null) 0f else (x - prevX) / dtSeconds
        val edx = dxFilter.filter(dx, alpha(dCutoff, dtSeconds))
        val cutoff = minCutoff + beta * abs(edx)
        return xFilter.filter(x, alpha(cutoff, dtSeconds))
    }

    private fun alpha(cutoff: Float, dtSeconds: Float): Float {
        val tau = 1f / (2f * PI.toFloat() * cutoff)
        return 1f / (1f + tau / dtSeconds)
    }

    private class LowPassFilter {
        private var hatXPrev: Float? = null

        fun filter(x: Float, alpha: Float): Float {
            val hatX = hatXPrev?.let { alpha * x + (1 - alpha) * it } ?: x
            hatXPrev = hatX
            return hatX
        }

        fun lastValue(): Float? = hatXPrev
    }
}

/** Applies an independent [OneEuroFilter] per axis to a 3-component signal. */
class Vector3OneEuroFilter(minCutoff: Float, beta: Float, dCutoff: Float = 1.0f) {
    private val xFilter = OneEuroFilter(minCutoff, beta, dCutoff)
    private val yFilter = OneEuroFilter(minCutoff, beta, dCutoff)
    private val zFilter = OneEuroFilter(minCutoff, beta, dCutoff)

    fun filter(x: Float, y: Float, z: Float, timestampMs: Long): Triple<Float, Float, Float> {
        return Triple(
            xFilter.filter(x, timestampMs),
            yFilter.filter(y, timestampMs),
            zFilter.filter(z, timestampMs)
        )
    }
}
