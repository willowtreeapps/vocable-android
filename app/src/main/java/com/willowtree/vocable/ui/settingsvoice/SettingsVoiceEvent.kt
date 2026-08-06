package com.willowtree.vocable.ui.settingsvoice

/** Events that the Settings -> Voice screen can send to its parent. */
sealed interface SettingsVoiceEvent {
    data object NavigateBack : SettingsVoiceEvent
    data object NavigateToChangeVoice : SettingsVoiceEvent
}