package com.willowtree.vocable.utils

import android.content.SharedPreferences
import com.willowtree.vocable.core.IVocableSharedPreferences
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

    override fun registerOnSharedPreferenceChangeListener(vararg listeners: SharedPreferences.OnSharedPreferenceChangeListener) {
        // no-op currently
    }

    override fun unregisterOnSharedPreferenceChangeListener(vararg listeners: SharedPreferences.OnSharedPreferenceChangeListener) {
        // no-op currently
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
    }

    override fun setHeadTrackingEnabled(enabled: Boolean) {
        headTrackingEnabled = enabled
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

    private var debugTrackingEngine: String? = null

    override fun getDebugTrackingEngine(): String? {
        return debugTrackingEngine
    }

    override fun setDebugTrackingEngine(engineName: String) {
        debugTrackingEngine = engineName
    }

    override fun clearAll() {
        mySayings = listOf()
        dwellTime = DEFAULT_DWELL_TIME
        sensitivity = DEFAULT_SENSITIVITY
        headTrackingEnabled = DEFAULT_HEAD_TRACKING_ENABLED
        selectedVoiceName = null
        debugTrackingEngine = null
    }
}