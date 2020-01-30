package com.example.eyespeak

import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    val minOpenGlVersion = 3.0
    var faceTrackFragment: FaceTrackFragment? = null

    private val displayMetrics = DisplayMetrics()
    val size = Point()


    var pointer = PointerDrawable()
    var isTracking = false
    var isHitting = false
    private var isDragging = false
    private val allViews = mutableListOf<View>()
    private var currentView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        windowManager.defaultDisplay.getSize(size)
        setContentView(R.layout.activity_main)
        faceTrackFragment = supportFragmentManager.findFragmentById(R.id.face_fragment) as FaceTrackFragment

        with(allViews) {
            add(good_button)
            add(bad_button)
        }
    }

    override fun onStart() {
        super.onStart()
        pointer_view.updatePointerPositionPercent(100, 100)
    }

    fun updatePointer(x: Float, y: Float) {
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
        pointer_view.updatePointerPosition(newX, newY)
        pointer_view.bringToFront()

        if (currentView == null) {
            findIntersectingView(newX, newY)
        } else {
            if (!viewIntersects(currentView!!, newX, newY)) {
                (currentView as? EyeSpeakButton)?.onPointerExit()
                findIntersectingView(newX, newY)
            }
        }
    }

    private fun findIntersectingView(x: Float, y: Float) {
        currentView = null
        allViews.forEach {
            if (viewIntersects(it, x, y)) {
                currentView = it
                (currentView as EyeSpeakButton).onPointerEnter()
                return
            }
        }
    }

    private fun viewIntersects(view: View, x: Float, y: Float): Boolean {
        val coords = IntArray(2)
        view.getLocationOnScreen(coords)
        val rect = Rect(coords[0], coords[1], coords[0] + view.measuredWidth, coords[1] + view.measuredHeight)

        val pointerCoords = IntArray(2)
        pointer_view.getLocationOnScreen(pointerCoords)
        val pointerRect = Rect(pointerCoords[0], pointerCoords[1], pointerCoords[0] + pointer_view.measuredWidth, pointerCoords[1] + pointer_view.measuredHeight)
        return rect.contains(pointerRect.centerX(), pointerRect.centerY())
    }

    fun onUpdate() {
        val trackingChanged = updateTracking()
        val contentView = findViewById<View>(android.R.id.content)
        if (trackingChanged) {
            if (isTracking) {
                contentView.overlay.add(pointer)
            } else {
                contentView.overlay.remove(pointer)
            }
            contentView.invalidate()
        }

        if (isTracking) {
           val hitTestChanged = updateHitTest()
            if (hitTestChanged) {
                pointer.setEnabled(isHitting)
                contentView.invalidate()
            }
        }
    }

    private fun updateHitTest(): Boolean {
        val frame = faceTrackFragment?.arSceneView?.arFrame
        val pt = getScreenCenter()

        val wasHitting = isHitting
        isHitting = false
        frame?.let {
            frame.hitTest(pt?.x?.toFloat() ?: 0f, pt?.y?.toFloat() ?: 0f).forEach { hit ->
                val trackable = hit.trackable
                if (trackable is Plane &&
                    trackable.isPoseInPolygon(hit.hitPose)
                ) {
                    isHitting = true
                    return@forEach
                }
            }
        }

        return wasHitting != this.isHitting

    }

    private fun getScreenCenter(): Point? {
        val vw = findViewById<View>(android.R.id.content)
        return Point(vw.width / 2, vw.height / 2)
    }

    private fun updateTracking(): Boolean {
        val frame = faceTrackFragment?.getArScene()?.arFrame
        val wasTracking = isTracking
        isTracking = frame != null && frame.camera.trackingState == TrackingState.TRACKING
        return isTracking != wasTracking
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
    fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        if (ArCoreApk.getInstance().checkAvailability(activity) === ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            Log.e("TAG", "Augmented Faces requires ARCore.")
            Toast.makeText(activity, "Augmented Faces requires ARCore", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }
        val openGlVersionString =
            (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (java.lang.Double.parseDouble(openGlVersionString) < minOpenGlVersion) {
            Log.e("TAG", "Sceneform requires OpenGL ES 3.0 later")
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show()
            activity.finish()
            return false
        }
        return true
    }

    private class CustomDragShadowBuilder : View.DragShadowBuilder() {

    }
}


