package com.willowtree.vocable.core

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.Locale

object VocableTextToSpeech {

    data class VoiceOption(
        val name: String,
        val displayName: String,
        val locale: Locale,
        val isDownloaded: Boolean = true
    )

    private var textToSpeech: TextToSpeech? = null
    private var lastSetLocale: Locale? = null

    private val liveIsSpeaking = MutableLiveData<Boolean>()
    val isSpeaking: LiveData<Boolean> = liveIsSpeaking

    private val _isSpeakingFlow = MutableStateFlow(false)
    val isSpeakingFlow: StateFlow<Boolean> = _isSpeakingFlow.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    fun initialize(context: Context) {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(context) { status ->
                Timber.d("VocableTextToSpeech initialized with status: $status")
                _isReady.value = status == TextToSpeech.SUCCESS
            }.apply {
                setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onDone(utteranceId: String?) {
                        liveIsSpeaking.postValue(false)
                        _isSpeakingFlow.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Timber.e("VocableTextToSpeech onError: utteranceId=$utteranceId")
                        liveIsSpeaking.postValue(false)
                        _isSpeakingFlow.value = false
                    }

                    override fun onStart(utteranceId: String?) {
                        liveIsSpeaking.postValue(true)
                        _isSpeakingFlow.value = true
                    }
                })
            }
        }
    }

    fun shutdown() {
        textToSpeech?.let {
            it.stop()
            it.shutdown()
        }
        textToSpeech = null
        lastSetLocale = null
        _isSpeakingFlow.value = false
        _isReady.value = false
    }

    /** Halts in-progress speech (e.g. a voice preview) without tearing down the engine like [shutdown]. */
    fun stop() {
        textToSpeech?.let {
            it.stop()
            liveIsSpeaking.postValue(false)
        }
        _isSpeakingFlow.value = false
    }

    fun getAvailableVoices(locale: Locale = Locale.getDefault()): List<VoiceOption> {
        val tts = textToSpeech ?: return emptyList()
        val availableVoices = tts.voices ?: return emptyList()

        val matchingVoices = availableVoices.filter { voice ->
            val voiceLocale = voice.locale ?: return@filter false
            voiceLocale.language.equals(locale.language, ignoreCase = true) &&
                !voice.isNetworkConnectionRequired
        }

        val displayNamesByVoiceName = buildVoiceDisplayNames(
            matchingVoices.map { VoiceLabelInput(it.name, it.locale.displayName, it.quality) }
        )

        return matchingVoices
            .sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.name })
            .map { voice ->
                VoiceOption(
                    name = voice.name,
                    displayName = displayNamesByVoiceName.getValue(voice.name),
                    locale = voice.locale,
                    isDownloaded = isVoiceDownloaded(voice)
                )
            }
    }

    fun getCurrentEngine(): String? = textToSpeech?.defaultEngine

    /**
     * Resolves the display name of whichever voice is actually active right now — [selectedVoiceName]
     * if it still resolves to an installed voice, otherwise the device's live current default voice
     * (never persisted/cached) — without mutating engine state. Mirrors [applySelectedVoice]'s
     * resolution branches but is read-only, so it's safe to call from UI state building.
     *
     * @return null if the engine isn't initialized yet or no voice can be resolved for [locale].
     */
    fun getActiveVoiceDisplayName(selectedVoiceName: String?, locale: Locale = Locale.getDefault()): String? {
        val tts = textToSpeech ?: return null

        val candidateVoices = tts.voices
            ?.filter { isVoiceSupportedForLocale(tts, it, locale) && isVoiceDownloaded(it) }
            ?: return null
        val availableVoiceNames = candidateVoices.mapTo(mutableSetOf()) { it.name }

        val resolvedVoiceName = when (resolveVoiceSelection(selectedVoiceName, availableVoiceNames)) {
            VoiceResolution.EXPLICIT -> selectedVoiceName
            VoiceResolution.LIVE_DEFAULT, VoiceResolution.STALE_FALLBACK_TO_LIVE_DEFAULT ->
                tts.defaultVoice?.takeIf { isVoiceSupportedForLocale(tts, it, locale) }?.name
        } ?: return null

        // Reuses getAvailableVoices() as the single source of truth for numbering, so the active
        // voice's label always matches what's shown for it in the Change Voice picker.
        return getAvailableVoices(locale).firstOrNull { it.name == resolvedVoiceName }?.displayName
    }

    /**
     * Speaks [text] in [locale] (or the system default if null), using [selectedVoiceName] if it
     * still resolves to an installed voice, otherwise falling back to the device's own current
     * default voice (checked live, not persisted).
     *
     * @return true if [selectedVoiceName] was non-null but no longer resolves to an installed
     * voice — the caller should clear its persisted selection in that case, since this voice is
     * gone and Vocable has silently fallen back to the device default for this call.
     */
    fun speak(locale: Locale?, text: String, selectedVoiceName: String? = null): Boolean {
        val tts = textToSpeech ?: run {
            Timber.e("VocableTextToSpeech speak failed: textToSpeech engine is null")
            return false
        }

        val targetLocale = locale ?: Locale.getDefault()
        Timber.d("VocableTextToSpeech speak called. text: '$text', requested locale: $locale, target locale: $targetLocale, selectedVoiceName: $selectedVoiceName")

        if (lastSetLocale?.toLanguageTag() != targetLocale.toLanguageTag()) {
            var result = tts.setLanguage(targetLocale)
            Timber.d("VocableTextToSpeech setLanguage result: $result (LANG_MISSING_DATA=-1, LANG_NOT_SUPPORTED=-2)")

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val fallbackLocale = Locale.forLanguageTag(targetLocale.toLanguageTag())
                Timber.d("VocableTextToSpeech: Trying fallback locale: $fallbackLocale")
                result = tts.setLanguage(fallbackLocale)

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Timber.e("VocableTextToSpeech: Language data missing or not supported for locale $targetLocale and fallback $fallbackLocale. Result code: $result")
                    return false
                }
            }
            lastSetLocale = targetLocale
        }

        val selectionWasStale = applySelectedVoice(tts, selectedVoiceName, targetLocale)
        Timber.d("VocableTextToSpeech: Speaking text...")
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
        return selectionWasStale
    }

    /** Which branch of voice-resolution logic [applySelectedVoice] should take. Pure/testable — see [resolveVoiceSelection]. */
    internal enum class VoiceResolution { EXPLICIT, LIVE_DEFAULT, STALE_FALLBACK_TO_LIVE_DEFAULT }

    /**
     * Pure decision logic, kept free of any real `android.speech.tts` types so it's unit-testable
     * without Robolectric (this project's unit tests use hand-written fakes, not a mocking
     * framework or Robolectric, and `Voice`/`TextToSpeech` can't be constructed under the plain
     * Android SDK stub jar used by `app/src/test`).
     */
    internal fun resolveVoiceSelection(selectedVoiceName: String?, availableVoiceNames: Set<String>): VoiceResolution =
        when {
            selectedVoiceName.isNullOrBlank() -> VoiceResolution.LIVE_DEFAULT
            selectedVoiceName in availableVoiceNames -> VoiceResolution.EXPLICIT
            else -> VoiceResolution.STALE_FALLBACK_TO_LIVE_DEFAULT
        }

    /**
     * @return true if [selectedVoiceName] was provided but didn't resolve to an installed voice
     * (stale/removed) — the live device default was applied instead in that case.
     */
    private fun applySelectedVoice(tts: TextToSpeech, selectedVoiceName: String?, locale: Locale): Boolean {
        val availableVoiceNames = tts.voices
            ?.filter { isVoiceSupportedForLocale(tts, it, locale) && isVoiceDownloaded(it) }
            ?.mapTo(mutableSetOf()) { it.name }
            ?: emptySet()

        return when (resolveVoiceSelection(selectedVoiceName, availableVoiceNames)) {
            VoiceResolution.LIVE_DEFAULT -> {
                applyLiveDefaultVoice(tts, locale)
                false
            }
            VoiceResolution.EXPLICIT -> {
                val matchingVoice = tts.voices?.firstOrNull { it.name == selectedVoiceName }
                if (matchingVoice != null) {
                    tts.voice = matchingVoice
                    Timber.d("VocableTextToSpeech applied voice: ${matchingVoice.name}")
                }
                false
            }
            VoiceResolution.STALE_FALLBACK_TO_LIVE_DEFAULT -> {
                Timber.w("VocableTextToSpeech: selected voice '$selectedVoiceName' no longer resolves, falling back to device default")
                applyLiveDefaultVoice(tts, locale)
                true
            }
        }
    }

    /** Reads the engine's own current default voice live, every call — never cached or persisted. */
    private fun applyLiveDefaultVoice(tts: TextToSpeech, locale: Locale) {
        val defaultVoice = tts.getDefaultVoice()
        if (defaultVoice != null && isVoiceSupportedForLocale(tts, defaultVoice, locale)) {
            tts.voice = defaultVoice
            Timber.d("VocableTextToSpeech applied device default voice: ${defaultVoice.name}")
        } else {
            Timber.w("VocableTextToSpeech: device default voice unavailable/unsupported for locale $locale; leaving engine's current voice as-is")
        }
    }

    private fun isVoiceSupportedForLocale(tts: TextToSpeech, voice: Voice, locale: Locale): Boolean {
        val voiceLocale = voice.locale ?: return false
        val languageMatches = voiceLocale.language.equals(locale.language, ignoreCase = true)
        return languageMatches && !voice.isNetworkConnectionRequired && !isVoiceUnavailable(tts, voice)
    }

    private fun isVoiceUnavailable(tts: TextToSpeech, voice: Voice): Boolean {
        return tts.isLanguageAvailable(voice.locale) < TextToSpeech.LANG_AVAILABLE
    }

    /**
     * The engine keeps listing a voice in [TextToSpeech.getVoices] even after its data has been
     * uninstalled — it's only flagged via [TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED], not
     * removed from the list. Name/locale matching alone can't tell "installed" from "known but
     * uninstalled," so this must be checked explicitly wherever a voice is treated as usable.
     */
    private fun isVoiceDownloaded(voice: Voice): Boolean = isVoiceDownloaded(voice.features)

    /** Pure/testable half of [isVoiceDownloaded] — operates on the raw feature set, not a real [Voice]. */
    internal fun isVoiceDownloaded(features: Set<String>?): Boolean =
        features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true

    /** A voice's attributes needed to number it for display — kept free of [Voice] so it's unit-testable. */
    internal data class VoiceLabelInput(val name: String, val localeDisplayName: String, val quality: Int)

    /**
     * Android's TTS `Voice` API has no human-friendly name (unlike iOS's `AVSpeechSynthesisVoice.name`),
     * so each voice is labeled by locale plus an ordinal within that locale instead — e.g.
     * "English (United States) Voice 1", "Voice 2" — assigned in the same order used to display the
     * list elsewhere (quality desc, then name). A previous quality-word label ("Enhanced"/"Standard")
     * used an en-dash that some TTS engines mis-speak when interpolated into a preview sample.
     */
    internal fun buildVoiceDisplayNames(voices: List<VoiceLabelInput>): Map<String, String> =
        voices
            .sortedWith(compareByDescending<VoiceLabelInput> { it.quality }.thenBy { it.name })
            .groupBy { it.localeDisplayName }
            .flatMap { (localeName, localeVoices) ->
                localeVoices.mapIndexed { index, voice -> voice.name to "$localeName Voice ${index + 1}" }
            }
            .toMap()
}