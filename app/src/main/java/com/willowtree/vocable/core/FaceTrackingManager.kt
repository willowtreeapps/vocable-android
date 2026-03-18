package com.willowtree.vocable.core

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.ArCoreApk
import com.willowtree.vocable.BuildConfig
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.Double

/**
 * Interface for updating the AR pointer actions
 */
interface FaceTrackingPointerUpdates {
    fun toggleVisibility(visible: Boolean)
}

class FaceTrackingManager(
    private val activity: AppCompatActivity,
    private val faceTrackingPermissions: IFaceTrackingPermissions,
) {

    companion object {
        private const val minOpenGlVersion = 3.0
    }

    val displayMetrics = DisplayMetrics()

    private lateinit var faceTrackingPointerUpdates: FaceTrackingPointerUpdates

    /**
     * Initializes the FaceTrackingManager and begins listening to [IFaceTrackingPermissions.PermissionState] updates.
     * @param faceTrackingPointerUpdates The interface for updating user facing AR UI elements
     */
    suspend fun initialize(faceTrackingPointerUpdates: FaceTrackingPointerUpdates) {

        this.faceTrackingPointerUpdates = faceTrackingPointerUpdates

        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)

        if (BuildConfig.USE_HEAD_TRACKING && checkIsSupportedDevice()) {
            coroutineScope {
                launch {
                    faceTrackingPermissions.permissionState.collect { headTrackingState ->
                        when (headTrackingState) {
                            IFaceTrackingPermissions.PermissionState.Enabled -> {
                                togglePointerVisible(true)
                                setupArTracking()
                            }

                            IFaceTrackingPermissions.PermissionState.Disabled -> {
                                togglePointerVisible(false)
                            }
                        }
                    }
                }
            }
        } else {
            faceTrackingPermissions.disableFaceTracking()
            togglePointerVisible(false)
        }

    }

    private var hasSetupAr: Boolean = false
    private fun setupArTracking() {
        if (!hasSetupAr) {

            hasSetupAr = true

            listenToOrientationChanges()

            // MainActivity now creates the face fragment container programmatically.
            // We can still reference the same R.id.face_fragment.
//            if (activity.supportFragmentManager.findFragmentById(R.id.face_fragment) == null) {
//                activity.supportFragmentManager
//                    .beginTransaction()
//                    .replace(R.id.face_fragment, FaceTrackFragment())
//                    .commitAllowingStateLoss()
//            } else {
//                activity.window
//                    .decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    .or(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
//                    .or(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
//                    .or(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
//                    .or(View.SYSTEM_UI_FLAG_FULLSCREEN)
//                    .or(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
//            }

        }
    }

    private fun togglePointerVisible(visible: Boolean) {
        faceTrackingPointerUpdates.toggleVisibility(if (!BuildConfig.USE_HEAD_TRACKING) false else visible)
    }

    private fun checkIsSupportedDevice(): Boolean {
        if (ArCoreApk.getInstance().checkAvailability(activity) === ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            Timber.e("TAG", "Augmented Faces requires ARCore.")
            Toast.makeText(activity, "Augmented Faces requires ARCore", Toast.LENGTH_LONG).show()
            return false
        }
        val openGlVersionString =
            (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (Double.parseDouble(openGlVersionString) < minOpenGlVersion) {
            Timber.e("TAG", "Sceneform requires OpenGL ES 3.0 later")
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun listenToOrientationChanges() {
        val windowManager = activity.windowManager
        val displayListener = object : DisplayManager.DisplayListener {

            private var orientation = activity.windowManager.defaultDisplay.rotation

            override fun onDisplayChanged(displayId: Int) {
                val newOrientation = windowManager.defaultDisplay.rotation
                when (orientation) {
                    Surface.ROTATION_0 -> {
                        if (newOrientation == Surface.ROTATION_180) {
                            resetFaceTrackFragment("${Surface.ROTATION_180}")
                        }
                    }

                    Surface.ROTATION_90 -> {
                        if (newOrientation == Surface.ROTATION_270) {
                            resetFaceTrackFragment("${Surface.ROTATION_270}")
                        }
                    }

                    Surface.ROTATION_180 -> {
                        if (newOrientation == Surface.ROTATION_0) {
                            resetFaceTrackFragment("${Surface.ROTATION_0}")
                        }
                    }

                    Surface.ROTATION_270 -> {
                        if (newOrientation == Surface.ROTATION_90) {
                            resetFaceTrackFragment("${Surface.ROTATION_90}")
                        }
                    }
                }
                orientation = newOrientation
            }

            override fun onDisplayAdded(displayId: Int) = Unit

            override fun onDisplayRemoved(displayId: Int) = Unit
        }
        val displayManager = activity.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)
    }

    private fun resetFaceTrackFragment(tag: String) {
//        if (!activity.supportFragmentManager.isDestroyed && activity.supportFragmentManager.findFragmentByTag(tag) == null) {
//            activity.supportFragmentManager
//                .beginTransaction()
//                .replace(R.id.face_fragment, FaceTrackFragment(), tag)
//                .commitAllowingStateLoss()
//        }
    }
}
