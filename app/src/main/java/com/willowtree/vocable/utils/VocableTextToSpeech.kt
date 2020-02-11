package com.willowtree.vocable.utils

import android.content.Context
import android.speech.tts.TextToSpeech

object VocableTextToSpeech {

    private var textToSpeech: TextToSpeech? = null

    fun initialize(context: Context) {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(context, TextToSpeech.OnInitListener {
                // No-op
            })
        }
    }

    fun shutdown() {
        with(textToSpeech) {
            this?.stop()
            this?.shutdown()
        }
    }

    fun getTextToSpeech(): TextToSpeech? {
        return textToSpeech
    }
}