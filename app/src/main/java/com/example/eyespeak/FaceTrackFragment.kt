package com.example.eyespeak

import android.os.Bundle
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.math.Vector3
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
        with(planeDiscoveryController) {
            hide()
            setInstructionView(null)
        }
        attachFaceTracker()
    }

    private fun attachFaceTracker() {
        val scene = arSceneView.scene
        arSceneView.scene.addOnUpdateListener {
            arSceneView.session?.getAllTrackables(AugmentedFace::class.java)?.let {
                it.forEach { augmentedFace ->
                    val pose = augmentedFace.getRegionPose(AugmentedFace.RegionType.NOSE_TIP)
                    val vector =
                        scene.camera.worldToScreenPoint(Vector3(pose.tx(), pose.ty(), pose.tz()))
                    (activity as MainActivity).updatePointer(
                        vector.x,
                        vector.y
                    )
                }
            }
        }
    }
}