package com.willowtree.vocable.customviews

import android.content.Context
import android.speech.tts.TextToSpeech
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatButton
import com.willowtree.vocable.utils.SpokenText
import com.willowtree.vocable.utils.VocableTextToSpeech
import kotlinx.coroutines.*

/**
 * A custom AppCompatButton that will delay for two seconds when a pointer enters and then will call
 * VocableTextToSpeech to say the button text aloud and then perform an action based on the
 * subclass's implementation
 */
open class VocableButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatButton(context, attrs, defStyle),
    PointerListener {

    companion object {
        private const val DEFAULT_TTS_TIMEOUT = 1500L
        private const val SINGLE_SPACE = " "
        private const val NO_TEXT_START = 0
        private const val NO_TEXT_END = 1
    }

    private var buttonJob: Job? = null
    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val uiScope = CoroutineScope(Dispatchers.Main)

    init {
        setOnClickListener {
            sayText(text)
            performAction()
        }
    }

    override fun onPointerEnter() {
        buttonJob = backgroundScope.launch {
            uiScope.launch {
                isSelected = true
            }

            delay(DEFAULT_TTS_TIMEOUT)

            uiScope.launch {
                isSelected = false
                isPressed = true
                sayText(text)
                performAction()
            }
        }
    }

    fun setIconWithNoText(@DrawableRes icon: Int) {
        val imageSpan = ImageSpan(
            context,
            icon,
            DynamicDrawableSpan.ALIGN_BOTTOM
        )
        val stringBuilder = SpannableStringBuilder().apply {
            append(SINGLE_SPACE)
            setSpan(imageSpan, NO_TEXT_START, NO_TEXT_END, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        setText(stringBuilder, BufferType.SPANNABLE)
    }

    protected open fun sayText(text: CharSequence?) {
        text?.let {
            VocableTextToSpeech.getTextToSpeech()
                ?.speak(it, TextToSpeech.QUEUE_FLUSH, null, id.toString())
            SpokenText.postValue(it.toString())
        }
    }

    protected open fun performAction() {
        // No-op in the base class
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