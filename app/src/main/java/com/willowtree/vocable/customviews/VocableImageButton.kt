package com.willowtree.vocable.customviews

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import kotlinx.coroutines.*

class VocableImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(context, attrs, defStyle),
    PointerListener {

    companion object {
        private const val DEFAULT_TTS_TIMEOUT = 1500L
    }

    private var buttonJob: Job? = null
    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val uiScope = CoroutineScope(Dispatchers.Main)

    var action: (() -> Unit)? = null

    init {
        setOnClickListener {
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
                performAction()
            }
        }
    }

    override fun onPointerExit() {
        isPressed = false
        isSelected = false
        buttonJob?.cancel()
    }

    private fun performAction() {
        action?.invoke()
    }

    override fun onDetachedFromWindow() {
        buttonJob?.cancel()
        super.onDetachedFromWindow()
    }
}