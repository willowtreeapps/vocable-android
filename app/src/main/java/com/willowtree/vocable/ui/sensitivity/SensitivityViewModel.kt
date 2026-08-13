package com.willowtree.vocable.ui.sensitivity

import androidx.lifecycle.ViewModel
import com.willowtree.vocable.core.IVocableSharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** ViewModel for the [SensitivityScreen]. Manages state related to sensitivity and dwell time settings. */
class SensitivityViewModel(
    private val sharedPrefs: IVocableSharedPreferences
) : ViewModel() {

    companion object {
        const val LOW_SENSITIVITY = 0.05F
        const val MEDIUM_SENSITIVITY = 0.1F
        const val HIGH_SENSITIVITY = 0.15F
        const val DWELL_TIME_ONE_SECOND = 1000L
        private const val DWELL_TIME_CHANGE = 500L
        private const val MIN_DWELL_TIME = 500L
        private const val MAX_DWELL_TIME = 4000L
    }

    private val _sensitivity = MutableStateFlow(sharedPrefs.getSensitivity())
    val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    private val _dwellTime = MutableStateFlow(sharedPrefs.getDwellTime())
    val dwellTime: StateFlow<Long> = _dwellTime.asStateFlow()

    fun setSensitivity(value: Float) {
        sharedPrefs.setSensitivity(value)
        _sensitivity.update { value }
    }

    fun increaseDwellTime() {
        _dwellTime.update { current ->
            val newTime = (current + DWELL_TIME_CHANGE).coerceAtMost(MAX_DWELL_TIME)
            sharedPrefs.setDwellTime(newTime)
            newTime
        }
    }

    fun decreaseDwellTime() {
        _dwellTime.update { current ->
            val newTime = (current - DWELL_TIME_CHANGE).coerceAtLeast(MIN_DWELL_TIME)
            sharedPrefs.setDwellTime(newTime)
            newTime
        }
    }
}