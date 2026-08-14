package com.willowtree.vocable.core

import kotlin.math.abs

/**
 * Maps the nose-tip's position in the camera's display-oriented frame to a screen-directional
 * gaze offset, relative to a neutral captured at tracking start.
 *
 * POSITION-based tracking, not orientation-based. The engine comparison (#678) isolated a
 * yaw->pitch cross-error in ARCore's face ORIENTATION estimate: the cursor swooped vertically
 * during horizontal turns, and the artifact survived every consumption-side fix (centerPose,
 * angle decomposition, velocity gating, and finally a port of iOS's exact camera-relative ray
 * projection) - placing it in the orientation estimate itself. iOS doesn't show it because
 * iPhones face-track with the TrueDepth depth sensor; ARCore fits a mesh to flat RGB, and an
 * orientation derived from that fit bends under yaw. Directly OBSERVED positions don't:
 * MediaPipe FaceDetector's image-space landmark position showed no swoop and "perfect"
 * horizontal feel on-device. This class is that same signal shape from the shipped engine: the
 * nose-tip's position in the display-oriented camera frame, normalized by depth (= image-space
 * position, distance-invariant), relative to a neutral averaged over the first
 * [calibrationSampleCount] tracked samples.
 *
 * Axis signs: x is amplified as-is and y confirmed on-device (x was correct, y read reversed
 * under the old orientation signal) - the display-oriented camera frame's +y and
 * `convertCoordSystems`' inversion stack up such that the raw offset is already
 * screen-directional for y.
 *
 * Pure math, no Android/ARCore types - deliberately, so the full tracking pipeline is
 * exercisable in JVM unit tests.
 */
class HeadPositionTracker(
    private val calibrationSampleCount: Int = NEUTRAL_CALIBRATION_SAMPLES,
    private val amplitudeX: Float = POSITION_AMPLITUDE_X,
    private val amplitudeY: Float = POSITION_AMPLITUDE_Y,
) {
    companion object {
        // Samples averaged before the neutral position locks (~0.7s at ARCore's ~30fps). A
        // single-first-frame neutral was tried and rested visibly off-center: ARCore's first
        // tracked sample lands before the mesh fit stabilizes and before the user has settled
        // facing the screen, and whatever offset existed in that instant became "center".
        const val NEUTRAL_CALIBRATION_SAMPLES = 20

        // Gain applied to the depth-normalized nose-position offset. The position signal is
        // ~4x weaker per degree of head rotation than the old orientation components (the nose
        // swings on a ~10cm lever arm around the neck at ~40cm from the device), so these are
        // correspondingly larger. Y is half of X because the PID tick loop applies the phone
        // `y * 2` reachability scaling on the smoothed output.
        const val POSITION_AMPLITUDE_X = 4f
        const val POSITION_AMPLITUDE_Y = 2f

        private const val MIN_DEPTH_METERS = 0.05f
    }

    private var neutralX = 0f
    private var neutralY = 0f
    private var isCalibrated = false
    private var sumX = 0f
    private var sumY = 0f
    private var sampleCount = 0

    /**
     * Processes one tracked sample: the nose-tip translation in the display-oriented camera
     * frame, in meters. Returns the amplified gaze offset - or (0, 0) while the neutral is
     * still calibrating, so the cursor holds screen-center. Always returns a fresh instance:
     * the PID tick loop relies on reference identity to tell a new sample from the same one
     * re-presented across vsync frames.
     */
    fun process(x: Float, y: Float, z: Float): GazePoint {
        val depth = abs(z).coerceAtLeast(MIN_DEPTH_METERS)
        val imageX = x / depth
        val imageY = y / depth

        if (!isCalibrated) {
            sumX += imageX
            sumY += imageY
            sampleCount++
            if (sampleCount >= calibrationSampleCount) {
                neutralX = sumX / sampleCount
                neutralY = sumY / sampleCount
                isCalibrated = true
                sumX = 0f
                sumY = 0f
                sampleCount = 0
            }
            return GazePoint(0f, 0f)
        }

        return GazePoint(
            (imageX - neutralX) * amplitudeX,
            (imageY - neutralY) * amplitudeY,
        )
    }

    /** Clears the neutral so the next samples recalibrate from scratch. */
    fun reset() {
        neutralX = 0f
        neutralY = 0f
        isCalibrated = false
        sumX = 0f
        sumY = 0f
        sampleCount = 0
    }
}
