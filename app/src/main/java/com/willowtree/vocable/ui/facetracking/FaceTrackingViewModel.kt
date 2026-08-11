package com.willowtree.vocable.ui.facetracking

import android.content.Context
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.LifecycleObserver
import com.google.ar.core.AugmentedFace
import com.google.mediapipe.tasks.components.containers.NormalizedKeypoint
import com.willowtree.vocable.R
import com.willowtree.vocable.ui.base.BaseViewModel
import com.willowtree.vocable.core.ComposeGazeTarget
import com.willowtree.vocable.core.GazeInteractionManager
import com.willowtree.vocable.core.IFaceTrackingPermissions
import com.willowtree.vocable.core.VocableSharedPreferences
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

        // Prototype (#676) empirical amplitude knob for the MediaPipe path - tune on-device,
        // separate from the deterministic [0,1]->centered remap it's applied alongside.
        private const val MEDIAPIPE_AMPLITUDE_X = 2.0f
        private const val MEDIAPIPE_AMPLITUDE_Y = 2.0f

        // Measured on-device (2026-08-11, Pixel 3a) neutral raw keypoint position when the
        // nose is pointed straight at screen-center - not 0.5/0.5, see comment at call site.
        private const val NEUTRAL_X = 0.505f
        private const val NEUTRAL_Y = 0.55f
    }

    private var faceTrackingJob: Job? = null
    private val viewModelJob = SupervisorJob()
    private val backgroundScope = CoroutineScope(viewModelJob + Dispatchers.IO)

    private var oldVector: Vector3? = null

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
                var y = zAxis[1]
                val z = -zAxis[2]

                if (!isTablet) {
                    y *= 2F
                }
                applySmoothedVector(x, y, z)
            }
        }
    }

    // Prototype (#676): MediaPipe FaceDetector path, mirrors onSceneUpdate above but takes a
    // single normalized nose-tip keypoint from MediaPipeFaceTracker instead of an ARCore
    // AugmentedFace. convertCoordSystems (ARCore-tuned) stays untouched — the remap below adapts
    // MediaPipe's [0,1] image-space coordinate into a comparable numeric range before reusing the
    // same smoothing helper, so everything downstream of liveAdjustedVector is unaffected.
    fun onMediaPipeUpdate(noseTip: NormalizedKeypoint?) {
        if (!uiState.value.headTrackingEnabled) {
            if (uiState.value.showError) {
                updateState { copy(showError = false) }
            }
            return
        }

        if (noseTip != null || lastDetectedFaceTime == 0L) {
            lastDetectedFaceTime = System.currentTimeMillis()
        }

        val faceDetectionTimeoutExpired = System.currentTimeMillis() - lastDetectedFaceTime > FACE_DETECTION_TIMEOUT

        if (noseTip == null && faceDetectionTimeoutExpired) {
            if (!uiState.value.showError) {
                updateState { copy(showError = true) }
            }
            return
        }

        if (uiState.value.showError) {
            updateState { copy(showError = false) }
        }

        val keypoint = noseTip ?: return

        // Deterministic remap: MediaPipe's normalized [0,1] image-space -> centered [-1,1]-ish
        // range, matching the convention convertCoordSystems already expects from ARCore's
        // zAxis output. MEDIAPIPE_AMPLITUDE is the separate "does this feel like ARCore" knob -
        // tune live on-device, it is not part of the deterministic remap above.
        // Flipped sign on both axes vs. the naive (keypoint - 0.5) remap: on-device testing
        // showed both axes reversed (turn left -> cursor right, look up -> cursor down),
        // the signature of a 180-degree-equivalent sign error upstream in
        // MediaPipeFaceTracker's rotate+mirror Matrix. Correcting it here (rather than the
        // Matrix pivot math) is equivalent for this single point and much simpler.
        // NEUTRAL_Y != 0.5f: the nose tip sits anatomically below face-center, so a face
        // dead-centered in frame reports keypoint.y() ~0.55, not 0.5 (measured on-device via
        // MediaPipeFaceTracker's diagnostic log). Treating raw 0.5 as neutral center amplified
        // that ~5% bias by the full gain chain into the cursor landing ~65% down the screen.
        val centeredX = (NEUTRAL_X - keypoint.x()) * 2f * MEDIAPIPE_AMPLITUDE_X
        val centeredY = (NEUTRAL_Y - keypoint.y()) * 2f * MEDIAPIPE_AMPLITUDE_Y

        backgroundScope.launch {
            applySmoothedVector(centeredX, centeredY, 0f)
        }
    }

    private fun applySmoothedVector(x: Float, y: Float, z: Float) {
        when (oldVector) {
            null -> {
                oldVector = Vector3(x, y, z)
                updateState { copy(adjustedVector = oldVector) }
                liveAdjustedVector.value = oldVector
            }

            else -> {
                // sensitivity (smoothing) is applied here
                val adjustedVector = Vector3.lerp(oldVector, Vector3(x, y, z), sensitivity)
                updateState { copy(adjustedVector = adjustedVector) }
                liveAdjustedVector.value = adjustedVector
                oldVector = adjustedVector
            }
        }
    }

    override fun onCleared() {
        viewModelJob.cancel()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPrefsListener)
    }
}
