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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * Note on coverage: [com.willowtree.vocable.core.VocableTextToSpeech] is a global object wrapping
 * the real Android `TextToSpeech` engine, so under plain JVM unit tests it never initializes and
 * `getAvailableVoices()` always returns an empty list. The voice-list side of this ViewModel
 * therefore can't be asserted against real data here — it would need an injected seam on the
 * ViewModel, which is deliberately out of scope for #636. Selection and persistence are fully
 * covered; the voice list is only covered for its empty/no-crash path.
 */
class VoiceSelectionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val voice = VocableTextToSpeech.VoiceOption("voice_1", "English (United States) – Enhanced", Locale.US)

    private fun createViewModel(
        prefs: FakeVocableSharedPreferences = FakeVocableSharedPreferences()
    ): Pair<VoiceSelectionViewModel, FakeVocableSharedPreferences> =
        VoiceSelectionViewModel(prefs) to prefs

    @Test
    fun `initial state reads the persisted voice name`() = runTest {
        val (viewModel, _) = createViewModel(
            FakeVocableSharedPreferences(selectedVoiceName = "en-us-x-sfg#male_1-local")
        )

        assertEquals("en-us-x-sfg#male_1-local", viewModel.uiState.value.selectedVoiceName)
    }

    @Test
    fun `initial state has no selection when nothing is persisted`() = runTest {
        val (viewModel, _) = createViewModel()

        assertNull(viewModel.uiState.value.selectedVoiceName)
    }

    @Test
    fun `onVoiceSelected persists the chosen voice`() = runTest {
        val (viewModel, prefs) = createViewModel()

        viewModel.onVoiceSelected("en-us-x-tpf-local")

        assertEquals("en-us-x-tpf-local", prefs.getSelectedVoiceName())
    }

    @Test
    fun `onVoiceSelected updates state`() = runTest {
        val (viewModel, _) = createViewModel()

        viewModel.onVoiceSelected("en-us-x-tpf-local")

        assertEquals("en-us-x-tpf-local", viewModel.uiState.value.selectedVoiceName)
    }

    @Test
    fun `onVoiceSelected emits navigate back`() = runTest {
        val (viewModel, _) = createViewModel()

        viewModel.event.test {
            viewModel.onVoiceSelected("en-us-x-tpf-local")
            assertEquals(VoiceSelectionEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `selecting a different voice replaces the previous selection`() = runTest {
        val (viewModel, prefs) = createViewModel(
            FakeVocableSharedPreferences(selectedVoiceName = "en-us-x-sfg#male_1-local")
        )

        viewModel.onVoiceSelected("en-us-x-tpf-local")

        assertEquals("en-us-x-tpf-local", prefs.getSelectedVoiceName())
        assertEquals("en-us-x-tpf-local", viewModel.uiState.value.selectedVoiceName)
    }

    /**
     * The Change Voice screen no longer offers a "Default" row (#636), but null remains a valid
     * selection: the stale-voice revert in `MainActivity`/`PresetsViewModel`/`KeyboardViewModel`
     * clears the preference when a persisted voice stops resolving. Pinned so removing the UI
     * affordance doesn't invite deleting the null path too.
     */
    @Test
    fun `onVoiceSelected with null clears the persisted selection`() = runTest {
        val (viewModel, prefs) = createViewModel(
            FakeVocableSharedPreferences(selectedVoiceName = "en-us-x-sfg#male_1-local")
        )

        viewModel.onVoiceSelected(null)

        assertNull(prefs.getSelectedVoiceName())
        assertNull(viewModel.uiState.value.selectedVoiceName)
    }

    @Test
    fun `refreshVoices is safe when the TTS engine is unavailable`() = runTest {
        val (viewModel, _) = createViewModel()

        viewModel.refreshVoices()

        assertTrue(viewModel.uiState.value.voices.isEmpty())
    }

    @Test
    fun `refreshVoices preserves the current selection`() = runTest {
        val (viewModel, _) = createViewModel(
            FakeVocableSharedPreferences(selectedVoiceName = "en-us-x-sfg#male_1-local")
        )

        viewModel.refreshVoices()

        assertEquals("en-us-x-sfg#male_1-local", viewModel.uiState.value.selectedVoiceName)
    }

    @Test
    fun `onDownloadVoice emits launch tts settings event`() = runTest {
        val (viewModel, _) = createViewModel()

        viewModel.event.test {
            viewModel.onDownloadVoice()
            assertEquals(VoiceSelectionEvent.LaunchTtsSettings(null), awaitItem())
        }
    }

    @Test
    fun `onPreviewVoice marks the voice as previewing`() = runTest {
        val (viewModel, _) = createViewModel()

        viewModel.onPreviewVoice(voice)

        assertEquals(voice.name, viewModel.uiState.value.previewingVoiceName)
    }

    @Test
    fun `onPreviewVoice again for the same voice stops the preview`() = runTest {
        val (viewModel, _) = createViewModel()

        viewModel.onPreviewVoice(voice)
        viewModel.onPreviewVoice(voice)

        assertNull(viewModel.uiState.value.previewingVoiceName)
    }
}
