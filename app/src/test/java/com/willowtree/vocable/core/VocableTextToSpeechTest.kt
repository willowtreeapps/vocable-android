package com.willowtree.vocable.core

import android.speech.tts.TextToSpeech
import com.willowtree.vocable.core.VocableTextToSpeech.VoiceResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    // #642: a voice can still be listed by the engine after its data is uninstalled — only
    // flagged via KEY_FEATURE_NOT_INSTALLED, not removed. Name/locale matching alone can't tell
    // "installed" from "known but uninstalled."

    @Test
    fun `voice with no features is considered downloaded`() {
        assertTrue(VocableTextToSpeech.isVoiceDownloaded(features = null))
    }

    @Test
    fun `voice with unrelated features is considered downloaded`() {
        assertTrue(VocableTextToSpeech.isVoiceDownloaded(features = setOf("someOtherFeature")))
    }

    @Test
    fun `voice flagged not-installed is not considered downloaded`() {
        assertFalse(
            VocableTextToSpeech.isVoiceDownloaded(
                features = setOf(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            )
        )
    }

    // #618: the picker shows only voices actually installed and usable on-device — undownloaded
    // ones are hidden outright rather than offered with a download prompt. `isVoiceSupportedForLocale`
    // is the locale/network/language-data half of that filter; `isVoiceDownloaded` above is the
    // install half. Both must pass for a voice to reach the list.

    private fun isSupported(
        voiceLanguage: String? = "en",
        targetLanguage: String = "en",
        isNetworkConnectionRequired: Boolean = false,
        languageAvailability: Int = TextToSpeech.LANG_AVAILABLE
    ) = VocableTextToSpeech.isVoiceSupportedForLocale(
        voiceLanguage = voiceLanguage,
        targetLanguage = targetLanguage,
        isNetworkConnectionRequired = isNetworkConnectionRequired,
        languageAvailability = languageAvailability
    )

    @Test
    fun `local voice matching the target language with data present is supported`() {
        assertTrue(isSupported())
    }

    @Test
    fun `language match is case-insensitive`() {
        assertTrue(isSupported(voiceLanguage = "EN", targetLanguage = "en"))
    }

    @Test
    fun `country-specific language availability still counts as supported`() {
        assertTrue(isSupported(languageAvailability = TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE))
    }

    @Test
    fun `voice for a different language is not supported`() {
        assertFalse(isSupported(voiceLanguage = "fr", targetLanguage = "en"))
    }

    @Test
    fun `voice with no locale is not supported`() {
        assertFalse(isSupported(voiceLanguage = null))
    }

    /**
     * Network voices are excluded unconditionally, not just while offline: Vocable is an
     * offline/local-first app, so a voice that needs a connection can't be relied on mid-conversation.
     */
    @Test
    fun `network-required voice is not supported`() {
        assertFalse(isSupported(isNetworkConnectionRequired = true))
    }

    /**
     * The cross-reference that makes the hide-undownloaded filter trustworthy — `KEY_FEATURE_NOT_INSTALLED`
     * is inconsistently populated across OEM engines, so missing language data alone disqualifies a voice.
     */
    @Test
    fun `voice whose language data is missing is not supported`() {
        assertFalse(isSupported(languageAvailability = TextToSpeech.LANG_MISSING_DATA))
    }

    @Test
    fun `voice whose language is not supported by the engine is not supported`() {
        assertFalse(isSupported(languageAvailability = TextToSpeech.LANG_NOT_SUPPORTED))
    }
}
