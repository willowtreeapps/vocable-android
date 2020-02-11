package com.willowtree.vocable.customviews

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import com.willowtree.vocable.R
import com.willowtree.vocable.utils.VocableTextToSpeech

/**
 * A subclass of VocableButton that will close the current activity (if the button's context is an
 * AppCompatActivity)
 */
class CloseButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : VocableButton(context, attrs, defStyle) {

    override fun onPointerEnter() {
        setText(R.string.button_close)
        super.onPointerEnter()
    }

    override fun performAction() {
        if (context is AppCompatActivity) {
            (context as AppCompatActivity).finish()
        }
    }

    override fun onPointerExit() {
        text = null
        super.onPointerExit()
    }

    override fun sayText(text: CharSequence?) {
        text?.let {
            VocableTextToSpeech.getTextToSpeech()
                ?.speak(it, TextToSpeech.QUEUE_FLUSH, null, id.toString())
        }
    }
}