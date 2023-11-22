package com.willowtree.vocable.utils

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.net.Uri
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.ar.core.ArCoreApk
import com.willowtree.vocable.BuildConfig
import com.willowtree.vocable.R
import com.willowtree.vocable.facetracking.FaceTrackFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

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
    fun initialize(faceTrackingPointerUpdates: FaceTrackingPointerUpdates) {
        this.faceTrackingPointerUpdates = faceTrackingPointerUpdates

        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)

        if (BuildConfig.USE_HEAD_TRACKING && checkIsSupportedDevice()) {
            CoroutineScope(Dispatchers.Main).launch {
                faceTrackingPermissions.permissionState.collect { headTrackingState ->
                    when (headTrackingState) {
                        IFaceTrackingPermissions.PermissionState.PermissionRequested -> {
                            requestPermissions()
                        }

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
        } else {
            // We are not actually observing in this scenario, but this will clear the permissions and still update the view
            faceTrackingPermissions.disableFaceTracking()
            togglePointerVisible(false)
        }

    }

    private var hasSetupAr: Boolean = false
    private fun setupArTracking() {
        if (!hasSetupAr) {

            hasSetupAr = true

            listenToOrientationChanges()

            if (activity.supportFragmentManager.findFragmentById(R.id.face_fragment) == null) {
                activity.supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.face_fragment, FaceTrackFragment())
                    .commitAllowingStateLoss()
            } else {
                activity.window
                    .decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    .or(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
                    .or(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                    .or(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                    .or(View.SYSTEM_UI_FLAG_FULLSCREEN)
                    .or(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }

        }
    }

    private fun togglePointerVisible(visible: Boolean) {
        faceTrackingPointerUpdates.toggleVisibility(if (!BuildConfig.USE_HEAD_TRACKING) false else visible)
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     *
     * Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     *
     * Disables Permissions if the device is not supported.
     */
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
        if (java.lang.Double.parseDouble(openGlVersionString) < minOpenGlVersion) {
            Timber.e("TAG", "Sceneform requires OpenGL ES 3.0 later")
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    /**
     * Resets the FaceTrackFragment if the device is rotated to ensure proper AR orientation relative to the users head.
     */
    private fun listenToOrientationChanges() {
        val windowManager = activity.windowManager
        val displayListener = object : DisplayManager.DisplayListener {

            private var orientation = activity.windowManager.defaultDisplay.rotation

            override fun onDisplayChanged(displayId: Int) {
                val newOrientation = windowManager.defaultDisplay.rotation
                // Only reset FaceTrackFragment if device is rotated 180 degrees
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

    /**
     * If the device rotates 180 degrees (portrait to portrait/landscape to landscape), the
     * activity won't be destroyed and recreated. This means that the FaceTrackFragment will not
     * reset its camera positioning. The only way to reset it currently is to create a new
     * instance of the fragment and add it to the activity.
     * @param tag The tag to use for the FaceTrackFragment, should be unique to the orientation
     */
    private fun resetFaceTrackFragment(tag: String) {
        if (!activity.supportFragmentManager.isDestroyed && activity.supportFragmentManager.findFragmentByTag(tag) == null) {
            activity.supportFragmentManager
                .beginTransaction()
                .replace(R.id.face_fragment, FaceTrackFragment(), tag)
                .commitAllowingStateLoss()
        }
    }

    //region Permissions

    private val permissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                faceTrackingPermissions.enableFaceTracking()
            } else {
                faceTrackingPermissions.disableFaceTracking()
                permissionSettingsDialog.show()
            }
        }

    private val permissionResultLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(object : ActivityResultContract<String, Boolean>() {
            override fun createIntent(context: Context, input: String): Intent {
                return Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", activity.packageName, null)
                }
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
                return when (resultCode) {
                    Activity.RESULT_OK, Activity.RESULT_CANCELED -> true
                    else -> false
                }
            }
        }) { result ->
            if (result) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

    private val permissionRationaleDialog by lazy {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.permissions_rationale_title))
            .setPositiveButton(activity.getString(R.string.permissions_confirm)) { _, _ ->
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton(activity.getString(R.string.permissions_cancel)) { dialog, _ ->
                faceTrackingPermissions.disableFaceTracking()
                dialog.dismiss()
            }
            .setOnDismissListener { dialog ->
                faceTrackingPermissions.disableFaceTracking()
                dialog.dismiss()
            }.create()
    }

    private val permissionSettingsDialog by lazy {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.permissions_missing_dialog_title))
            .setMessage(activity.getString(R.string.permissions_missing_dialog_body))
            .setPositiveButton(activity.getString(R.string.permissions_confirm)) { _, _ ->
                permissionResultLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton(activity.getString(R.string.permissions_cancel)) { dialog, _ ->
                faceTrackingPermissions.disableFaceTracking()
                dialog.dismiss()
            }
            .create()
    }

    private fun requestPermissions() {

        // Bypass check if we already have permission
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            faceTrackingPermissions.enableFaceTracking()
            return
        }

        // Permission has been denied before, show rationale
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
            permissionRationaleDialog.show()
            return
        }

        // Ask for permissions.
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    //endregion Permissions

}