package com.willowtree.vocable.ui.modifiers

import android.graphics.Rect
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.willowtree.vocable.core.ComposeGazeTarget
import com.willowtree.vocable.core.GazeInteractionManager
import com.willowtree.vocable.core.VocableTextToSpeech
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Fallback hold duration for buttons that don't trigger TTS. */
private const val NON_SPEAKING_RESET_DELAY_MS = 500L

fun Modifier.gazeClickable(
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    dwellTime: Long = 1000L,
    accessibilityLabel: String? = null
): Modifier = composed {
    val scope = rememberCoroutineScope()

    val currentOnClick by rememberUpdatedState(onClick)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentAccessibilityLabel by rememberUpdatedState(accessibilityLabel)

    var currentTarget by remember { mutableStateOf<ComposeGazeTarget?>(null) }
    var dwellJob by remember { mutableStateOf<Job?>(null) }
    var activeHover by remember { mutableStateOf<HoverInteraction.Enter?>(null) }

    // True from the moment onClick fires until the reset completes.
    // onExit must NOT cancel the job or reset state while this is true.
    var isSelected by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            currentTarget?.let { GazeInteractionManager.unregister(it) }
            dwellJob?.cancel()
            activeHover?.let { hover ->
                scope.launch { interactionSource.emit(HoverInteraction.Exit(hover)) }
            }
            GazeInteractionManager.updateDwellProgress(0f)
        }
    }

    Modifier.onGloballyPositioned { coordinates ->
        currentTarget?.let {
            GazeInteractionManager.unregister(it)
            currentTarget = null
        }

        if (!currentEnabled) return@onGloballyPositioned

        val bounds = coordinates.boundsInWindow()
        val androidRect = Rect(
            bounds.left.roundToInt(),
            bounds.top.roundToInt(),
            bounds.right.roundToInt(),
            bounds.bottom.roundToInt()
        )

        val newTarget = ComposeGazeTarget(
            bounds = androidRect,
            onEnter = {
                // If we're mid post-selection hold, ignore re-entry until it self-resets.
                if (isSelected) return@ComposeGazeTarget

                dwellJob?.cancel()
                dwellJob = scope.launch {
                    // ── Phase 1: Hover ── amber outline on button, arc fills up
                    val hover = HoverInteraction.Enter()
                    activeHover = hover
                    interactionSource.emit(hover)

                    animate(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = dwellTime.toInt(), easing = LinearEasing)
                    ) { value, _ ->
                        GazeInteractionManager.updateDwellProgress(value)
                    }

                    // Amber outline off → button turns green
                    interactionSource.emit(HoverInteraction.Exit(hover))
                    activeHover = null

                    // ── Phase 2: Selected ── green button + complete arc
                    isSelected = true
                    val press = PressInteraction.Press(Offset.Zero)
                    interactionSource.emit(press)
                    currentOnClick()

                    // ── Wait for reset signal ──
                    if (currentAccessibilityLabel != null) {
                        // Speaking button: stay green until TTS finishes
                        VocableTextToSpeech.isSpeakingFlow.first { it }   // wait for start
                        VocableTextToSpeech.isSpeakingFlow.first { !it }  // wait for finish
                    } else {
                        // Non-speaking button: short visual confirmation then reset
                        delay(NON_SPEAKING_RESET_DELAY_MS)
                    }

                    // ── Phase 3: Null state ── release press and reset arc
                    interactionSource.emit(PressInteraction.Release(press))
                    GazeInteractionManager.updateDwellProgress(0f)
                    isSelected = false
                }
            },
            onExit = {
                // If a selection is in progress, let it finish — reset is driven above.
                if (isSelected) return@ComposeGazeTarget

                dwellJob?.cancel()
                dwellJob = null
                GazeInteractionManager.updateDwellProgress(0f)

                val hover = activeHover
                if (hover != null) {
                    activeHover = null
                    scope.launch {
                        interactionSource.emit(HoverInteraction.Exit(hover))
                    }
                }
            },
            accessibilityLabel = currentAccessibilityLabel
        )

        GazeInteractionManager.register(newTarget)
        currentTarget = newTarget
    }
}
