package com.willowtree.vocable.ui.facetracking

import androidx.compose.runtime.Composable

/**
 * Debug-variant host for the alternate tracking engines (#678 engine comparison). Declared with
 * an identical signature in the release source set (as an empty stub), so MainActivity in main
 * source can reference it without the release build ever linking a MediaPipe class.
 */
@Composable
fun DebugEngineTrackingScreen(engine: TrackingEngine, viewModel: FaceTrackingViewModel) {
    when (engine) {
        TrackingEngine.FACE_DETECTOR -> FaceDetectorTrackingScreen(viewModel)
        TrackingEngine.FACE_LANDMARKER -> MediaPipeFaceTrackingScreen(viewModel)
        TrackingEngine.ARCORE -> Unit // hosted directly by MainActivity, never routed here
    }
}
