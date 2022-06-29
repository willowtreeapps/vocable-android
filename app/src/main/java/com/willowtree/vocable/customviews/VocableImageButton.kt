package com.willowtree.vocable.customviews

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.willowtree.vocable.utils.VocableSharedPreferences
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class VocableImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(context, attrs, defStyle),
    PointerListener,
    SharedPreferences.OnSharedPreferenceChangeListener,
    KoinComponent {

    private var buttonJob: Job? = null
    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val uiScope = CoroutineScope(Dispatchers.Main)

    private val sharedPrefs: VocableSharedPreferences by inject()
    private var dwellTime: Long

    var action: (() -> Unit)? = null

    init {
        dwellTime = sharedPrefs.getDwellTime()
        setOnClickListener {
            performAction()
        }
    }

    override fun onPointerEnter() {
        buttonJob = backgroundScope.launch {
            uiScope.launch {
                isSelected = true
            }

            delay(dwellTime)

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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        sharedPrefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDetachedFromWindow() {
        buttonJob?.cancel()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this)
        super.onDetachedFromWindow()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            VocableSharedPreferences.KEY_DWELL_TIME -> {
                dwellTime = sharedPrefs.getDwellTime()
            }
        }
    }
}