package com.example.eyespeak

import com.google.ar.core.AugmentedFace
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment
import java.util.*
import kotlin.collections.ArrayList


class FaceTrackFragment : ArFragment() {

    private val session = Session(context, EnumSet.of(Session.Feature.FRONT_CAMERA))
    val config = getSessionConfiguration(session)
    private var faceList = session.getAllTrackables(AugmentedFace::class.java)

    fun getPoses(): List<Pose> {
        var poses = ArrayList<Pose>()
        faceList.forEach { augmentedFace ->
            poses.add(augmentedFace.centerPose)
        }
        return poses

    }
}



}