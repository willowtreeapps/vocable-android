package com.example.eyespeak

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.AttributeSet
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CloseButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : EyeSpeakButton(context, attrs, defStyle) {

    init {
        setOnClickListener {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            VocableTextToSpeech.getTextToSpeech()
                ?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id.toString())
            if (context is AppCompatActivity) {
                context.finish()
            }
        }
    }

    override fun onPointerEnter() {
        text = "Close"
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
                if (context is AppCompatActivity) {
                    (context as AppCompatActivity).finish()
                }
            }
        }
    }

    override fun onPointerExit() {
        text = null
        super.onPointerExit()
    }
}