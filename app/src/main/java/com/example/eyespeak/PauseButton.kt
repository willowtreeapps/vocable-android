package com.example.eyespeak

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.AttributeSet
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PauseButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : EyeSpeakButton(context, attrs, defStyle) {

    private val liveIsPaused = MutableLiveData<Boolean>().apply {
        value = false
    }
    var isPaused: LiveData<Boolean> = liveIsPaused

    init {
        setOnClickListener {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            VocableTextToSpeech.getTextToSpeech()
                ?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id.toString())
            togglePause()
        }
    }

    override fun onPointerEnter() {
        if (isPaused.value == false) {
            text = "Pause"
        }
        buttonJob = backgroundScope.launch {

            delay(2000)
            uiScope.launch {
                isPressed = true
                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                VocableTextToSpeech.getTextToSpeech()
                    ?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id.toString())
                togglePause()
            }
        }
    }

    override fun onPointerExit() {
        super.onPointerExit()
        if (isPaused.value == false) {
            text = null
        }
    }

    fun togglePause(): Boolean {
        liveIsPaused.value?.let {
            liveIsPaused.value = !it
        }
        text = if (liveIsPaused.value == true) {
            "Resume"
        } else {
            "Pause"
        }
        return liveIsPaused.value ?: false
    }

}