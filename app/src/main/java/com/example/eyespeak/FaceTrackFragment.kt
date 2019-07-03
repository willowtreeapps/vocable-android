package com.example.eyespeak

import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment
import java.util.*
import com.google.ar.core.AugmentedFace



class FaceTrackFragment: ArFragment() {

    private val session = Session(context, EnumSet.of(Session.Feature.FRONT_CAMERA))
    val config = getSessionConfiguration(session)
    var faceList = session.getAllTrackables(AugmentedFace::class.java)

    init {
        faceList.forEach { augmentedFace ->
            var pose= augmentedFace.centerPose
        }
    }



}