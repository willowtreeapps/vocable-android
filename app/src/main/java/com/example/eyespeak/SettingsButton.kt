package com.example.eyespeak

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.util.AttributeSet
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : EyeSpeakButton(context, attrs, defStyle) {

    init {
        setOnClickListener {
            navigateToSettings()
        }
    }

    override fun onPointerEnter() {
        buttonJob = backgroundScope.launch {
            delay(2000)
            uiScope.launch {
                isPressed = true
                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                VocableTextToSpeech.getTextToSpeech()
                    ?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id.toString())
                navigateToSettings()
            }
        }
    }

    private fun navigateToSettings() {
        context.startActivity(Intent(context, SettingsActivity::class.java))
    }
}