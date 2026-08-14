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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.AugmentedFace
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.willowtree.vocable.R
import com.willowtree.vocable.ui.base.MviScreen
import com.willowtree.vocable.ui.components.GazePointer
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.ARCameraNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.rememberEngine
import timber.log.Timber
import java.util.EnumSet

/**
 * Entry point — wires MVI: collects state, renders content, no business logic here.
 */
@Composable
fun FaceTrackingScreen(viewModel: FaceTrackingViewModel) {
    // Events (Speak) are handled in MainActivity via MviScreen — nothing to handle here.
    val engine = rememberEngine()
    val cameraNode = rememberARCameraNode(engine)

    MviScreen(viewModel = viewModel, onEvent = { /* handled in MainActivity */ }) { state ->
        FaceTrackingContent(
            state = state,
            cameraNode = cameraNode,
            viewModel = viewModel
        )
    }
}

@Composable
private fun FaceTrackingContent(
    state: FaceTrackingState,
    cameraNode: ARCameraNode,
    viewModel: FaceTrackingViewModel
) {
    if (!state.headTrackingEnabled) return

    Box(modifier = Modifier.fillMaxSize()) {
        VocableARScene(cameraNode = cameraNode) { session, frame ->
            val faces = session.getAllTrackables(AugmentedFace::class.java)
            // displayOrientedPose: camera pose whose axes follow the display rotation, so the
            // ViewModel's camera-relative ray projection lands in screen-aligned coordinates.
            viewModel.onSceneUpdate(faces, frame.camera.displayOrientedPose)
        }

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
            ErrorBanner()
        }
    }
}

@Composable
private fun ErrorBanner() {
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

@Composable
fun VocableARScene(cameraNode: ARCameraNode, onSessionUpdated: (Session, Frame) -> Unit) {
    ARScene(
        modifier = Modifier.size(1.dp),
        isOpaque = false,
        sessionFeatures = EnumSet.of(Session.Feature.FRONT_CAMERA),
        onViewCreated = { setBackgroundColor(Color.Transparent.toArgb()) },
        sessionConfiguration = { session, config ->
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            config.focusMode = Config.FocusMode.AUTO
            config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
            session.configure(config)

            // Request 60fps camera capture where the hardware offers it - ARCore defaults to
            // 30fps, and the raw sample rate is the tracking pipeline's fidelity floor (the
            // PID tick runs at display refresh and interpolates whatever cadence arrives).
            // Degrades to the default config on devices without a 60fps option, and to 30fps
            // if ARCore rejects the change for session-state timing (setting cameraConfig
            // requires a paused session; sceneview's callback timing isn't contractual).
            runCatching {
                val sixtyFpsConfigs = session.getSupportedCameraConfigs(
                    CameraConfigFilter(session)
                        .setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_60))
                )
                if (sixtyFpsConfigs.isNotEmpty()) {
                    // The list varies by more than fps (capture resolution, GPU texture size),
                    // and its order is not contractual - blindly taking the head could change
                    // resolution as a side effect of requesting 60fps, which affects face-mesh
                    // quality and CPU load. Only the fps should change: prefer the config whose
                    // capture resolution matches what the session already chose.
                    val currentImageSize = session.cameraConfig.imageSize
                    session.cameraConfig = sixtyFpsConfigs
                        .firstOrNull { it.imageSize == currentImageSize }
                        ?: sixtyFpsConfigs.first()
                }
            }.onFailure { Timber.w(it, "60fps camera config not applied; staying on default") }
        },
        onSessionUpdated = { session, frame -> onSessionUpdated(session, frame) },
        cameraNode = cameraNode,
        planeRenderer = false,
        cameraStream = null,
    )
}
