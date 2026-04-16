package com.willowtree.vocable.ui.modifiers

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.horizontalPageSwipe(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
): Modifier = pointerInput(onSwipeLeft, onSwipeRight) {
    var totalDrag = 0f
    var hasFired = false
    detectHorizontalDragGestures(
        onDragEnd = { totalDrag = 0f; hasFired = false },
        onDragCancel = { totalDrag = 0f; hasFired = false }
    ) { _, dragAmount ->
        if (hasFired) return@detectHorizontalDragGestures
        totalDrag += dragAmount
        when {
            totalDrag < -100f -> { onSwipeRight(); hasFired = true }
            totalDrag > 100f -> { onSwipeLeft(); hasFired = true }
        }
    }
}
