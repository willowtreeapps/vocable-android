package com.willowtree.vocable.ui.facetracking

import android.content.Context
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.LifecycleObserver
import com.google.ar.core.AugmentedFace
import com.willowtree.vocable.R
import com.willowtree.vocable.ui.base.BaseViewModel
import com.willowtree.vocable.core.ComposeGazeTarget
import com.willowtree.vocable.core.GazeInteractionManager
import com.willowtree.vocable.core.IFaceTrackingPermissions
import com.willowtree.vocable.core.VocableSharedPreferences
import com.willowtree.vocable.core.Vector3OneEuroFilter
import com.willowtree.vocable.core.isEnabled
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

        // Prototype (#676) empirical amplitude knob for the MediaPipe FaceLandmarker path -
        // tune on-device. Units are now radians of yaw/pitch (MediaPipeFaceTracker.
        // headForwardYawPitch), not raw vector components - a full head turn is roughly
        // 0.3-0.5 rad, so this needs a materially different scale than the old raw-vector
        // amplitude did.
        private const val MEDIAPIPE_AMPLITUDE_X = 2.5f
        private const val MEDIAPIPE_AMPLITUDE_Y = 2.5f

        // Neutral yaw/pitch (radians) for a straight-ahead pose when the phone is held
        // naturally (not on a fixed perpendicular stand) - still non-zero even with the
        // yaw/pitch conversion, since the mismatch is between the canonical face model's own
        // axes and the camera's, not just handheld tilt. Placeholder pending on-device
        // measurement with the new yaw/pitch signal - see MediaPipeFaceTracker's diagnostic log.
        // Measured on-device (2026-08-11, Pixel 3a): settled steady-state yaw/pitch (degrees
        // converted to radians) for a straight-ahead pose with the phone held naturally.
        private const val NEUTRAL_YAW = 0.157f // ~9 degrees
        private const val NEUTRAL_PITCH = -0.0785f // ~-4.5 degrees

        // Prototype (#629) One Euro Filter tuning - replaces the shipped sensitivity-based
        // lerp for this comparison branch. Structurally different from a fixed blend fraction:
        // cutoff adapts to an internally-tracked velocity estimate, so it doesn't need a
        // deadzone/threshold (no boundary to flicker across) to get both "stable at rest" and
        // "responsive when moving" at once. Starting guesses, tune on-device:
        // - MIN_CUTOFF (Hz): smoothing strength at rest - lower = smoother/more lag when still.
        // - BETA: how fast cutoff rises with velocity - higher = less lag when moving quickly.
        private const val ONE_EURO_MIN_CUTOFF = 0.5f
        private const val ONE_EURO_BETA = 20f
    }

    private var faceTrackingJob: Job? = null
    private val viewModelJob = SupervisorJob()
    private val backgroundScope = CoroutineScope(viewModelJob + Dispatchers.IO)

    private val oneEuroFilter = Vector3OneEuroFilter(minCutoff = ONE_EURO_MIN_CUTOFF, beta = ONE_EURO_BETA)

    private val liveAdjustedVector = MutableStateFlow<Vector3?>(null)
    val adjustedVector : StateFlow<Vector3?> = liveAdjustedVector

    private val sharedPrefs: VocableSharedPreferences by inject()
    private var sensitivity = VocableSharedPreferences.DEFAULT_SENSITIVITY
    private var headTrackingEnabled = true
    private val sharedPrefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                VocableSharedPreferences.KEY_SENSITIVITY -> {
                    sensitivity = sharedPrefs.getSensitivity()
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
        // Apply scaling factor to make it easier to reach corners
        val sensitivityX = 2.0f
        // Increase Y sensitivity (1.5x) to help reach bottom corners
        val sensitivityY = 1.5f 
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
        updateState { copy(headTrackingEnabled = headTrackingEnabled) }
        
        // Collect permission state
        backgroundScope.launch {
            headTrackingPermissions.permissionState.collect { state ->
                val enabled = state.isEnabled()
                updateState { copy(headTrackingEnabled = enabled) }
            }
        }
    }

    fun onSceneUpdate(augmentedFaces: Collection<AugmentedFace>?, useCenterPose: Boolean = false) {
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
                // centerPose ("the physical center point of the user's head" per ARCore's own
                // docs) is a fuller head-orientation pose than a single region's pose -
                // comparing it against the existing NOSE_TIP region pose per #676/#629 findings
                // that ARCore's own signal richness, not just MediaPipe, might close the gap
                // with iOS's less-head-movement-needed feel.
                val pose = if (useCenterPose) {
                    augmentedFace.centerPose
                } else {
                    augmentedFace.getRegionPose(AugmentedFace.RegionType.NOSE_TIP)
                }
                val zAxis = pose.zAxis
                val x = zAxis[0]
                var y = zAxis[1]
                val z = -zAxis[2]

                if (!isTablet) {
                    y *= 2F
                }
                applySmoothedVector(x, y, z)
            }
        }
    }

    // Prototype (#676): MediaPipe FaceLandmarker path, mirrors onSceneUpdate above but takes a
    // head-orientation "forward" vector from MediaPipeFaceTracker (decoded from
    // facialTransformationMatrixes) instead of an ARCore AugmentedFace's pose.zAxis.
    // convertCoordSystems (ARCore-tuned) stays untouched - amplitude is the only remap needed
    // here, since forward is already a signed, roughly-zero-when-centered direction component,
    // the same shape of value pose.zAxis already is for the ARCore path above.
    fun onMediaPipeUpdate(headForward: Offset?) {
        if (!uiState.value.headTrackingEnabled) {
            if (uiState.value.showError) {
                updateState { copy(showError = false) }
            }
            return
        }

        if (headForward != null || lastDetectedFaceTime == 0L) {
            lastDetectedFaceTime = System.currentTimeMillis()
        }

        val faceDetectionTimeoutExpired = System.currentTimeMillis() - lastDetectedFaceTime > FACE_DETECTION_TIMEOUT

        if (headForward == null && faceDetectionTimeoutExpired) {
            if (!uiState.value.showError) {
                updateState { copy(showError = true) }
            }
            return
        }

        if (uiState.value.showError) {
            updateState { copy(showError = false) }
        }

        val forward = headForward ?: return

        // Negated: on-device testing showed both axes reversed (turn left -> cursor right,
        // look up -> cursor down) with the yaw/pitch signal, same as the raw-vector approach
        // on prototype/mediapipe-facedetector needed - the sign convention doesn't carry over
        // automatically between the two different underlying signals.
        val centeredX = -(forward.x - NEUTRAL_YAW) * MEDIAPIPE_AMPLITUDE_X
        val centeredY = -(forward.y - NEUTRAL_PITCH) * MEDIAPIPE_AMPLITUDE_Y

        // No time-adjustment hack needed here (unlike the old lerp approach) - the One Euro
        // Filter takes a real timestamp per call and adapts to actual elapsed time itself,
        // so MediaPipe's slower ~65ms cadence vs ARCore's ~33ms doesn't need compensating for.
        backgroundScope.launch {
            applySmoothedVector(centeredX, centeredY, 0f)
        }
    }

    private fun applySmoothedVector(x: Float, y: Float, z: Float) {
        // The One Euro Filter's output is already the final smoothed value - no separate lerp
        // on top of it needed (that would just be stacking two smoothers on each other).
        val (fx, fy, fz) = oneEuroFilter.filter(x, y, z, System.currentTimeMillis())
        val adjustedVector = Vector3(fx, fy, fz)
        updateState { copy(adjustedVector = adjustedVector) }
        liveAdjustedVector.value = adjustedVector
    }

    override fun onCleared() {
        viewModelJob.cancel()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPrefsListener)
    }
}
