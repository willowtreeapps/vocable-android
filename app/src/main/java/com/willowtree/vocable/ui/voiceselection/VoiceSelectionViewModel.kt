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

    /**
     * Deep-links to the OS's TTS voice-data installer. Currently unreachable from the UI: #618 hides
     * undownloaded voices from the picker entirely (per iOS parity), so no row can trigger a
     * download anymore. Retained deliberately — the call there was to hide the undownloaded voices
     * "for now" without tearing out the plumbing, so restoring the affordance stays a one-line
     * change. Don't delete as dead code without checking #618 first.
     */
    fun onDownloadVoice() {
        sendEvent(VoiceSelectionEvent.LaunchTtsSettings(VocableTextToSpeech.getCurrentEngine()))
    }

    /**
     * Toggles playing [voice]'s preview sample aloud in that voice — stops if it's already
     * previewing. [sampleFormat] is `voice_preview_sample` (e.g. `"Hello, this is Vocable, %1$s."`),
     * resolved by the caller so this stays free of Android resource access; [voice]'s display name
     * (not its raw engine identifier) is substituted into it.
     */
    fun onPreviewVoice(voice: VocableTextToSpeech.VoiceOption, sampleFormat: String) {
        if (uiState.value.previewingVoiceName == voice.name) {
            VocableTextToSpeech.stop()
            updateState { copy(previewingVoiceName = null) }
        } else {
            updateState { copy(previewingVoiceName = voice.name) }
            VocableTextToSpeech.speak(voice.locale, sampleFormat.format(voice.displayName), voice.name)
        }
    }

    fun refreshVoices() {
        updateState { copy(voices = VocableTextToSpeech.getAvailableVoices()) }
    }
}