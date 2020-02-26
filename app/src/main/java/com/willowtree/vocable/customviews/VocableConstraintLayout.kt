package com.willowtree.vocable.customviews

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.*

class VocableConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle),
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
            action?.invoke()
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
                action?.invoke()
            }
        }
    }

    override fun onPointerExit() {
        isSelected = false
        buttonJob?.cancel()
    }
}