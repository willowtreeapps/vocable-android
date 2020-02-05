package com.example.eyespeak

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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.ar.core.ArCoreApk
import kotlinx.android.synthetic.main.activity_main.*

abstract class BaseActivity : AppCompatActivity() {
    private val minOpenGlVersion = 3.0

    private val displayMetrics = DisplayMetrics()

    private var currentView: View? = null

    private lateinit var viewModel: FaceTrackingViewModel

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
        subscribeToPauseButton()
        VocableTextToSpeech.initialize(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        VocableTextToSpeech.shutdown()
    }

    protected abstract fun getPointerView(): PointerView

    protected abstract fun getAllViews(): List<View>

    @LayoutRes
    protected abstract fun getLayout(): Int

    @CallSuper
    protected fun subscribeToViewModel() {
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
                (currentView as? EyeSpeakButton)?.onPointerExit()
                findIntersectingView()
            }
        }
    }

    private fun subscribeToPauseButton() {
        pause_button.isPaused.observe(this, Observer {
            it.let {
                paused = it
            }
        })
    }

    private fun findIntersectingView() {
        currentView = null
        if (!paused) {
            getAllViews().forEach {
                if (viewIntersects(it, getPointerView())) {
                    currentView = it
                    (currentView as EyeSpeakButton).onPointerEnter()
                    return
                }
            }

        } else {
            getAllViews().forEach {
                if (viewIntersects(it, getPointerView())) {
                    currentView = it
                    if(currentView is PauseButton) {
                        (currentView as PauseButton).onPointerEnter()
                    }
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