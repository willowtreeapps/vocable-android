package com.willowtree.vocable.core

import android.view.Choreographer

/**
 * One-shot source of display-frame (vsync) callbacks. Exists so [FaceTrackingViewModel]'s
 * PID tick loop can be driven deterministically in JVM unit tests - [Choreographer] is
 * unavailable off-device - while production uses [ChoreographerFrameClock].
 */
interface FrameClock {
    /**
     * Invokes [onFrame] with the frame's timestamp (nanoseconds, [Choreographer]'s
     * `frameTimeNanos` clock) on the next display frame. One-shot: re-request from inside the
     * callback to keep ticking. Requesting again while a request is pending replaces the
     * pending callback rather than adding a second one.
     */
    fun requestFrame(onFrame: (frameTimeNanos: Long) -> Unit)

    /** Drops any pending request. */
    fun cancel()
}

/** Main-thread-only, like [Choreographer] itself. */
class ChoreographerFrameClock : FrameClock {
    private var pendingOnFrame: ((Long) -> Unit)? = null

    private val frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
        val onFrame = pendingOnFrame
        pendingOnFrame = null
        onFrame?.invoke(frameTimeNanos)
    }

    override fun requestFrame(onFrame: (frameTimeNanos: Long) -> Unit) {
        val alreadyPosted = pendingOnFrame != null
        pendingOnFrame = onFrame
        if (!alreadyPosted) {
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    override fun cancel() {
        pendingOnFrame = null
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }
}
