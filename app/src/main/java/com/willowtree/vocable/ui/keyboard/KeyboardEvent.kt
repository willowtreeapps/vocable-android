package com.willowtree.vocable.ui.keyboard

/** Events that the Keyboard screen can send to its parent. */
sealed interface KeyboardEvent {
    data class ShowToast(val message: String) : KeyboardEvent
    object NavigateBack : KeyboardEvent
}