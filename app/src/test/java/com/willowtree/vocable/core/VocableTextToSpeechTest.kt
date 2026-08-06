package com.willowtree.vocable.core

import android.speech.tts.TextToSpeech
import com.willowtree.vocable.core.VocableTextToSpeech.VoiceLabelInput
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

    // #643: Android's TTS `Voice` has no human-friendly name, so each voice is labeled by locale
    // plus an ordinal within that locale instead of a quality word — avoids interpolating an
    // en-dash-containing label into a spoken preview sample.

    @Test
    fun `single voice for a locale is numbered Voice 1`() {
        val result = VocableTextToSpeech.buildVoiceDisplayNames(
            listOf(VoiceLabelInput("voice_a", "English (United States)", quality = 400))
        )

        assertEquals(mapOf("voice_a" to "English (United States) Voice 1"), result)
    }

    @Test
    fun `voices sharing a locale are numbered by quality descending, then name`() {
        val result = VocableTextToSpeech.buildVoiceDisplayNames(
            listOf(
                VoiceLabelInput("voice_low", "English (United States)", quality = 200),
                VoiceLabelInput("voice_high", "English (United States)", quality = 500),
                VoiceLabelInput("voice_mid", "English (United States)", quality = 300)
            )
        )

        assertEquals(
            mapOf(
                "voice_high" to "English (United States) Voice 1",
                "voice_mid" to "English (United States) Voice 2",
                "voice_low" to "English (United States) Voice 3"
            ),
            result
        )
    }

    @Test
    fun `voices in different locales are numbered independently`() {
        val result = VocableTextToSpeech.buildVoiceDisplayNames(
            listOf(
                VoiceLabelInput("voice_us", "English (United States)", quality = 400),
                VoiceLabelInput("voice_gb", "English (United Kingdom)", quality = 400)
            )
        )

        assertEquals("English (United States) Voice 1", result.getValue("voice_us"))
        assertEquals("English (United Kingdom) Voice 1", result.getValue("voice_gb"))
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
        languageAvailability = { languageAvailability }
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

    /**
     * `languageAvailability` wraps a synchronous binder call into the TTS service, and this predicate
     * runs once per entry in `getVoices()` — several hundred on Google TTS — on the main thread for
     * every `speak()`. These two pin the short-circuiting so it can't be lost again to a refactor
     * that passes the value eagerly instead of the lambda.
     */
    private fun countingIsSupported(
        voiceLanguage: String? = "en",
        isNetworkConnectionRequired: Boolean = false
    ): Int {
        var calls = 0
        VocableTextToSpeech.isVoiceSupportedForLocale(
            voiceLanguage = voiceLanguage,
            targetLanguage = "en",
            isNetworkConnectionRequired = isNetworkConnectionRequired,
            languageAvailability = {
                calls++
                TextToSpeech.LANG_AVAILABLE
            }
        )
        return calls
    }

    @Test
    fun `language availability is not queried for a voice in another language`() {
        assertEquals(0, countingIsSupported(voiceLanguage = "fr"))
        assertEquals(0, countingIsSupported(voiceLanguage = null))
    }

    @Test
    fun `language availability is not queried for a network-required voice`() {
        assertEquals(0, countingIsSupported(isNetworkConnectionRequired = true))
    }

    @Test
    fun `language availability is queried once for an otherwise-eligible voice`() {
        assertEquals(1, countingIsSupported())
    }
}
