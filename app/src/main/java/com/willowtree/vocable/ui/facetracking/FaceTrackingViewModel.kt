package com.willowtree.vocable.ui.facetracking

import android.content.SharedPreferences
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.viewModelScope
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Pose
import com.willowtree.vocable.core.ComposeGazeTarget
import com.willowtree.vocable.core.FrameClock
import com.willowtree.vocable.core.GazeInteractionManager
import com.willowtree.vocable.core.GazePIDFilter
import com.willowtree.vocable.core.GazePoint
import com.willowtree.vocable.core.HeadPositionTracker
import com.willowtree.vocable.core.IFaceTrackingPermissions
import com.willowtree.vocable.core.IVocableSharedPreferences
import com.willowtree.vocable.core.VocableSharedPreferences
import com.willowtree.vocable.core.isEnabled
import com.willowtree.vocable.ui.base.BaseViewModel
import com.willowtree.vocable.ui.sensitivity.SensitivityViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * THREADING: everything in this class runs on the main thread, and that confinement is
 * load-bearing. sceneview (2.3.3) drives [onSceneUpdate] from `ARSceneView.onFrame`, a
 * main-thread `Choreographer.FrameCallback` that calls `session.update()` synchronously -
 * verified in its sources; there is no separate "ARCore session thread" delivering it. The PID
 * tick ([FrameClock]) is the same main-thread Choreographer. [onSceneUpdate]'s reset path
 * mutates the same filter/tracker state the tick loop reads, which is safe only because both
 * run on the main thread - if a sceneview upgrade ever moves session updates off-main, this
 * class needs real synchronization, not sprinkled `@Volatile`.
 */
