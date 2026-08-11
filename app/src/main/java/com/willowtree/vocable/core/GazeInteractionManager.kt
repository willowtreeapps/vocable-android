package com.willowtree.vocable.core

import android.graphics.Rect
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class ComposeGazeTarget(
    val bounds: Rect,
    val onEnter: () -> Unit,
    val onExit: () -> Unit,
    val accessibilityLabel: String? = null
)

/**
 * Holds the current dwell progress [0f, 1f] for the active gaze target.
 * - 0f means idle / no target hovered.
 * - Values between 0f and 1f mean dwelling is in progress.
 * - 1f means dwell completed (click fired); the pointer shows the completed arc until reset.
 * Reset back to 0f happens after the post-selection fade delay.
 */
object GazeInteractionManager {
    private val targets = mutableListOf<ComposeGazeTarget>()

    private val _dwellProgress = MutableStateFlow(0f)
    val dwellProgress: StateFlow<Float> = _dwellProgress.asStateFlow()

    // Prototype (#676): a one-shot "a selection just fired" event, distinct from dwellProgress.
    // dwellProgress is a single value shared by every target and stays at 1f for whichever
    // target is in its post-click hold (which can last seconds, gated on TTS) - it's "is some
    // button currently holding," not "did a selection just happen." The bounce-select pointer
    // needs the latter so it doesn't stay bounced/re-trigger just because the cursor moved to a
    // new tile while an old tile's TTS-gated hold was still in progress.
    private val _selectionEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val selectionEvents: SharedFlow<Unit> = _selectionEvents.asSharedFlow()

    fun emitSelection() {
        _selectionEvents.tryEmit(Unit)
    }

    fun register(target: ComposeGazeTarget) {
        targets.add(target)
    }

    fun unregister(target: ComposeGazeTarget) {
        targets.remove(target)
    }

    fun getTargets(): List<ComposeGazeTarget> = targets.toList()

    fun updateDwellProgress(progress: Float) {
        _dwellProgress.value = progress
    }
}
