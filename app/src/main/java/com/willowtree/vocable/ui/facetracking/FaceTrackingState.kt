package com.willowtree.vocable.ui.facetracking

import io.github.sceneview.collision.Vector3

/**
 * Which tracking source drives the gaze cursor. Only [ARCORE] exists in release builds; the
 * MediaPipe engines are debug-only comparison tooling for #678 (their trackers, screens, model
 * assets, and library dependencies all live in the debug source set / debugImplementation).
 * The enum itself lives in main source so [FaceTrackingState] and MainActivity can reference it
 * without touching any MediaPipe type.
 */
enum class TrackingEngine {
    ARCORE,
    FACE_DETECTOR,
    FACE_LANDMARKER,
}

data class FaceTrackingState(
    val headTrackingEnabled: Boolean = false,
    val showError: Boolean = false,
    val adjustedVector: Vector3? = null,
    val pointerLocation: Vector3? = null,
    val trackingEngine: TrackingEngine = TrackingEngine.ARCORE,
)
