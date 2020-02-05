package com.example.eyespeak

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.AttributeSet
import android.widget.RadioButton
import android.widget.Toast
import kotlinx.coroutines.*

class CategoryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RadioButton(context, attrs, defStyle), PointerListener {

    private var buttonJob: Job? = null
    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val uiScope = CoroutineScope(Dispatchers.Main)

    init {
        setOnClickListener {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            VocableTextToSpeech.getTextToSpeech()
                ?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id.toString())
        }
    }

    override fun onPointerEnter() {
        if (!isChecked) {
            buttonJob = backgroundScope.launch {
                uiScope.launch {
                    isSelected = true
                }

                delay(2000)

                uiScope.launch {
                    isSelected = false
                    isChecked = true
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                    VocableTextToSpeech.getTextToSpeech()
                        ?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id.toString())
                }
            }
        }
    }

    override fun onPointerExit() {
        isSelected = false
        buttonJob?.cancel()
    }
}