package com.willowtree.vocable.ui.voiceselection

import com.willowtree.vocable.core.VocableTextToSpeech

data class VoiceSelectionState(
    val voices: List<VocableTextToSpeech.VoiceOption> = emptyList(),
    val selectedVoiceName: String? = null
)