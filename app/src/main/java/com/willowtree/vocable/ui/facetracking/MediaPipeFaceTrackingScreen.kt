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
import com.willowtree.vocable.core.MediaPipeFaceTracker
import com.willowtree.vocable.ui.base.MviScreen
import com.willowtree.vocable.ui.components.GazePointer

/**
 * Prototype (#676) alternative to FaceTrackingScreen.kt: MediaPipe FaceDetector + CameraX instead
 * of ARCore. Same downstream GazePointer/error-banner UI, gated on the same headTrackingEnabled
 * state - only the tracking source differs.
 */
@Composable
fun MediaPipeFaceTrackingScreen(viewModel: FaceTrackingViewModel) {
    MviScreen(viewModel = viewModel, onEvent = { /* handled in MainActivity */ }) { state ->
        MediaPipeFaceTrackingContent(state = state, viewModel = viewModel)
    }
}

@Composable
private fun MediaPipeFaceTrackingContent(
    state: FaceTrackingState,
    viewModel: FaceTrackingViewModel
) {
    if (!state.headTrackingEnabled) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val tracker = remember {
        MediaPipeFaceTracker(context = context, onHeadForward = viewModel::onMediaPipeUpdate)
    }

    DisposableEffect(lifecycleOwner) {
        tracker.bind(lifecycleOwner, context)
        onDispose { tracker.close() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GazePointer(
            viewModel = viewModel,
            useBounceSelection = true,
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = state.showError,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            MediaPipeErrorBanner()
        }
    }
}

@Composable
private fun MediaPipeErrorBanner() {
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
