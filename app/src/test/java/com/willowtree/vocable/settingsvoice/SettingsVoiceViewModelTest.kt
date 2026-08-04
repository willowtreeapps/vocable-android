package com.willowtree.vocable.settingsvoice

import app.cash.turbine.test
import com.willowtree.vocable.MainDispatcherRule
import com.willowtree.vocable.ui.settingsvoice.SettingsVoiceEvent
import com.willowtree.vocable.ui.settingsvoice.SettingsVoiceViewModel
import com.willowtree.vocable.utils.FakeVocableSharedPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * `VocableTextToSpeech` can't resolve a real voice under this module's plain-SDK-stub unit test
 * environment (no engine ever gets initialized), so every case here observes the documented
 * fallback label rather than a resolved voice name — the live-resolution logic itself is covered
 * by `VocableTextToSpeechTest`.
 */
class SettingsVoiceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(selectedVoiceName: String? = null): SettingsVoiceViewModel =
        SettingsVoiceViewModel(FakeVocableSharedPreferences(selectedVoiceName = selectedVoiceName))

    @Test
    fun `initial state falls back to default label when no voice can be resolved`() = runTest {
        val viewModel = createViewModel(selectedVoiceName = "en-us-x-sfg#male_1-local")

        assertEquals(SettingsVoiceViewModel.DEFAULT_VOICE_LABEL, viewModel.uiState.value.activeVoiceDisplayName)
    }

    @Test
    fun `refreshActiveVoice keeps the fallback label when no voice can be resolved`() = runTest {
        val viewModel = createViewModel()

        viewModel.refreshActiveVoice()

        assertEquals(SettingsVoiceViewModel.DEFAULT_VOICE_LABEL, viewModel.uiState.value.activeVoiceDisplayName)
    }

    @Test
    fun `onChangeVoice emits navigate event`() = runTest {
        val viewModel = createViewModel()

        viewModel.event.test {
            viewModel.onChangeVoice()
            assertEquals(SettingsVoiceEvent.NavigateToChangeVoice, awaitItem())
        }
    }

    @Test
    fun `onBack emits navigate back event`() = runTest {
        val viewModel = createViewModel()

        viewModel.event.test {
            viewModel.onBack()
            assertEquals(SettingsVoiceEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `onPreviewActiveVoice marks the preview as playing`() = runTest {
        val viewModel = createViewModel()

        viewModel.onPreviewActiveVoice()

        assertTrue(viewModel.uiState.value.isPreviewPlaying)
    }

    @Test
    fun `onPreviewActiveVoice again stops the preview`() = runTest {
        val viewModel = createViewModel()

        viewModel.onPreviewActiveVoice()
        viewModel.onPreviewActiveVoice()

        assertFalse(viewModel.uiState.value.isPreviewPlaying)
    }
}
