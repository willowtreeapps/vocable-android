package com.willowtree.vocable.core

import android.content.SharedPreferences


interface IVocableSharedPreferences {

    fun registerOnSharedPreferenceChangeListener(vararg listeners: SharedPreferences.OnSharedPreferenceChangeListener)

    fun unregisterOnSharedPreferenceChangeListener(vararg listeners: SharedPreferences.OnSharedPreferenceChangeListener)

    fun getMySayings(): List<String>

    fun setMySayings(mySayings: Set<String>)

    fun getDwellTime(): Long

    fun setDwellTime(time: Long)

    fun getSensitivity(): Float

    fun setSensitivity(sensitivity: Float)

    fun setHeadTrackingEnabled(enabled: Boolean)

    fun getHeadTrackingEnabled(): Boolean

    fun setSelectedVoiceName(voiceName: String?)

    fun getSelectedVoiceName(): String?

    /**
     * Debug-only (#678 engine comparison): which tracking engine drives the gaze cursor,
     * stored as a [com.willowtree.vocable.ui.facetracking.TrackingEngine] name. Only read when
     * BuildConfig.DEBUG - release builds always use ARCore and contain no alternate engines.
     */
    fun getDebugTrackingEngine(): String?

    fun setDebugTrackingEngine(engineName: String)

    fun clearAll()
}