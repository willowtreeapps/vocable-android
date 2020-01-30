package com.example.eyespeak

import android.os.Bundle
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.ArFragment
import java.util.*


class FaceTrackFragment : ArFragment() {

    private var oldVector: Vector3? = null


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
        scene.addOnUpdateListener {
            arSceneView.session?.getAllTrackables(AugmentedFace::class.java)?.let { faces ->
                faces.forEach { augmentedFace ->
                    val pose = augmentedFace.getRegionPose(AugmentedFace.RegionType.NOSE_TIP)
                    val zAxis = pose.zAxis
                    val x = zAxis[0]
                    val y = zAxis[1]
                    val z = -zAxis[2]

                    when (this.oldVector) {
                        null -> {
                            this.oldVector = Vector3(x, y, z)
                        }
                        else -> {
                            val goodVector = Vector3.lerp(this.oldVector, Vector3(x, y, z), 0.5F)
                            val vector =
                                scene.camera.worldToScreenPoint(goodVector)

                            (activity as MainActivity).updatePointer(
                                vector.x,
                                vector.y
                            )

                            this.oldVector = goodVector
                        }
                    }
                }
            }
        }
    }
}