package com.willowtree.vocable.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.willowtree.vocable.ui.facetracking.FaceTrackingViewModel
import com.willowtree.vocable.ui.theme.ColorAccent
import com.willowtree.vocable.ui.theme.SelectedColor
import com.willowtree.vocable.core.GazeInteractionManager
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private const val POINTER_RADIUS = 32f

/**
 * Displays a gaze pointer on screen, and handles hit-testing and dwell logic to trigger button clicks.
 *
 * The pointer position is determined by the [FaceTrackingViewModel]'s [FaceTrackingViewModel.adjustedVector],
 * which applies smoothing and calibration to the raw face tracking data. This is converted into screen
 * coordinates within this composable, which also tracks its own layout position/size for bounds checking.
 *
 * When the pointer moves, we check for intersection with any buttons in the hierarchy (via [FaceTrackingViewModel.intersect])
 * and update the hover state accordingly (via [FaceTrackingViewModel.handleHover]). The hover state is used by buttons to
 * determine when to start/stop dwell progress, which is observed here to trigger redraws of the pointer arc.
 */
@Composable
fun GazePointer(
    viewModel: FaceTrackingViewModel,
    modifier: Modifier = Modifier
) {
    val vectorPosition by viewModel.adjustedVector.collectAsState()
    var pointerOffset by remember { mutableStateOf(Offset.Zero) }

    var windowOffsetY by remember { mutableFloatStateOf(0f) }
    var layoutWidthPx by remember { mutableFloatStateOf(0f) }
    var layoutHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        snapshotFlow { vectorPosition }
            .map { v ->
                v?.let {
                    val offset = viewModel.convertCoordSystems(it, layoutHeightPx, layoutWidthPx)
                    pointerOffset = offset
                    // Shift Y into window coords for hit-test against boundsInWindow() rects
                    Offset(offset.x, offset.y + windowOffsetY)
                }
            }
            .map { windowOffset -> windowOffset?.let { viewModel.intersect(it) } }
            .distinctUntilChanged()
            .collect { target -> viewModel.handleHover(target) }
    }

    val dwellProgress by GazeInteractionManager.dwellProgress.collectAsState()

    GazePointerCanvas(
        offset = pointerOffset,
        dwellProgress = dwellProgress,
        modifier = modifier.onGloballyPositioned { coords ->
            val bounds = coords.boundsInWindow()
            windowOffsetY = bounds.top
            layoutWidthPx = bounds.width
            layoutHeightPx = bounds.height
        }
    )
}

/**
 * Pure drawing composable — no ViewModel dependency, easy to preview/test.
 *
 * - Amber filled circle: always visible.
 * - Green arc overlay: appears while [dwellProgress] > 0f, sweeps clockwise during dwell,
 *   stays complete (1f) while the selected button is green, then disappears when reset to 0f.
 */
@Composable
fun GazePointerCanvas(
    offset: Offset,
    dwellProgress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val x = offset.x.coerceIn(POINTER_RADIUS, size.width - POINTER_RADIUS)
        val y = offset.y.coerceIn(POINTER_RADIUS, size.height - POINTER_RADIUS)

        drawCircle(
            color = ColorAccent,
            radius = POINTER_RADIUS,
            center = Offset(x, y)
        )

        if (dwellProgress > 0f) {
            drawArc(
                color = SelectedColor,
                startAngle = -90f,
                sweepAngle = dwellProgress * 360f,
                useCenter = false,
                topLeft = Offset(x - POINTER_RADIUS, y - POINTER_RADIUS),
                size = Size(POINTER_RADIUS * 2, POINTER_RADIUS * 2),
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }
}
