package com.example.eyespeak

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

object VocableTextToSpeech {

    private var textToSpeech: TextToSpeech? = null

    fun getTextToSpeech(context: Context): TextToSpeech? {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(context, TextToSpeech.OnInitListener {
                if (it != TextToSpeech.ERROR) {
                    textToSpeech?.language = Locale.US
                }
            })
        }
        return textToSpeech
    }
}