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

    fun initialize(context: Context) {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(context) {
                Timber.d("VocableTextToSpeech initialized with status: $it")
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
    }

    fun getAvailableVoices(locale: Locale = Locale.getDefault()): List<VoiceOption> {
        val tts = textToSpeech ?: return emptyList()
        val availableVoices = tts.voices ?: return emptyList()

        return availableVoices
            .filter { voice ->
                val voiceLocale = voice.locale ?: return@filter false
                voiceLocale.language.equals(locale.language, ignoreCase = true) &&
                    !voice.isNetworkConnectionRequired
            }
            .sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.name })
            .map { voice ->
                val isDownloaded = voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
                VoiceOption(
                    name = voice.name,
                    displayName = buildVoiceDisplayName(voice),
                    locale = voice.locale,
                    isDownloaded = isDownloaded
                )
            }
    }

    fun getCurrentEngine(): String? = textToSpeech?.defaultEngine

    fun speak(locale: Locale?, text: String, selectedVoiceName: String? = null) {
        textToSpeech?.let { tts ->
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
                        return@let
                    }
                }
                lastSetLocale = targetLocale
            }

            applySelectedVoice(tts, selectedVoiceName, targetLocale)
            Timber.d("VocableTextToSpeech: Speaking text...")
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
        } ?: run {
            Timber.e("VocableTextToSpeech speak failed: textToSpeech engine is null")
        }
    }

    private fun applySelectedVoice(tts: TextToSpeech, selectedVoiceName: String?, locale: Locale) {
        if (selectedVoiceName.isNullOrBlank()) return

        val matchingVoice = tts.voices
            ?.firstOrNull { it.name == selectedVoiceName && isVoiceSupportedForLocale(tts, it, locale) }

        if (matchingVoice != null) {
            tts.voice = matchingVoice
            Timber.d("VocableTextToSpeech applied voice: ${matchingVoice.name}")
        } else {
            Timber.w("VocableTextToSpeech could not find compatible voice for name: $selectedVoiceName")
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

    private fun buildVoiceDisplayName(voice: Voice): String {
        val localeName = voice.locale.displayName
        val qualityLabel = when {
            voice.quality >= Voice.QUALITY_VERY_HIGH -> "Enhanced"
            voice.quality >= Voice.QUALITY_HIGH -> "High Quality"
            else -> "Standard"
        }
        return "$localeName – $qualityLabel"
    }
}