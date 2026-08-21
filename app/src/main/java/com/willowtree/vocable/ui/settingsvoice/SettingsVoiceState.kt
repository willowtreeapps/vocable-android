package com.willowtree.vocable.ui.settingsvoice

/** State for the Settings -> Voice screen. */
data class SettingsVoiceState(
    val activeVoiceDisplayName: String,
    val isPreviewPlaying: Boolean = false
)
