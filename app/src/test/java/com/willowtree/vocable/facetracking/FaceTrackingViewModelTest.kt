package com.willowtree.vocable.facetracking

import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.core.GazePoint
import com.willowtree.vocable.core.HeadPositionTracker
import com.willowtree.vocable.ui.facetracking.FaceTrackingViewModel
import com.willowtree.vocable.ui.sensitivity.SensitivityViewModel
import com.willowtree.vocable.utils.FakeFaceTrackingPermissions
import com.willowtree.vocable.utils.FakeFrameClock
import com.willowtree.vocable.utils.FakeVocableSharedPreferences
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class FaceTrackingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val frameClock = FakeFrameClock()
    private val sharedPrefs = FakeVocableSharedPreferences(
        headTrackingEnabled = true,
        sensitivity = SensitivityViewModel.MEDIUM_SENSITIVITY,
    )

    private fun createViewModel(isTablet: Boolean = true) = FaceTrackingViewModel(
        headTrackingPermissions = FakeFaceTrackingPermissions(enabled = true),
        sharedPrefs = sharedPrefs,
        isTablet = isTablet,
        isAccessibilityEnabled = { false },
        frameClock = frameClock,
    )

    // Feeds enough identical samples to lock the neutral. No display frames are advanced, so
    // the PID filter stays unseeded and the next post-calibration frame is a pass-through -
    // which makes expected values exact.
    private fun FaceTrackingViewModel.calibrateAt(x: Float, y: Float) {
        repeat(HeadPositionTracker.NEUTRAL_CALIBRATION_SAMPLES) { onHeadSample(x, y, -1f) }
    }

    @Test
    fun `cursor holds screen-center while the neutral calibrates`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()

        viewModel.onHeadSample(0.1f, 0.1f, -1f)
        frameClock.advanceFrame()

        assertEquals(GazePoint(0f, 0f), viewModel.adjustedVector.value)
    }

    @Test
    fun `first sample after calibration maps relative to the averaged neutral`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(isTablet = true)
            viewModel.calibrateAt(0.1f, 0.2f)

            viewModel.onHeadSample(0.2f, 0.4f, -1f)
            frameClock.advanceFrame()

            // Image-space offset (0.1, 0.2) x amplitudes (4, 2) = (0.4, 0.4); the PID filter's
            // first sample passes through unchanged, and tablets get no reachability scaling.
            val emitted = viewModel.adjustedVector.value!!
            assertEquals(0.4f, emitted.x, 1e-6f)
            assertEquals(0.4f, emitted.y, 1e-6f)
        }

    @Test
    fun `phones double the smoothed y output, applied after filtering`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel(isTablet = false)
            viewModel.calibrateAt(0.1f, 0.2f)

            viewModel.onHeadSample(0.2f, 0.4f, -1f)
            frameClock.advanceFrame()

            // Same signal as the tablet test: x is untouched, y is doubled on the OUTPUT side
            // (0.4 -> 0.8). If the scaling leaked to the input side, the filter's y deadband
            // behavior would change too - see the work-log's pre-filter-scaling bug.
            val emitted = viewModel.adjustedVector.value!!
            assertEquals(0.4f, emitted.x, 1e-6f)
            assertEquals(0.8f, emitted.y, 1e-6f)
        }

    @Test
    fun `at rest the state flow keeps the same value instance instead of emitting per tick`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.calibrateAt(0.1f, 0.1f)
            viewModel.onHeadSample(0.2f, 0.2f, -1f)

            // Converge fully onto the held target, then keep ticking at rest.
            repeat(600) { frameClock.advanceFrame() }
            val atRest = viewModel.adjustedVector.value
            repeat(60) { frameClock.advanceFrame() }

            // GazePoint equality means unchanged output never replaces the StateFlow value, so
            // the cursor doesn't recompose at 60Hz while frozen.
            assertSame(atRest, viewModel.adjustedVector.value)
            assertTrue("tick loop should stay alive while a target exists", frameClock.hasPendingFrame)
        }

    @Test
    fun `disabling head tracking stops the tick loop`() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel()
        viewModel.calibrateAt(0.1f, 0.1f)
        viewModel.onHeadSample(0.2f, 0.2f, -1f)
        frameClock.advanceFrame()
        assertTrue(frameClock.hasPendingFrame)

        sharedPrefs.setHeadTrackingEnabled(false)
        frameClock.advanceFrame()

        assertFalse("tick loop should stop instead of running at vsync forever", frameClock.hasPendingFrame)
    }

    @Test
    fun `re-enabling head tracking resets the filter and recalibrates the neutral`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            viewModel.calibrateAt(0.1f, 0.1f)
            viewModel.onHeadSample(0.3f, 0.3f, -1f)
            frameClock.advanceFrame()
            assertNotEquals(GazePoint(0f, 0f), viewModel.adjustedVector.value)

            sharedPrefs.setHeadTrackingEnabled(false)
            frameClock.advanceFrame()
            sharedPrefs.setHeadTrackingEnabled(true)

            // A new ARCore session tracks from a different pose (device/user moved while
            // disabled). If the old neutral or filter history survived the toggle, this sample
            // would map to a large offset; a correct reset means we're calibrating again and
            // the cursor holds center.
            viewModel.onHeadSample(0.5f, 0.5f, -1f)
            frameClock.advanceFrame()
            assertEquals(GazePoint(0f, 0f), viewModel.adjustedVector.value)
        }

    @Test
    fun `sensitivity scales cursor travel in screen mapping, not smoothing`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = createViewModel()
            val smoothedVector = GazePoint(0.1f, 0.1f)

            sharedPrefs.setSensitivity(SensitivityViewModel.LOW_SENSITIVITY)
            val low = viewModel.convertCoordSystems(smoothedVector, 1000f, 1000f)
            sharedPrefs.setSensitivity(SensitivityViewModel.HIGH_SENSITIVITY)
            val high = viewModel.convertCoordSystems(smoothedVector, 1000f, 1000f)

            // Same smoothed vector, more travel from screen center (500, 500) on High - iOS
            // semantics, where sensitivity is an amplitude knob and the PID constants never change.
            assertTrue(abs(high.x - 500f) > abs(low.x - 500f))
            assertTrue(abs(high.y - 500f) > abs(low.y - 500f))
        }
}
