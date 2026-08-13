package com.willowtree.vocable.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PIDFilterTest {

    @Test
    fun `first sample is passed through unchanged`() {
        val filter = PIDFilter()

        val result = filter.filter(setPoint = 0.42f, timestampMs = 0L)

        assertEquals(0.42f, result)
    }

    @Test
    fun `moves toward a new set point without overshooting on the very next tick`() {
        val filter = PIDFilter()
        filter.filter(setPoint = 0f, timestampMs = 0L)

        val result = filter.filter(setPoint = 1f, timestampMs = 33L)

        assertTrue("expected movement toward target, got $result", result > 0f)
        assertTrue("expected output to still be short of target, got $result", result < 1f)
    }

    @Test
    fun `converges to a held set point over repeated ticks`() {
        val filter = PIDFilter()
        var value = filter.filter(setPoint = 0f, timestampMs = 0L)

        var timestamp = 0L
        repeat(200) {
            timestamp += 16L
            value = filter.filter(setPoint = 1f, timestampMs = timestamp)
        }

        assertTrue("expected convergence near 1.0, got $value", abs(value - 1f) < 0.05f)
    }

    @Test
    fun `integral is damped each tick so a sustained error does not accumulate unbounded`() {
        // A tiny integral gain isolates the integral term's own growth from proportional/derivative
        // contributions, so we can assert the *damping* behavior specifically rather than overall
        // convergence (which the "converges to a held set point" test already covers).
        val filter = PIDFilter(kp = 0f, ki = 1f, kd = 0f, minimumValueStep = -1f)

        var timestamp = 0L
        var lastValue = 0f
        repeat(50) {
            timestamp += 16L
            lastValue = filter.filter(setPoint = 1f, timestampMs = timestamp)
        }

        // An undamped integral term windows up without bound over 50 ticks of sustained error;
        // per-tick damping keeps it - and the value it drives - bounded near the set point
        // instead of diverging.
        assertTrue("expected damped integral to stay bounded, got $lastValue", abs(lastValue - 1f) < 0.5f)
    }

    @Test
    fun `freezes output and resets internal accumulation once within the quiescence threshold`() {
        val filter = PIDFilter(minimumValueStep = 0.05f)
        var timestamp = 0L
        var value = filter.filter(setPoint = 1f, timestampMs = timestamp)

        // Drive close enough to the set point to enter quiescence.
        repeat(200) {
            timestamp += 16L
            value = filter.filter(setPoint = 1f, timestampMs = timestamp)
        }
        assertTrue("expected to have reached quiescence, got $value", abs(value - 1f) < 0.05f)

        // Once quiescent, holding the same set point should not micro-jitter the output.
        timestamp += 16L
        val next = filter.filter(setPoint = 1f, timestampMs = timestamp)

        assertEquals(value, next, 1e-6f)
    }

    @Test
    fun `large time gaps are chunked instead of applied as one unstable step`() {
        val filter = PIDFilter()
        filter.filter(setPoint = 0f, timestampMs = 0L)

        // A single 0.5s step at these gains would fly past the set point in one unstable jump;
        // chunking into 0.05s-capped steps keeps the result finite and within a sane range.
        val result = filter.filter(setPoint = 1f, timestampMs = 500L)

        assertTrue("expected chunked result to stay bounded, got $result", result.isFinite() && abs(result) < 10f)
    }

    @Test
    fun `reset clears state so the next sample is treated as a fresh first sample`() {
        val filter = PIDFilter()
        filter.filter(setPoint = 0f, timestampMs = 0L)
        filter.filter(setPoint = 1f, timestampMs = 16L)

        filter.reset()
        val result = filter.filter(setPoint = 5f, timestampMs = 1000L)

        assertEquals(5f, result)
    }

    @Test
    fun `vector3 filter applies an independent PIDFilter per axis`() {
        val filter = Vector3PIDFilter()

        val first = filter.filter(x = 0f, y = 0f, z = 0f, timestampMs = 0L)
        assertEquals(Triple(0f, 0f, 0f), first)

        val second = filter.filter(x = 1f, y = -1f, z = 0.5f, timestampMs = 16L)

        assertTrue(second.first > 0f)
        assertTrue(second.second < 0f)
        assertTrue(second.third > 0f)
    }
}
