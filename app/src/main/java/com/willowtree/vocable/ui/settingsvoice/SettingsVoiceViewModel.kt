package com.willowtree.vocable.ui.settingsvoice

import com.willowtree.vocable.core.IVocableSharedPreferences
import com.willowtree.vocable.core.VocableTextToSpeech
import com.willowtree.vocable.ui.base.BaseViewModel

/** ViewModel for the Settings -> Voice screen. */
class SettingsVoiceViewModel(
    private val prefs: IVocableSharedPreferences
) : BaseViewModel<SettingsVoiceState, SettingsVoiceEvent>(
    SettingsVoiceState(activeVoiceDisplayName = resolveActiveVoiceDisplayName(prefs))
) {

    fun refreshActiveVoice() {
        updateState { copy(activeVoiceDisplayName = resolveActiveVoiceDisplayName(prefs)) }
    }

    fun onChangeVoice() {
        sendEvent(SettingsVoiceEvent.NavigateToChangeVoice)
    }

    fun onBack() {
        sendEvent(SettingsVoiceEvent.NavigateBack)
    }

    companion object {
        const val DEFAULT_VOICE_LABEL = "Default"

        private fun resolveActiveVoiceDisplayName(prefs: IVocableSharedPreferences): String =
            VocableTextToSpeech.getActiveVoiceDisplayName(prefs.getSelectedVoiceName()) ?: DEFAULT_VOICE_LABEL
    }
}