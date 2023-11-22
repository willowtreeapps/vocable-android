package com.willowtree.vocable.facetracking

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment
import java.util.EnumSet

class FaceTrackFragment : ArFragment() {

    private val viewModel: FaceTrackingViewModel by activityViewModels()

    override fun getSessionConfiguration(session: Session): Config {
        val config = Config(session)
        config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
        return config
    }

    override fun getSessionFeatures(): Set<Session.Feature> {
        return EnumSet.of(Session.Feature.FRONT_CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.headTrackingEnabledLd.observe(this) {
            enableFaceTracking(it)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        with(planeDiscoveryController) {
            hide()
            setInstructionView(null)
        }

        arSceneView.scene.addOnUpdateListener {
            viewModel.onSceneUpdate(arSceneView.session?.getAllTrackables(AugmentedFace::class.java))
        }

        viewModel.adjustedVector.observe(viewLifecycleOwner) {
            viewModel.onScreenPointAvailable(arSceneView.scene.camera.worldToScreenPoint(it))
        }
    }

    private fun enableFaceTracking(enable: Boolean) {
        if (enable) {
            arSceneView.resume()
        } else {
            arSceneView.pause()
            viewModel.onSceneUpdate(null)
        }
    }

    /**
     * This scenario should only happen if Permissions where given, which launches this fragment, and then revoked.
     *
     * ie. headTrackingEnabled = true, but no permissions
     *
     * This fragment will attempt to create its own permissions, bypassing the usual triggers but ultimately putting it back into flow.
     *
     * Funnels permission work back into Activity to allow it to be handle in a singular place.
     * More importantly we DO NOT allow the super to be called. Denied permissions will trigger built in dialogs and ultimately close the app
     *
     * This is ultimately a result of our dependency on [ArFragment], which we would like to move away from
     */
    @Deprecated(
        "Deprecated,  but it is what is used",
        ReplaceWith("Nothing, this is what AR is expecting")
    )
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        requireActivity().onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}
