package com.example.eyespeak

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.ArCoreApk
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    val minOpenGlVersion = 3.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        setContentView(R.layout.activity_main)
        // Code from AugmentedFaces sample is left below for reference
//        var sceneView = arFragment.arSceneView
//        sceneView.cameraStreamRenderPriority = Renderable.RENDER_PRIORITY_FIRST
//        var scene = sceneView.scene
//
//        scene.addOnUpdateListener { frameTime: FrameTime ->
//            val faceList = sceneView.getSession()!!.getAllTrackables(AugmentedFace::class.java)
//
//             Make new AugmentedFaceNodes for any new faces.
//            for (face in faceList) {
//                if (!faceNodeMap.containsKey(face)) {
//                    val faceNode = AugmentedFaceNode(face)
//                    faceNode.setParent(scene)
//                    faceNode.faceRegionsRenderable = faceRegionsRenderable
//                    faceNode.faceMeshTexture = faceMeshTexture
//                    faceNodeMap.put(face, faceNode)
//                }
//            }
//
//            // Remove any AugmentedFaceNodes associated with an AugmentedFace that stopped tracking.
//            val iter = faceNodeMap.entries.iterator()
//            while (iter.hasNext()) {
//                val entry = iter.next()
//                val face = entry.key
//                if (face.getTrackingState() == TrackingState.STOPPED) {
//                    val faceNode = entry.value
//                    faceNode.setParent(null)
//                    iter.remove()
//                }
//            }
//        }


    }

    override fun onStart() {
        super.onStart()
        pointer_view.updatePointerPositionPercent(100, 100)
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
        val openGlVersionString = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
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
}


