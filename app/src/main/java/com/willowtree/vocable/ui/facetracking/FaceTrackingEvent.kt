package com.willowtree.vocable.ui.facetracking

sealed interface FaceTrackingEvent {
    data class Speak(val text: String) : FaceTrackingEvent
}