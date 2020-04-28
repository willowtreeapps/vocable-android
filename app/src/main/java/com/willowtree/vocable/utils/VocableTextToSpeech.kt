package com.willowtree.vocable.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.*

object VocableTextToSpeech {

    private var textToSpeech: TextToSpeech? = null

    private val liveIsSpeaking = MutableLiveData<Boolean>()
    val isSpeaking: LiveData<Boolean> = liveIsSpeaking

    fun initialize(context: Context) {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(context, TextToSpeech.OnInitListener {
                // No-op
            }).apply {
                setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onDone(utteranceId: String?) {
                        liveIsSpeaking.postValue(false)
                    }

                    override fun onError(utteranceId: String?) {
                        liveIsSpeaking.postValue(false)
                    }

                    override fun onStart(utteranceId: String?) {
                        liveIsSpeaking.postValue(true)
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
    }

    fun speak(locale: Locale, text: String) {
        textToSpeech?.let {
            it.language = locale
            it.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
        }
    }
}