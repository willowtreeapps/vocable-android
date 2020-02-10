package com.example.eyespeak.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

object VocableTextToSpeech {

    private var textToSpeech: TextToSpeech? = null

    fun initialize(context: Context) {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(context, TextToSpeech.OnInitListener {
                if (it != TextToSpeech.ERROR) {
                    textToSpeech?.language = Locale.US
                }
            })
        }
    }

    fun shutdown(){
        with(textToSpeech) {
            this?.stop()
            this?.shutdown()
        }
    }

    fun getTextToSpeech(): TextToSpeech? {
        return textToSpeech
    }
}