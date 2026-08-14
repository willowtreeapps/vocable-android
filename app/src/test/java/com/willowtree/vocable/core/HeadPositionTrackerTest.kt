package com.willowtree.vocable.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadPositionTrackerTest {

    @Test
    fun `returns center while calibrating and locks the neutral as the average`() {
        val tracker = HeadPositionTracker(calibrationSampleCount = 2, amplitudeX = 1f, amplitudeY = 1f)

        assertEquals(GazePoint(0f, 0f), tracker.process(0.1f, 0.2f, -1f))
        assertEquals(GazePoint(0f, 0f), tracker.process(0.3f, 0.4f, -1f))

        // Neutral is the average of the calibration samples: (0.2, 0.3).
        val result = tracker.process(0.5f, 0.5f, -1f)
        assertEquals(0.3f, result.x, 1e-6f)
        assertEquals(0.2f, result.y, 1e-6f)
    }

    @Test
    fun `depth normalization makes the offset distance-invariant`() {
        val near = HeadPositionTracker(calibrationSampleCount = 1, amplitudeX = 1f, amplitudeY = 1f)
        near.process(0f, 0f, -0.5f)
        val far = HeadPositionTracker(calibrationSampleCount = 1, amplitudeX = 1f, amplitudeY = 1f)
        far.process(0f, 0f, -1f)

        // The same gaze angle (x/z ratio) at different distances must produce the same offset.
        val nearResult = near.process(0.05f, 0.05f, -0.5f)
        val farResult = far.process(0.1f, 0.1f, -1f)

        assertEquals(nearResult.x, farResult.x, 1e-6f)
        assertEquals(nearResult.y, farResult.y, 1e-6f)
    }

    @Test
    fun `amplitudes scale each axis independently`() {
        val tracker = HeadPositionTracker(calibrationSampleCount = 1, amplitudeX = 4f, amplitudeY = 2f)
        tracker.process(0f, 0f, -1f)

        val result = tracker.process(0.1f, 0.1f, -1f)

        assertEquals(0.4f, result.x, 1e-6f)
        assertEquals(0.2f, result.y, 1e-6f)
    }

    @Test
    fun `reset clears the neutral so the next samples recalibrate`() {
        val tracker = HeadPositionTracker(calibrationSampleCount = 1, amplitudeX = 1f, amplitudeY = 1f)
        tracker.process(0.1f, 0.1f, -1f)
        assertNotEquals(GazePoint(0f, 0f), tracker.process(0.5f, 0.5f, -1f))

        tracker.reset()

        // Calibrating again: holds center, then maps relative to the NEW neutral.
        assertEquals(GazePoint(0f, 0f), tracker.process(0.5f, 0.5f, -1f))
        val recalibrated = tracker.process(0.6f, 0.6f, -1f)
        assertEquals(0.1f, recalibrated.x, 1e-6f)
        assertEquals(0.1f, recalibrated.y, 1e-6f)
    }

    @Test
    fun `a zero depth is clamped instead of dividing toward infinity`() {
        val tracker = HeadPositionTracker(calibrationSampleCount = 1, amplitudeX = 1f, amplitudeY = 1f)
        tracker.process(0f, 0f, -1f)

        val result = tracker.process(0.1f, 0.1f, 0f)

        assertTrue(result.x.isFinite() && result.y.isFinite())
        assertEquals(0.1f / 0.05f, result.x, 1e-4f)
    }

    @Test
    fun `every returned sample is a fresh instance for reference-identity freshness checks`() {
        // The PID tick loop distinguishes new samples from re-presented ones by reference
        // identity - equal-valued samples must still be distinct instances.
        val tracker = HeadPositionTracker(calibrationSampleCount = 1, amplitudeX = 1f, amplitudeY = 1f)
        tracker.process(0f, 0f, -1f)

        val first = tracker.process(0.1f, 0.1f, -1f)
        val second = tracker.process(0.1f, 0.1f, -1f)

        assertEquals(first, second)
        assertNotSame(first, second)
    }
}
