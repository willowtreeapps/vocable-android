package com.willowtree.vocable.core

import android.graphics.Rect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
