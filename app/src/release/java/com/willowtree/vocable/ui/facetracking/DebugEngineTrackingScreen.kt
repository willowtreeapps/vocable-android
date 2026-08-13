package com.willowtree.vocable.ui.facetracking

import androidx.compose.runtime.Composable

/**
 * Release stub for the debug-only tracking-engine comparison host (#678). Release builds contain
 * no alternate engines (no MediaPipe dependencies, screens, or model assets), and
 * [FaceTrackingViewModel] never selects a non-ARCore engine outside debug builds, so this is
 * unreachable in practice - it exists only so MainActivity in main source compiles per-variant.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun DebugEngineTrackingScreen(engine: TrackingEngine, viewModel: FaceTrackingViewModel) = Unit
