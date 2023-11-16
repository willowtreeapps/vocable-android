package com.willowtree.vocable.utils

import android.content.SharedPreferences


class FakeVocableSharedPreferences(
    private var mySayings: List<String> = listOf(),
    private var dwellTime: Long = 0,
    private var sensitivity: Float = 0f,
    private var headTrackingEnabled: Boolean = false,
    private var firstTime: Boolean = false
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

    override fun setFirstTime() {
        firstTime = true
    }

    override fun getFirstTime(): Boolean {
        return firstTime
    }
}