package com.willowtree.vocable.customviews

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import com.willowtree.vocable.utils.SpokenText
import com.willowtree.vocable.utils.VocableTextToSpeech
import kotlinx.coroutines.*

/**
 * A subclass of AppCompatRadioButton that represents a category on the main screen
 */
class CategoryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatRadioButton(context, attrs, defStyle),
    PointerListener {

    companion object {
        private const val DEFAULT_TTS_TIMEOUT = 2000L
    }

    private var buttonJob: Job? = null
    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val uiScope = CoroutineScope(Dispatchers.Main)

    init {
        setOnClickListener {
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

                delay(DEFAULT_TTS_TIMEOUT)

                uiScope.launch {
                    isSelected = false
                    isChecked = true
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