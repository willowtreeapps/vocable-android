package com.willowtree.vocable.ui.facetracking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.willowtree.vocable.R
import com.willowtree.vocable.core.MediaPipeFaceDetectorTracker
import com.willowtree.vocable.ui.base.MviScreen
import com.willowtree.vocable.ui.components.GazePointer

// Calibration/remap constants for the FaceDetector signal (a [0,1] image-space nose-tip
// position, not a head-pose angle), measured on-device on prototype/mediapipe-facedetector.
// NEUTRAL_X/Y aren't 0.5/0.5 because the nose tip sits anatomically below face-center. Sign
// flipped vs. the naive (keypoint - neutral) remap - on-device testing showed both axes
// reversed. AMPLITUDE_Y is half the prototype's 2.0 because the PID tick loop now applies the
// phone `y * 2` reachability scaling to every engine's smoothed output (the prototype's path
// never got that scaling), keeping end-to-end gain equal to the prototype's tuning.
private const val FACE_DETECTOR_AMPLITUDE_X = 2.0f
private const val FACE_DETECTOR_AMPLITUDE_Y = 1.0f
private const val FACE_DETECTOR_NEUTRAL_X = 0.505f
private const val FACE_DETECTOR_NEUTRAL_Y = 0.55f

/**
 * Debug-only (#678 engine comparison) alternative to FaceTrackingScreen.kt: MediaPipe
 * FaceDetector + CameraX - the lighter-weight MediaPipe option, ported from
 * prototype/mediapipe-facedetector. Same downstream GazePointer/error-banner UI, gated on the
 * same headTrackingEnabled state - only the tracking source differs. The remap into the
 * ViewModel's engine-agnostic centered-coordinate contract happens here, so no MediaPipe type
 * ever reaches main source.
 */
@Composable
fun FaceDetectorTrackingScreen(viewModel: FaceTrackingViewModel) {
    MviScreen(viewModel = viewModel, onEvent = { /* handled in MainActivity */ }) { state ->
        FaceDetectorTrackingContent(state = state, viewModel = viewModel)
    }
}

@Composable
private fun FaceDetectorTrackingContent(
    state: FaceTrackingState,
    viewModel: FaceTrackingViewModel
) {
    if (!state.headTrackingEnabled) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val tracker = remember {
        MediaPipeFaceDetectorTracker(
            context = context,
            onNoseTip = { keypoint ->
                viewModel.onDebugEngineUpdate(
                    centeredX = keypoint?.let { (FACE_DETECTOR_NEUTRAL_X - it.x()) * 2f * FACE_DETECTOR_AMPLITUDE_X },
                    centeredY = keypoint?.let { (FACE_DETECTOR_NEUTRAL_Y - it.y()) * 2f * FACE_DETECTOR_AMPLITUDE_Y },
                )
            }
        )
    }

    DisposableEffect(lifecycleOwner) {
        tracker.bind(lifecycleOwner, context)
        onDispose { tracker.close() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GazePointer(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = state.showError,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            FaceDetectorErrorBanner()
        }
    }
}

@Composable
private fun FaceDetectorErrorBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .background(
                color = Color(0xFFC00055),
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_error),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = stringResource(id = R.string.head_tracking_paused_message),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}
