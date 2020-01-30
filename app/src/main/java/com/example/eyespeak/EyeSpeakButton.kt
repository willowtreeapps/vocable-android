package com.example.eyespeak

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.Toast
import kotlinx.coroutines.*

class EyeSpeakButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : Button(context, attrs, defStyle), PointerListener {

    private var buttonJob: Job? = null
    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onPointerEnter() {
        buttonJob = backgroundScope.launch {
            delay(3000)
            uiScope.launch {
                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPointerExit() {
        buttonJob?.cancel()
    }

    override fun onDetachedFromWindow() {
        buttonJob?.cancel()
        super.onDetachedFromWindow()
    }
}