class FaceTrackingViewModel(
    headTrackingPermissions: IFaceTrackingPermissions,
    private val sharedPrefs: IVocableSharedPreferences,
    private val isTablet: Boolean,
    private val isAccessibilityEnabled: () -> Boolean,
    private val frameClock: FrameClock,
) : BaseViewModel<FaceTrackingState, FaceTrackingEvent>(FaceTrackingState()) {

    companion object {
        private const val FACE_DETECTION_TIMEOUT = 1000
    }

    // Defaults match iOS's actual production PID constants (HeadGazeTrackingInterpolator.swift
    // / PIDInterpolator.swift, via the vendored Pulse library) - that PID is genuinely live in
    // shipped iOS builds today, not a guess. Replaces the old fixed-fraction Vector3.lerp.
    private val pidFilter = GazePIDFilter()

    private val positionTracker = HeadPositionTracker()

    // Latest raw (unsmoothed) gaze sample - the PID tick loop reads this on its own schedule
    // rather than being driven directly by onSceneUpdate. Every sample is a fresh GazePoint
    // instance, so the tick loop can use reference identity to tell a genuinely new sample
    // from the same one re-presented across vsync frames - the filter's wake-confirmation
    // counting depends on that distinction (see PIDFilter's class doc).
    private var latestRawTarget: GazePoint? = null
    private var lastFilteredSample: GazePoint? = null

    private var isTicking = false

    // Ticks the PID filter on the display's vsync callback (Android's actual equivalent of
    // iOS's CADisplayLink) instead of once per incoming ARCore frame or an approximated
    // delay()-based loop. Two reasons this matters, not just one: (1) ARCore's AugmentedFace
    // updates land at ~30fps, under a typical display's refresh rate, so a per-frame-only tick
    // left the cursor only as smooth as the raw sensor cadence; (2) a coroutine delay() loop's
    // actual elapsed time isn't precisely timed the way a vsync callback is, and since the PID
    // math divides/multiplies by dt every tick, that timing jitter injects real noise into the
    // integral/derivative terms - confirmed on-device as visible overshoot/bounce before the
    // cursor settled, not just a quiescence-threshold flicker.
    //
    // The loop stops itself when there's no target or head tracking is off, and onHeadSample
    // restarts it with the next sample - otherwise a self-reposting vsync callback keeps the
    // main thread waking at refresh rate for the ViewModel's whole life even for touch-only
    // users (Pulse pauses its CADisplayLink for the same reason).
    private val onPidFrame: (Long) -> Unit = ::tickPidFilter

    private fun tickPidFilter(frameTimeNanos: Long) {
        val target = latestRawTarget
        if (target == null || !uiState.value.headTrackingEnabled) {
            isTicking = false
            return
        }

        val isNewSample = target !== lastFilteredSample
        lastFilteredSample = target
        val smoothed = pidFilter.filter(target.x, target.y, frameTimeNanos, isNewSample)

        // Reachability scaling applied to the smoothed output, not the raw input feeding the
        // filter - doing it before smoothing doubled y's raw noise floor right along with the
        // signal (confirmed on-device: y drifted at rest while x stayed put), since
        // minimumValueStep's deadband was sized for x's unscaled noise floor.
        val scaled = if (!isTablet) GazePoint(smoothed.x, smoothed.y * 2f) else smoothed

        // GazePoint has value equality, so the StateFlow dedups frozen-output ticks by itself -
        // no emission, no recomposition of the cursor at rest.
        _adjustedVector.value = scaled

        frameClock.requestFrame(onPidFrame)
    }

    private fun startTicking() {
        if (isTicking) return
        isTicking = true
        frameClock.requestFrame(onPidFrame)
    }

    private val _adjustedVector = MutableStateFlow<GazePoint?>(null)
    val adjustedVector: StateFlow<GazePoint?> = _adjustedVector.asStateFlow()

    // The user's Low/Medium/High sensitivity setting, as a cursor-travel amplitude multiplier.
    // This matches what "sensitivity" means on iOS (CursorSensitivity.swift scales the
    // NDC-to-screen mapping; the PID constants are fixed regardless) - NOT the old Android
    // meaning, where the stored value was the lerp blend fraction and "High" meant less
    // smoothing. Applied in convertCoordSystems, after smoothing, so it never changes the
    // noise floor the PID filter sees.
    private var sensitivityAmplitude = 1f

    // Stored values are the lerp-era constants (0.05/0.10/0.15) written verbatim by
    // SensitivityViewModel; multipliers mirror iOS's CursorSensitivity range midpoints
    // (3.0/4.0/5.25) relative to Medium. Any unrecognized stored value falls back to Medium.
    private fun sensitivityToAmplitude(storedSensitivity: Float): Float = when (storedSensitivity) {
        SensitivityViewModel.LOW_SENSITIVITY -> 0.75f
        SensitivityViewModel.HIGH_SENSITIVITY -> 1.3f
        else -> 1f
    }

    private val sharedPrefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                VocableSharedPreferences.KEY_SENSITIVITY -> {
                    sensitivityAmplitude = sensitivityToAmplitude(sharedPrefs.getSensitivity())
                }

                VocableSharedPreferences.KEY_HEAD_TRACKING_ENABLED -> {
                    setHeadTrackingEnabled(sharedPrefs.getHeadTrackingEnabled())
                }
            }
        }

    private fun setHeadTrackingEnabled(enabled: Boolean) {
        if (enabled && !uiState.value.headTrackingEnabled) {
            // Re-enabling composes a brand-new ARCore session (the AR scene leaves composition
            // entirely while disabled), and the neutral, filter history, and raw target are
            // camera-frame quantities from the previous session - the device or user has
            // usually moved in between, and nothing else clears them (the tracking-loss reset
            // below only runs while enabled). Without this, the cursor rests off-center after
            // a re-enable until a >=1s tracking loss happens to recalibrate.
            resetTracking()
        }
        updateState { copy(headTrackingEnabled = enabled) }
    }

    private fun resetTracking() {
        pidFilter.reset()
        positionTracker.reset()
        latestRawTarget = null
        lastFilteredSample = null
    }

    private var lastDetectedFaceTime = 0L

    // Track the last hovered target to handle enter/exit events
    private var lastTarget: ComposeGazeTarget? = null

    fun convertCoordSystems(vector: GazePoint, screenHeightPx: Float, screenWidthPx: Float): Offset {
        // Invert X axis logic: (1.0f - vector.x) instead of (vector.x + 1.0f)
        // Apply scaling factor to make it easier to reach corners; the user's Low/Medium/High
        // sensitivity setting multiplies these base factors (see sensitivityAmplitude).
        val sensitivityX = 2.0f * sensitivityAmplitude
        // Increase Y sensitivity (1.5x) to help reach bottom corners
        val sensitivityY = 1.5f * sensitivityAmplitude
        val pixelX = (1.0f - vector.x * sensitivityX) * 0.5f * screenWidthPx
        val pixelY = (1.0f - vector.y * sensitivityY) * 0.5f * screenHeightPx
        return Offset(pixelX, pixelY)
    }

    fun intersect(offset: Offset): ComposeGazeTarget? {
        val targets = GazeInteractionManager.getTargets()
        val x = offset.x.roundToInt()
        val y = offset.y.roundToInt()

        // Find the first target containing the point.
        return targets.firstOrNull { it.bounds.contains(x, y) }
    }

    fun handleHover(target: ComposeGazeTarget?) {
        if (target != lastTarget) {
            lastTarget?.onExit?.invoke()
            lastTarget = target
            target?.onEnter?.invoke()

            // Announce accessibility label if available
            target?.accessibilityLabel?.let { label ->
                if (isAccessibilityEnabled()) {
                    sendEvent(FaceTrackingEvent.Speak(label))
                }
            }
        }
    }

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
        sensitivityAmplitude = sensitivityToAmplitude(sharedPrefs.getSensitivity())
        setHeadTrackingEnabled(sharedPrefs.getHeadTrackingEnabled())

        viewModelScope.launch {
            headTrackingPermissions.permissionState.collect { state ->
                setHeadTrackingEnabled(state.isEnabled())
            }
        }
    }

    fun onSceneUpdate(augmentedFaces: Collection<AugmentedFace>?, cameraPose: Pose?) {
        if (!uiState.value.headTrackingEnabled) {
            if (uiState.value.showError) {
                updateState { copy(showError = false) }
            }
            return
        }

        if (!augmentedFaces.isNullOrEmpty() || lastDetectedFaceTime == 0L) {
            lastDetectedFaceTime = System.currentTimeMillis()
        }

        val faceDetectionTimeoutExpired = System.currentTimeMillis() - lastDetectedFaceTime > FACE_DETECTION_TIMEOUT

        if (augmentedFaces.isNullOrEmpty() && faceDetectionTimeoutExpired) {
            if (!uiState.value.showError) {
                updateState { copy(showError = true) }
                // Matches iOS's needsResetOnNextUpdate on tracking loss: without this, the
                // filter's integral/derivative history spans the gap and the cursor swoops in
                // from its stale position when the face is re-acquired, instead of starting
                // fresh at the new position - and re-acquisition recalibrates the neutral
                // rather than resuming a stale offset.
                resetTracking()
            }
            return
        }

        if (uiState.value.showError) {
            updateState { copy(showError = false) }
        }

        if (cameraPose == null) return

        // Runs inline on the caller's (main) thread, not a background job: the per-sample math
        // is a pose compose plus a few multiplies, and the old launch-and-skip-if-busy pattern
        // (from when this path did heavy region-pose work) silently dropped camera frames
        // whenever dispatcher scheduling lagged - every sample ARCore produces should reach
        // the filter.
        val augmentedFace = augmentedFaces?.firstOrNull() ?: return
        val noseInCamera = cameraPose.inverse()
            .compose(augmentedFace.getRegionPose(AugmentedFace.RegionType.NOSE_TIP))
        val position = noseInCamera.translation
        onHeadSample(position[0], position[1], position[2])
    }

    /**
     * Engine-agnostic seam: takes the nose-tip translation in the display-oriented camera
     * frame, no ARCore types - JVM tests drive the full tracking pipeline through here
     * (AugmentedFace can't be constructed off-device).
     */
    internal fun onHeadSample(x: Float, y: Float, z: Float) {
        latestRawTarget = positionTracker.process(x, y, z)
        // Hand off to the PID tick loop rather than smoothing here - it ticks at display
        // refresh, independent of how often a new sample arrives, and stops itself when
        // there's nothing left to do.
        startTicking()
    }

    override fun onCleared() {
        frameClock.cancel()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPrefsListener)
    }
}
