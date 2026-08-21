package com.willowtree.vocable.utils

import android.content.SharedPreferences
import com.willowtree.vocable.core.IVocableSharedPreferences
import com.willowtree.vocable.core.VocableSharedPreferences
import com.willowtree.vocable.core.VocableSharedPreferences.Companion.DEFAULT_DWELL_TIME
import com.willowtree.vocable.core.VocableSharedPreferences.Companion.DEFAULT_HEAD_TRACKING_ENABLED
import com.willowtree.vocable.core.VocableSharedPreferences.Companion.DEFAULT_SENSITIVITY


class FakeVocableSharedPreferences(
    private var mySayings: List<String> = listOf(),
    private var dwellTime: Long = 0,
    private var sensitivity: Float = 0f,
    private var headTrackingEnabled: Boolean = false,
    private var selectedVoiceName: String? = null
) : IVocableSharedPreferences {

    private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun registerOnSharedPreferenceChangeListener(vararg listeners: SharedPreferences.OnSharedPreferenceChangeListener) {
        this.listeners += listeners
    }

    override fun unregisterOnSharedPreferenceChangeListener(vararg listeners: SharedPreferences.OnSharedPreferenceChangeListener) {
        this.listeners -= listeners.toSet()
    }

    // The real preferences have no SharedPreferences instance to hand back on JVM; production
    // listeners key off the changed-key string only.
    private fun notifyListeners(key: String) {
        listeners.forEach { it.onSharedPreferenceChanged(null, key) }
    }

    override fun getMySayings(): List<String> {
        return mySayings
    }

    override fun setMySayings(mySayings: Set<String>) {
        this.mySayings = mySayings.toList()
    }

    override fun getDwellTime(): Long {
        return dwellTime
    }

    override fun setDwellTime(time: Long) {
        dwellTime = time
    }

    override fun getSensitivity(): Float {
        return sensitivity
    }

    override fun setSensitivity(sensitivity: Float) {
        this.sensitivity = sensitivity
        notifyListeners(VocableSharedPreferences.KEY_SENSITIVITY)
    }

    override fun setHeadTrackingEnabled(enabled: Boolean) {
        headTrackingEnabled = enabled
        notifyListeners(VocableSharedPreferences.KEY_HEAD_TRACKING_ENABLED)
    }

    override fun getHeadTrackingEnabled(): Boolean {
        return headTrackingEnabled
    }

    override fun setSelectedVoiceName(voiceName: String?) {
        selectedVoiceName = voiceName
    }

    override fun getSelectedVoiceName(): String? {
        return selectedVoiceName
    }

    override fun resetSensitivity() {
        dwellTime = DEFAULT_DWELL_TIME
        sensitivity = DEFAULT_SENSITIVITY
    }

    override fun clearAll() {
        mySayings = listOf()
        dwellTime = DEFAULT_DWELL_TIME
        sensitivity = DEFAULT_SENSITIVITY
        headTrackingEnabled = DEFAULT_HEAD_TRACKING_ENABLED
        selectedVoiceName = null
    }
}