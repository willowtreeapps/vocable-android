package com.willowtree.vocable.utils

import com.willowtree.vocable.core.FrameClock

/**
 * Deterministic [FrameClock]: tests advance display frames explicitly via [advanceFrame]
 * instead of waiting on a real Choreographer.
 */
class FakeFrameClock : FrameClock {
    private var pendingOnFrame: ((Long) -> Unit)? = null

    var frameTimeNanos = 0L
        private set

    val hasPendingFrame: Boolean
        get() = pendingOnFrame != null

    override fun requestFrame(onFrame: (frameTimeNanos: Long) -> Unit) {
        pendingOnFrame = onFrame
    }

    override fun cancel() {
        pendingOnFrame = null
    }

    /** Fires the pending frame callback (if any) [byNanos] after the previous frame. */
    fun advanceFrame(byNanos: Long = SIXTY_HZ_FRAME_NANOS) {
        frameTimeNanos += byNanos
        val onFrame = pendingOnFrame ?: return
        pendingOnFrame = null
        onFrame(frameTimeNanos)
    }

    companion object {
        const val SIXTY_HZ_FRAME_NANOS = 16_666_667L
    }
}
