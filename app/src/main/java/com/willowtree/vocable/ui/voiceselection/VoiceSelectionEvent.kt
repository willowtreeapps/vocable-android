package com.willowtree.vocable.ui.voiceselection

sealed interface VoiceSelectionEvent {
    data object NavigateBack : VoiceSelectionEvent
    data class LaunchTtsSettings(val enginePackage: String?) : VoiceSelectionEvent
}