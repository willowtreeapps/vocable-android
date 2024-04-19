package com.willowtree.vocable.customviews

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.willowtree.vocable.utils.VocableSharedPreferences
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class VocableConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle),
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
            action?.invoke()
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
                action?.invoke()
            }
        }
    }

    override fun onPointerExit() {
        isSelected = false
        buttonJob?.cancel()
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