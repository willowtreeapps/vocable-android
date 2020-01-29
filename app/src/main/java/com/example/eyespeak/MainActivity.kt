package com.example.eyespeak

import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.Context
import android.graphics.Point
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        windowManager.defaultDisplay.getSize(size)
        setContentView(R.layout.activity_main)
        faceTrackFragment = supportFragmentManager.findFragmentById(R.id.face_fragment) as FaceTrackFragment

        parent_layout.requestPointerCapture()
        good_button.setOnDragListener { v, event ->
            when(event.action) {
                DragEvent.ACTION_DRAG_ENTERED -> {
                    Toast.makeText(this, "DRAG ENTER", Toast.LENGTH_LONG).show()
                }
            }
            Toast.makeText(this, "DRAG ENTER", Toast.LENGTH_LONG).show()
            false
        }
        drag_test_view.setOnDragListener { v, event ->
            Toast.makeText(this, "TEST DRAG", Toast.LENGTH_LONG).show()
            false
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
        if (!isDragging) {
            pointer_view.startDragAndDrop(ClipData.newPlainText("",""), View.DragShadowBuilder(), null, 0)
            isDragging = true
        }
        //dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, newX, newY, 0))
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


