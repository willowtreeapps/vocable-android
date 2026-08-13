package com.willowtree.vocable.ui.facetracking

import android.content.Context
import android.content.SharedPreferences
import android.view.Choreographer
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.LifecycleObserver
import com.google.ar.core.AugmentedFace
import com.willowtree.vocable.R
import com.willowtree.vocable.ui.base.BaseViewModel
import com.willowtree.vocable.core.ComposeGazeTarget
import com.willowtree.vocable.core.GazeInteractionManager
import com.willowtree.vocable.core.IFaceTrackingPermissions
import com.willowtree.vocable.core.Vector3PIDFilter
import com.willowtree.vocable.core.VocableSharedPreferences
import com.willowtree.vocable.core.isEnabled
import com.willowtree.vocable.ui.sensitivity.SensitivityViewModel
import io.github.sceneview.collision.Vector3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import kotlin.math.roundToInt

class FaceTrackingViewModel(
    headTrackingPermissions: IFaceTrackingPermissions,
) : BaseViewModel<FaceTrackingState, FaceTrackingEvent>(FaceTrackingState()), LifecycleObserver, KoinComponent {

    companion object {
        private const val FACE_DETECTION_TIMEOUT = 1000
    }

    private var faceTrackingJob: Job? = null
    private val viewModelJob = SupervisorJob()
    private val backgroundScope = CoroutineScope(viewModelJob + Dispatchers.IO)

    // Defaults match iOS's actual production PID constants (HeadGazeTrackingInterpolator.swift
    // / PIDInterpolator.swift, via the vendored Pulse library) - that PID is genuinely live in
    // shipped iOS builds today, not a guess. Replaces the old fixed-fraction Vector3.lerp.
    private val pidFilter = Vector3PIDFilter()

    // Latest raw (unsmoothed) nose-tip vector from ARCore - the PID tick loop below reads this
    // on its own schedule rather than being driven directly by onSceneUpdate. Volatile: written
    // from the background face-tracking coroutine, read from the Choreographer callback on the
    // main thread.
    @Volatile
    private var latestRawTarget: Vector3? = null

    // Last values pushed into state by the tick loop - lets frozen-output ticks skip emission
    // entirely. Vector3 doesn't override equals, so without this every 60Hz tick would push a
    // distinct-but-identical object through the StateFlow and recompose the cursor at rest
    // (Pulse pauses its CADisplayLink at quiescence for the same reason).
    private var lastEmittedX = Float.NaN
    private var lastEmittedY = Float.NaN
    private var lastEmittedZ = Float.NaN

    // Ticks the PID filter on Choreographer's vsync callback (Android's actual equivalent of
    // iOS's CADisplayLink) instead of once per incoming ARCore frame or an approximated
    // delay()-based loop. Two reasons this matters, not just one: (1) ARCore's AugmentedFace
    // updates land at ~30fps, under a typical display's refresh rate, so a per-frame-only tick
    // left the cursor only as smooth as the raw sensor cadence; (2) a coroutine delay() loop's
    // actual elapsed time isn't precisely timed the way a vsync callback is, and since the PID
    // math divides/multiplies by dt every tick, that timing jitter injects real noise into the
    // integral/derivative terms - confirmed on-device as visible overshoot/bounce before the
    // cursor settled, not just a quiescence-threshold flicker.
    private val pidTickCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val target = latestRawTarget
            if (target != null && uiState.value.headTrackingEnabled) {
                val timestampMs = frameTimeNanos / 1_000_000L
                val (fx, fy, fz) = pidFilter.filter(target.x, target.y, target.z, timestampMs)

                // Reachability scaling applied to the smoothed output, not the raw input feeding
                // the filter - doing it before smoothing doubled y's raw noise floor right along
                // with the signal (confirmed on-device: y drifted at rest while x stayed put),
                // since minimumValueStep's deadband was sized for x's unscaled noise floor.
                val scaledY = if (!isTablet) fy * 2F else fy

                if (fx != lastEmittedX || scaledY != lastEmittedY || fz != lastEmittedZ) {
                    lastEmittedX = fx
                    lastEmittedY = scaledY
                    lastEmittedZ = fz
                    val adjustedVector = Vector3(fx, scaledY, fz)
                    updateState { copy(adjustedVector = adjustedVector) }
                    liveAdjustedVector.value = adjustedVector
                }
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private val liveAdjustedVector = MutableStateFlow<Vector3?>(null)
    val adjustedVector : StateFlow<Vector3?> = liveAdjustedVector

    private val sharedPrefs: VocableSharedPreferences by inject()
    private var headTrackingEnabled = true

    // The user's Low/Medium/High sensitivity setting, as a cursor-travel amplitude multiplier.
    // This matches what "sensitivity" means on iOS (CursorSensitivity.swift scales the
    // NDC-to-screen mapping; the PID constants are fixed regardless) - NOT the old Android
    // meaning, where the stored value was the lerp blend fraction and "High" meant less
    // smoothing. Applied in convertCoordSystems, after smoothing, so it never changes the
    // noise floor the PID filter sees.
    private var sensitivityAmplitude = 1f

    // Stored values are the lerp-era constants (0.05/0.10/0.15) written verbatim by
    // SensitivityViewModel; multipliers mirror iOS's CursorSensitivity range midpoints
    // (3.0/4.0/5.25) relative to Medium.
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
                    headTrackingEnabled = sharedPrefs.getHeadTrackingEnabled()
                    updateState { copy(headTrackingEnabled = headTrackingEnabled) }
                }
            }
        }

    private var isTablet = false
    private var lastDetectedFaceTime = 0L

    // Track the last hovered target to handle enter/exit events
    private var lastTarget: ComposeGazeTarget? = null

    private val accessibilityManager = get<Context>().applicationContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    fun convertCoordSystems(vector: Vector3, screenHeightPx: Float, screenWidthPx: Float) : Offset {
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

    fun intersect(offset: Offset) : ComposeGazeTarget? {
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
                if (accessibilityManager.isEnabled) {
                    sendEvent(FaceTrackingEvent.Speak(label))
                }
            }
        }
    }

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
        isTablet = get<Context>().resources.getBoolean(R.bool.is_tablet)
        headTrackingEnabled = sharedPrefs.getHeadTrackingEnabled()
        sensitivityAmplitude = sensitivityToAmplitude(sharedPrefs.getSensitivity())
        updateState { copy(headTrackingEnabled = headTrackingEnabled) }
        
        // Collect permission state
        backgroundScope.launch {
            headTrackingPermissions.permissionState.collect { state ->
                val enabled = state.isEnabled()
                updateState { copy(headTrackingEnabled = enabled) }
            }
        }

        Choreographer.getInstance().postFrameCallback(pidTickCallback)
    }

    fun onSceneUpdate(augmentedFaces: Collection<AugmentedFace>?) {
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
                // fresh at the new position.
                pidFilter.reset()
                latestRawTarget = null
            }
            return
        }

        if (uiState.value.showError) {
            updateState { copy(showError = false) }
        }

        if (faceTrackingJob != null && faceTrackingJob?.isActive == true) {
            return
        }

        augmentedFaces?.firstOrNull()?.let { augmentedFace ->
            faceTrackingJob = backgroundScope.launch {
                val pose = augmentedFace.getRegionPose(AugmentedFace.RegionType.NOSE_TIP)
                val zAxis = pose.zAxis
                val x = zAxis[0]
                val y = zAxis[1]
                val z = -zAxis[2]

                // Hand off to the PID tick loop (see init{}) rather than smoothing here -
                // that loop ticks independently of how often a new ARCore sample arrives.
                latestRawTarget = Vector3(x, y, z)
            }
        }
    }

    override fun onCleared() {
        viewModelJob.cancel()
        Choreographer.getInstance().removeFrameCallback(pidTickCallback)
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPrefsListener)
    }
}
