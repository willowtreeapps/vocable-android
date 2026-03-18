package com.willowtree.vocable.core

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.*

object VocableTextToSpeech {

    private var textToSpeech: TextToSpeech? = null

    private val liveIsSpeaking = MutableLiveData<Boolean>()
    val isSpeaking: LiveData<Boolean> = liveIsSpeaking

    private val _isSpeakingFlow = MutableStateFlow(false)
    val isSpeakingFlow: StateFlow<Boolean> = _isSpeakingFlow.asStateFlow()

    fun initialize(context: Context) {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(context, TextToSpeech.OnInitListener {
                Timber.d("VocableTextToSpeech initialized with status: $it")
            }).apply {
                setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onDone(utteranceId: String?) {
                        liveIsSpeaking.postValue(false)
                        _isSpeakingFlow.value = false
                    }

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
        _isSpeakingFlow.value = false
    }

    fun speak(locale: Locale?, text: String) {
        textToSpeech?.let { tts ->
            val targetLocale = locale ?: Locale.getDefault()
            Timber.d("VocableTextToSpeech speak called. text: '$text', requested locale: $locale, target locale: $targetLocale")
            
            var result = tts.setLanguage(targetLocale)
            Timber.d("VocableTextToSpeech setLanguage result: $result (LANG_MISSING_DATA=-1, LANG_NOT_SUPPORTED=-2)")
            
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Try falling back to just the language without the region/country code
                val fallbackLocale = Locale(targetLocale.language)
                Timber.d("VocableTextToSpeech: Trying fallback locale: $fallbackLocale")
                result = tts.setLanguage(fallbackLocale)
                
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Timber.e("VocableTextToSpeech: Language data missing or not supported for locale $targetLocale and fallback $fallbackLocale. Result code: $result")
                } else {
                    Timber.d("VocableTextToSpeech: Speaking text using fallback locale...")
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
                }
            } else {
                Timber.d("VocableTextToSpeech: Speaking text...")
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
            }
        } ?: run {
            Timber.e("VocableTextToSpeech speak failed: textToSpeech engine is null")
        }
    }
}
