package com.willowtree.vocable.facetracking

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment
import java.util.EnumSet

class FaceTrackFragment : Fragment() {

    private val viewModel: FaceTrackingViewModel by activityViewModels()

//    override fun getSessionConfiguration(session: Session): Config {
//        val config = Config(session)
//        config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
//        return config
//    }
//
//    override fun getSessionFeatures(): Set<Session.Feature> {
//        return EnumSet.of(Session.Feature.FRONT_CAMERA)
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.headTrackingEnabledLd.observe(this) {
            enableFaceTracking(it)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

//        with(planeDiscoveryController) {
//            hide()
//            setInstructionView(null)
//        }
//
//        arSceneView.scene.addOnUpdateListener {
//            viewModel.onSceneUpdate(arSceneView.session?.getAllTrackables(AugmentedFace::class.java))
//        }

//        viewModel.adjustedVector.observe(viewLifecycleOwner) {
//            viewModel.onScreenPointAvailable(arSceneView.scene.camera.worldToScreenPoint(it))
//        }
    }

    private fun enableFaceTracking(enable: Boolean) {
        if (enable) {
//            arSceneView.resume()
        } else {
//            arSceneView.pause()
            viewModel.onSceneUpdate(null)
        }
    }

    @Deprecated(
        "Permission requesting now handled by FaceTrackingManager",
        ReplaceWith("FaceTrackingManager")
    )
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) { }

}
