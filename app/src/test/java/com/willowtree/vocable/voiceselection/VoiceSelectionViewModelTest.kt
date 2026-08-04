package com.willowtree.vocable.voiceselection

import app.cash.turbine.test
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.core.VocableTextToSpeech
import com.willowtree.vocable.ui.voiceselection.VoiceSelectionEvent
import com.willowtree.vocable.ui.voiceselection.VoiceSelectionViewModel
import com.willowtree.vocable.utils.FakeVocableSharedPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class VoiceSelectionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(selectedVoiceName: String? = null): VoiceSelectionViewModel =
        VoiceSelectionViewModel(FakeVocableSharedPreferences(selectedVoiceName = selectedVoiceName))

    private val voice = VocableTextToSpeech.VoiceOption("voice_1", "English (United States) – Enhanced", Locale.US)

    @Test
    fun `onVoiceSelected persists selection, updates state, and navigates back`() = runTest {
        val viewModel = createViewModel()

        viewModel.event.test {
            viewModel.onVoiceSelected(voice.name)
            assertEquals(VoiceSelectionEvent.NavigateBack, awaitItem())
        }

        assertEquals(voice.name, viewModel.uiState.value.selectedVoiceName)
    }

    @Test
    fun `onDownloadVoice emits launch tts settings event`() = runTest {
        val viewModel = createViewModel()

        viewModel.event.test {
            viewModel.onDownloadVoice()
            assertEquals(VoiceSelectionEvent.LaunchTtsSettings(null), awaitItem())
        }
    }

    @Test
    fun `onPreviewVoice marks the voice as previewing`() = runTest {
        val viewModel = createViewModel()

        viewModel.onPreviewVoice(voice)

        assertEquals(voice.name, viewModel.uiState.value.previewingVoiceName)
    }

    @Test
    fun `onPreviewVoice again for the same voice stops the preview`() = runTest {
        val viewModel = createViewModel()

        viewModel.onPreviewVoice(voice)
        viewModel.onPreviewVoice(voice)

        assertNull(viewModel.uiState.value.previewingVoiceName)
    }
}
