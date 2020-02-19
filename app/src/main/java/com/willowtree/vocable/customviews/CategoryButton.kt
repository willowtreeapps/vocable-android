package com.willowtree.vocable.customviews

import android.content.Context
import android.util.AttributeSet
import kotlinx.coroutines.*

/**
 * A subclass of AppCompatRadioButton that represents a category on the main screen
 */
class CategoryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ActionButton(context, attrs, defStyle),
    PointerListener {

    companion object {
        private const val DEFAULT_TTS_TIMEOUT = 2000L
    }

    private var buttonJob: Job? = null
    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val uiScope = CoroutineScope(Dispatchers.Main)

    init {
        setOnClickListener {
            isSelected = true
            sayText(text)
            performAction()
        }
    }

    override fun onPointerEnter() {
        buttonJob = backgroundScope.launch {
            uiScope.launch {
                isPressed = true
            }

            delay(DEFAULT_TTS_TIMEOUT)

            uiScope.launch {
                isPressed = false
                isSelected = true
                sayText(text)
                performAction()
            }
        }
    }

    override fun onPointerExit() {
        buttonJob?.cancel()
    }
}