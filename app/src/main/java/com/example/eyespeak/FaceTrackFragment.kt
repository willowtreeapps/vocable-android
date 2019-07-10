package com.example.eyespeak

import android.os.Bundle
import android.util.Log
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment
import java.util.*


class FaceTrackFragment : ArFragment() {

    override fun getSessionConfiguration(session: Session): Config {
        val config = Config(session)
        config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
        return config
    }

    override fun getSessionFeatures(): Set<Session.Feature> {
        return EnumSet.of<Session.Feature>(Session.Feature.FRONT_CAMERA)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        attachFaceTracker()
    }

    private fun attachFaceTracker() {
        var scene = arSceneView.scene
        scene.addOnUpdateListener { frameTime ->
            var faceList = arSceneView.session?.getAllTrackables(AugmentedFace::class.java)
            faceList?.let {
                for (augmentedFace in faceList) {
                    val pose = augmentedFace.centerPose
                    Log.w("POSE", "x: ${pose.qx()}, y: ${pose.qy()}")
                }
            }
        }
    }

}