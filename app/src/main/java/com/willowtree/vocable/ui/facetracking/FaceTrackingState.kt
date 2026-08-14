package com.willowtree.vocable.ui.facetracking

import io.github.sceneview.collision.Vector3

data class FaceTrackingState(
    val headTrackingEnabled: Boolean = false,
    val showError: Boolean = false,
    val pointerLocation: Vector3? = null,
)
