package com.willowtree.vocable.utils

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

    fun setFirstTime()

    fun getFirstTime(): Boolean
}