package com.example.eyespeak

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.AttributeSet
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : EyeSpeakButton(context, attrs, defStyle) {

    var action: (() -> Unit)? = null

    init {
        setOnClickListener {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            VocableTextToSpeech.getTextToSpeech()
                ?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id.toString())
            action?.invoke()
        }
    }

    override fun onPointerEnter() {
        buttonJob = backgroundScope.launch {
            uiScope.launch {
                isSelected = true
            }

            delay(2000)

            uiScope.launch {
                isSelected = false
                isPressed = true
                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                VocableTextToSpeech.getTextToSpeech()
                    ?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id.toString())
                action?.invoke()
            }
        }
    }
}