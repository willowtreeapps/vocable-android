package com.willowtree.vocable.ui.settingsvoice

import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.core.IVocableSharedPreferences
import com.willowtree.vocable.core.VocableTextToSpeech
import com.willowtree.vocable.ui.base.BaseViewModel
import kotlinx.coroutines.launch

/** ViewModel for the Settings -> Voice screen. */
class SettingsVoiceViewModel(
    private val prefs: IVocableSharedPreferences
) : BaseViewModel<SettingsVoiceState, SettingsVoiceEvent>(
    SettingsVoiceState(activeVoiceDisplayName = resolveActiveVoiceDisplayName(prefs))
) {

    init {
        viewModelScope.launch {
            VocableTextToSpeech.isSpeakingFlow.collect { isSpeaking ->
                if (!isSpeaking) updateState { copy(isPreviewPlaying = false) }
            }
        }
    }

    fun refreshActiveVoice() {
        updateState { copy(activeVoiceDisplayName = resolveActiveVoiceDisplayName(prefs)) }
    }

    /** Toggles reading the active voice's own display name aloud in that voice — stops if already previewing. */
    fun onPreviewActiveVoice() {
        if (uiState.value.isPreviewPlaying) {
            VocableTextToSpeech.stop()
            updateState { copy(isPreviewPlaying = false) }
        } else {
            updateState { copy(isPreviewPlaying = true) }
            VocableTextToSpeech.speak(
                locale = null,
                text = uiState.value.activeVoiceDisplayName,
                selectedVoiceName = prefs.getSelectedVoiceName()
            )
        }
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
