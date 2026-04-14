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

    fun setSelectedLanguageTag(tag: String?)

    fun getSelectedLanguageTag(): String?

    fun setFirstTime()

    fun getFirstTime(): Boolean
}