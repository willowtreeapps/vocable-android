package com.willowtree.vocable.customviews

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.AttributeSet
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.R
import com.willowtree.vocable.utils.VocableTextToSpeech

/**
 * A subclass of VocableButton that will toggle the pause state for head tracking
 */
class PauseButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : VocableButton(context, attrs, defStyle) {

    private val liveIsPaused = MutableLiveData<Boolean>().apply {
        value = false
    }
    var isPaused: LiveData<Boolean> = liveIsPaused
    private var pausedIntentionally = false

    override fun onPointerEnter() {
        if (isPaused.value == false) {
            setText(R.string.button_pause)
        }
        super.onPointerEnter()
    }

    override fun onPointerExit() {
        super.onPointerExit()
        if (isPaused.value == false) {
            text = null
        }
    }

    override fun performAction() {
        liveIsPaused.value?.let {
            liveIsPaused.value = !it
        }
        pausedIntentionally = if (liveIsPaused.value == true) {
            setText(R.string.button_resume)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_resume, 0, 0, 0)
            true
        } else {
            setText(R.string.button_pause)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0)
            false
        }
    }

    override fun sayText(text: CharSequence?) {
        text?.let {
            VocableTextToSpeech.getTextToSpeech()
                ?.speak(it, TextToSpeech.QUEUE_FLUSH, null, id.toString())
        }
    }

    /**
     * Toggles the pause state if the user has not intentionally paused head tracking
     */
    fun togglePause(pause: Boolean) {
        if (!pausedIntentionally && liveIsPaused.value != pause) {
            liveIsPaused.postValue(pause)
            if (pause) {
                setText(R.string.button_resume)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_resume, 0, 0, 0)
            } else {
                text = null
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0)
            }
        }
    }
}