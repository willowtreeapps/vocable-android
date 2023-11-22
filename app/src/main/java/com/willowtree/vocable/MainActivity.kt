package com.willowtree.vocable

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.isVisible
import com.google.ar.core.ArCoreApk
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.customviews.PointerView
import com.willowtree.vocable.databinding.ActivityMainBinding
import com.willowtree.vocable.facetracking.FaceTrackFragment
import com.willowtree.vocable.facetracking.FaceTrackingViewModel
import com.willowtree.vocable.settings.selectionmode.HeadTrackingPermissionState
import com.willowtree.vocable.settings.selectionmode.SelectionModeViewModel
import com.willowtree.vocable.utils.VocableSharedPreferences
import com.willowtree.vocable.utils.VocableTextToSpeech
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ViewModelOwner
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainActivity : AppCompatActivity(),
                     EasyPermissions.PermissionCallbacks {

    private val minOpenGlVersion = 3.0
    private val displayMetrics = DisplayMetrics()
    private var currentView: View? = null
    private var paused = false
    private lateinit var binding: ActivityMainBinding
    private val sharedPrefs: VocableSharedPreferences by inject()
    private var hasSetupAr: Boolean = false
    private val allViews = mutableListOf<View>()

    private val selectionModeViewModel: SelectionModeViewModel by viewModel(owner = {
        ViewModelOwner.from(this)
    })
    private val faceTrackingViewModel: FaceTrackingViewModel by viewModel(owner = {
        ViewModelOwner.from(this)
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.pointerView.isVisible = false
        setContentView(binding.root)

        val canUseHeadTracking = BuildConfig.USE_HEAD_TRACKING
        val isSupportedDevice = checkIsSupportedDeviceOrFinish()

        if (canUseHeadTracking && isSupportedDevice) {
            selectionModeViewModel.headTrackingPermissionState.observe(this) { headTrackingState ->
                when (headTrackingState) {
                    HeadTrackingPermissionState.PermissionRequested -> requestPermissions()
                    HeadTrackingPermissionState.Enabled -> {
                        togglePointerVisible(true)
                        setupArTracking()
                    }

                    HeadTrackingPermissionState.Disabled -> {
                        togglePointerVisible(false)
                    }
                }
            }
        }


        faceTrackingViewModel.showError.observe(this) { showError ->
            if (!sharedPrefs.getHeadTrackingEnabled()) {
                getPointerView().isVisible = false
                getErrorView().isVisible = false
                return@observe
            }
            if (showError) {
                (currentView as? PointerListener)?.onPointerExit()
            }
            getErrorView().isVisible = showError
            getPointerView().isVisible = !showError
        }

        supportActionBar?.hide()
        VocableTextToSpeech.initialize(this)

        binding.mainNavHostFragment.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            allViews.clear()
        }
    }

    private fun togglePointerVisible(visible: Boolean) {
        binding.pointerView.isVisible = if (!BuildConfig.USE_HEAD_TRACKING) false else visible
    }

    private fun setupArTracking() {

        if (!hasSetupAr) {

            hasSetupAr = true

            if (supportFragmentManager.findFragmentById(R.id.face_fragment) == null) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.face_fragment, FaceTrackFragment())
                    .commitAllowingStateLoss()
            } else {
                window
                    .decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    .or(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
                    .or(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                    .or(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                    .or(View.SYSTEM_UI_FLAG_FULLSCREEN)
                    .or(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }

            faceTrackingViewModel.pointerLocation.observe(this) {
                updatePointer(it.x, it.y)
            }

            windowManager.defaultDisplay.getMetrics(displayMetrics)

            val displayListener = object : DisplayManager.DisplayListener {

                private var orientation = windowManager.defaultDisplay.rotation

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

            val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            displayManager.registerDisplayListener(displayListener, null)

        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onDestroy() {
        super.onDestroy()
        VocableTextToSpeech.shutdown()
    }

    private fun getErrorView(): View = binding.errorView.root

    private fun getPointerView(): PointerView = binding.pointerView

    fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(binding.parentLayout)
            getAllFragmentViews()
        }
        return allViews
    }

    fun resetAllViews() {
        allViews.clear()
    }

    private fun getAllChildViews(viewGroup: ViewGroup) {
        viewGroup.children.forEach {
            if (it is PointerListener) {
                allViews.add(it)
            } else if (it is ViewGroup) {
                getAllChildViews(it)
            }
        }
    }

    private fun getAllFragmentViews() {
        supportFragmentManager.fragments.forEach {
            if (it is BaseFragment<*>) {
                allViews.addAll(it.getAllViews())
            }
        }
    }

    /**
     * If the device rotates 180 degrees (portrait to portrait/landscape to landscape), the
     * activity won't be destroyed and recreated. This means that the FaceTrackFragment will not
     * reset its camera positioning. The only way to reset it currently is to create a new
     * instance of the fragment and add it to the activity.
     * @param tag The tag to use for the FaceTrackFragment, should be unique to the orientation
     */
    private fun resetFaceTrackFragment(tag: String) {
        if (!supportFragmentManager.isDestroyed && supportFragmentManager.findFragmentByTag(tag) == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.face_fragment, FaceTrackFragment(), tag)
                .commitAllowingStateLoss()
        }
    }

    private fun updatePointer(x: Float, y: Float) {
        var newX = x
        var newY = y
        if (x < 0) {
            newX = 0f
        } else if (x > displayMetrics.widthPixels) {
            newX = displayMetrics.widthPixels.toFloat()
        }

        if (y < 0) {
            newY = 0f
        } else if (y > displayMetrics.heightPixels) {
            newY = displayMetrics.heightPixels.toFloat()
        }
        getPointerView().updatePointerPosition(newX, newY)
        getPointerView().bringToFront()

        if (currentView == null) {
            findIntersectingView()
        } else {
            if (!viewIntersects(currentView!!, getPointerView())) {
                (currentView as? PointerListener)?.onPointerExit()
                findIntersectingView()
            }
        }
    }

    private fun findIntersectingView() {
        currentView = null
        if (!paused) {
            getAllViews().forEach {
                if (viewIntersects(it, getPointerView())) {
                    if (it.isEnabled && it.isVisible) {
                        currentView = it
                        (currentView as PointerListener).onPointerEnter()
                        return
                    }
                }
            }
        }
    }

    private fun viewIntersects(view1: View, view2: View): Boolean {
        val coords = IntArray(2)
        view1.getLocationOnScreen(coords)
        val rect = Rect(
            coords[0],
            coords[1],
            coords[0] + view1.measuredWidth,
            coords[1] + view1.measuredHeight
        )

        val view2Coords = IntArray(2)
        view2.getLocationOnScreen(view2Coords)
        val view2Rect = Rect(
            view2Coords[0],
            view2Coords[1],
            view2Coords[0] + view2.measuredWidth,
            view2Coords[1] + view2.measuredHeight
        )
        return rect.contains(view2Rect.centerX(), view2Rect.centerY())
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     *
     * Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     *
     * Finishes the activity if Sceneform can not run
     */
    private fun checkIsSupportedDeviceOrFinish(): Boolean {
        if (ArCoreApk.getInstance().checkAvailability(this) === ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            Timber.e("TAG", "Augmented Faces requires ARCore.")
            Toast.makeText(this, "Augmented Faces requires ARCore", Toast.LENGTH_LONG).show()
            finish()
            return false
        }
        val openGlVersionString =
            (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (java.lang.Double.parseDouble(openGlVersionString) < minOpenGlVersion) {
            Timber.e("TAG", "Sceneform requires OpenGL ES 3.0 later")
            Toast.makeText(this, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show()
            finish()
            return false
        }
        return true
    }

    /**
     * PERMISSIONS
     */

    private fun requestPermissions() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            selectionModeViewModel.enableHeadTracking()
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(
                host = this,
                rationale = "Allow camera permissions to enable Head Tracking.",
                requestCode = REQUEST_CAMERA_PERMISSION_CODE,
                perms = arrayOf(Manifest.permission.CAMERA)
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this@MainActivity, perms)) {
            SettingsDialog.Builder(this@MainActivity).build().show()
        } else {
            selectionModeViewModel.disableHeadTracking()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        requestPermissions()
    }

}

const val REQUEST_CAMERA_PERMISSION_CODE = 5504
