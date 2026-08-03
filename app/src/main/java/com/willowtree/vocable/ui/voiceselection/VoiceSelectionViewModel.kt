package com.willowtree.vocable.ui.voiceselection

import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.core.IVocableSharedPreferences
import com.willowtree.vocable.core.VocableTextToSpeech
import com.willowtree.vocable.ui.base.BaseViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class VoiceSelectionViewModel(
    private val sharedPreferences: IVocableSharedPreferences
) : BaseViewModel<VoiceSelectionState, VoiceSelectionEvent>(
    VoiceSelectionState(
        voices = VocableTextToSpeech.getAvailableVoices(),
        selectedVoiceName = sharedPreferences.getSelectedVoiceName()
    )
) {

    init {
        // TTS initializes asynchronously — reload voices once it's ready
        viewModelScope.launch {
            VocableTextToSpeech.isReady
                .filter { it }
                .collect { refreshVoices() }
        }

        viewModelScope.launch {
            VocableTextToSpeech.isSpeakingFlow.collect { isSpeaking ->
                if (!isSpeaking) {
                    updateState { copy(previewingVoiceName = null) }
                }
            }
        }
    }

    fun onVoiceSelected(voiceName: String?) {
        sharedPreferences.setSelectedVoiceName(voiceName)
        updateState { copy(selectedVoiceName = voiceName) }
        sendEvent(VoiceSelectionEvent.NavigateBack)
    }

    fun onDownloadVoice() {
        sendEvent(VoiceSelectionEvent.LaunchTtsSettings(VocableTextToSpeech.getCurrentEngine()))
    }

    /** Toggles reading [voice]'s own display name aloud in that voice — stops if it's already previewing. */
    fun onPreviewVoice(voice: VocableTextToSpeech.VoiceOption) {
        if (uiState.value.previewingVoiceName == voice.name) {
            VocableTextToSpeech.stop()
            updateState { copy(previewingVoiceName = null) }
        } else {
            updateState { copy(previewingVoiceName = voice.name) }
            VocableTextToSpeech.speak(voice.locale, voice.displayName, voice.name)
        }
    }

    fun refreshVoices() {
        updateState { copy(voices = VocableTextToSpeech.getAvailableVoices()) }
    }
}