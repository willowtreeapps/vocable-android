package com.willowtree.vocable.facetracking

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.*
import com.google.ar.core.AugmentedFace
import com.google.ar.sceneform.math.Vector3
import com.willowtree.vocable.R
import com.willowtree.vocable.utils.VocableSharedPreferences
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

class FaceTrackingViewModel : ViewModel(), LifecycleObserver, KoinComponent {

    companion object {
        private const val FACE_DETECTION_TIMEOUT = 1000
    }

    private val viewModelJob = SupervisorJob()
    private val backgroundScope = CoroutineScope(viewModelJob + Dispatchers.IO)

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
                }
            }
        }

    private var faceTrackingJob: Job? = null

    private val liveAdjustedVector = MutableLiveData<Vector3>()
    val adjustedVector: LiveData<Vector3> = liveAdjustedVector

    private val livePointerLocation = MutableLiveData<Vector3>()
    val pointerLocation: LiveData<Vector3> = livePointerLocation

    private val liveShowError = MutableLiveData<Boolean>()
    val showError: LiveData<Boolean> = liveShowError

    private var isTablet = false

    private var oldVector: Vector3? = null

    private var lastDetectedFaceTime = 0L

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
        isTablet = get<Context>().resources.getBoolean(R.bool.is_tablet)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        lastDetectedFaceTime = System.currentTimeMillis()
    }

    @SuppressLint("NullSafeMutableLiveData")
    fun onSceneUpdate(augmentedFaces: Collection<AugmentedFace>?) {
        if (!headTrackingEnabled) {
            liveShowError.postValue(false)
            return
        }

        // After losing track of a face, we will allow up to 1 second for a face to be re-detected
        // before showing the error.
        if (!augmentedFaces.isNullOrEmpty() || lastDetectedFaceTime == 0L) {
            lastDetectedFaceTime = System.currentTimeMillis()
        }

        val faceDetectionTimeoutExpired = System.currentTimeMillis() - lastDetectedFaceTime > FACE_DETECTION_TIMEOUT

        if (augmentedFaces.isNullOrEmpty() && faceDetectionTimeoutExpired) {
            liveShowError.postValue(true)
            return
        }

        if (liveShowError.value == true) {
            liveShowError.postValue(false)
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

                when (oldVector) {
                    null -> {
                        oldVector = Vector3(x, y, z)
                        liveAdjustedVector.postValue(oldVector)
                    }
                    else -> {
                        if (!isTablet) {
                            y *= 2F
                        }
                        val adjustedVector = Vector3.lerp(oldVector, Vector3(x, y, z), sensitivity)
                        liveAdjustedVector.postValue(adjustedVector)
                        oldVector = adjustedVector
                    }
                }
            }
        }
    }

    fun onScreenPointAvailable(screenPoint: Vector3) {
        livePointerLocation.postValue(screenPoint)
    }

    override fun onCleared() {
        viewModelJob.cancel()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPrefsListener)
    }
}