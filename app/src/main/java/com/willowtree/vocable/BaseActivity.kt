package com.willowtree.vocable

import android.app.ActivityManager
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.ar.core.ArCoreApk
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.customviews.PointerView
import com.willowtree.vocable.facetracking.FaceTrackingViewModel
import com.willowtree.vocable.utils.VocableSharedPreferences
import org.koin.android.ext.android.inject

abstract class BaseActivity : AppCompatActivity() {

    private val minOpenGlVersion = 3.0

    private val displayMetrics = DisplayMetrics()

    private var currentView: View? = null

    private lateinit var viewModel: FaceTrackingViewModel

    private val sharedPrefs: VocableSharedPreferences by inject()

    private var paused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish()) {
            return
        }
        setContentView(getLayout())
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        viewModel = ViewModelProviders.of(this).get(FaceTrackingViewModel::class.java)
        subscribeToViewModel()
    }

    protected abstract fun getPointerView(): PointerView

    protected abstract fun getErrorView(): View

    protected abstract fun getAllViews(): List<View>

    @LayoutRes
    protected abstract fun getLayout(): Int

    @CallSuper
    protected open fun subscribeToViewModel() {
        viewModel.showError.observe(this, Observer {
            if (!sharedPrefs.getHeadTrackingEnabled()) {
                getPointerView().isVisible = false
                getErrorView().isVisible = false
                return@Observer
            }
            it?.let {
                if (it) {
                    (currentView as? PointerListener)?.onPointerExit()
                }
                getErrorView().isVisible = it
                getPointerView().isVisible = !it
            }
        })
        viewModel.pointerLocation.observe(this, Observer {
            it.let {
                updatePointer(it.x, it.y)
            }
        })
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
                    currentView = it
                    (currentView as PointerListener).onPointerEnter()
                    return
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
            Log.e("TAG", "Augmented Faces requires ARCore.")
            Toast.makeText(this, "Augmented Faces requires ARCore", Toast.LENGTH_LONG).show()
            finish()
            return false
        }
        val openGlVersionString =
            (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (java.lang.Double.parseDouble(openGlVersionString) < minOpenGlVersion) {
            Log.e("TAG", "Sceneform requires OpenGL ES 3.0 later")
            Toast.makeText(this, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show()
            finish()
            return false
        }
        return true
    }
}