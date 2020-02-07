package com.example.eyespeak

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.AttributeSet
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import kotlinx.coroutines.*

open class EyeSpeakButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatButton(context, attrs, defStyle), PointerListener {

    protected var buttonJob: Job? = null
    protected val backgroundScope = CoroutineScope(Dispatchers.IO)
    protected val uiScope = CoroutineScope(Dispatchers.Main)

    init {
        setOnClickListener {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            VocableTextToSpeech.getTextToSpeech()
                ?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id.toString())
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
            }
        }
    }

    override fun onPointerExit() {
        isPressed = false
        isSelected = false
        buttonJob?.cancel()
    }

    override fun onDetachedFromWindow() {
        buttonJob?.cancel()
        super.onDetachedFromWindow()
    }
}