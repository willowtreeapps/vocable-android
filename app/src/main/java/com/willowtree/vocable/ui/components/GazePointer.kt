package com.willowtree.vocable.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
    useBounceSelection: Boolean = false,
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

    // Bounce trigger is a distinct one-shot event, not derived from dwellProgress - see
    // GazeInteractionManager.selectionEvents doc for why dwellProgress==1f alone isn't a safe
    // "just selected" signal (it stays 1f for whichever button is in its post-click hold).
    var selectionPulse by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        GazeInteractionManager.selectionEvents.collect { selectionPulse++ }
    }

    GazePointerCanvas(
        offset = pointerOffset,
        dwellProgress = dwellProgress,
        useBounceSelection = useBounceSelection,
        selectionPulse = selectionPulse,
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
 * Default ([useBounceSelection] = false, ARCore path): amber filled circle, plus a green arc
 * overlay while [dwellProgress] > 0f that sweeps clockwise during dwell, stays complete (1f)
 * while the selected button is green, then disappears when reset to 0f.
 *
 * [useBounceSelection] = true (MediaPipe prototype, #676): no arc during dwell at all - the
 * cursor just pops with a quick scale bounce each time [selectionPulse] changes (a selection
 * just fired), matching iOS's snappy no-visible-wait feedback instead of a loading-style
 * progress sweep.
 */
@Composable
fun GazePointerCanvas(
    offset: Offset,
    dwellProgress: Float,
    useBounceSelection: Boolean = false,
    selectionPulse: Int = 0,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }

    if (useBounceSelection) {
        LaunchedEffect(selectionPulse) {
            if (selectionPulse > 0) {
                scale.snapTo(1f)
                scale.animateTo(1.5f, animationSpec = tween(durationMillis = 100))
                scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
        }
    }

    Canvas(modifier = modifier) {
        val x = offset.x.coerceIn(POINTER_RADIUS, size.width - POINTER_RADIUS)
        val y = offset.y.coerceIn(POINTER_RADIUS, size.height - POINTER_RADIUS)

        if (useBounceSelection) {
            drawCircle(
                color = ColorAccent,
                radius = POINTER_RADIUS * scale.value,
                center = Offset(x, y)
            )
        } else {
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
}
