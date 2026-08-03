package com.willowtree.vocable.core

import com.willowtree.vocable.core.VocableTextToSpeech.VoiceResolution
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers [VocableTextToSpeech.resolveVoiceSelection], the pure decision logic behind the
 * live device-voice fallback / explicit-pick persistence / stale-selection recovery contract
 * (see #632). The rest of [VocableTextToSpeech] wraps `android.speech.tts.TextToSpeech`/`Voice`,
 * which can't be constructed under the plain Android SDK stub jar this module's unit tests run
 * against (no Robolectric, no mocking framework, per this repo's testing conventions) — that
 * behavior is verified manually/on-emulator instead.
 */
class VocableTextToSpeechTest {

    @Test
    fun `no explicit selection resolves to the live device default`() {
        val result = VocableTextToSpeech.resolveVoiceSelection(
            selectedVoiceName = null,
            availableVoiceNames = setOf("en-us-x-tpd-local")
        )

        assertEquals(VoiceResolution.LIVE_DEFAULT, result)
    }

    @Test
    fun `blank explicit selection resolves to the live device default`() {
        val result = VocableTextToSpeech.resolveVoiceSelection(
            selectedVoiceName = "",
            availableVoiceNames = setOf("en-us-x-tpd-local")
        )

        assertEquals(VoiceResolution.LIVE_DEFAULT, result)
    }

    @Test
    fun `explicit selection that is still installed resolves to explicit`() {
        val result = VocableTextToSpeech.resolveVoiceSelection(
            selectedVoiceName = "en-us-x-tpd-local",
            availableVoiceNames = setOf("en-us-x-tpd-local", "en-us-x-tpf-local")
        )

        assertEquals(VoiceResolution.EXPLICIT, result)
    }

    @Test
    fun `explicit selection no longer installed falls back to the live device default`() {
        val result = VocableTextToSpeech.resolveVoiceSelection(
            selectedVoiceName = "en-us-x-removed-voice",
            availableVoiceNames = setOf("en-us-x-tpd-local", "en-us-x-tpf-local")
        )

        assertEquals(VoiceResolution.STALE_FALLBACK_TO_LIVE_DEFAULT, result)
    }

    @Test
    fun `explicit selection falls back when no voices are available at all`() {
        val result = VocableTextToSpeech.resolveVoiceSelection(
            selectedVoiceName = "en-us-x-tpd-local",
            availableVoiceNames = emptySet()
        )

        assertEquals(VoiceResolution.STALE_FALLBACK_TO_LIVE_DEFAULT, result)
    }
}
