package com.willowtree.vocable.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.willowtree.vocable.settings.SensitivityFragment
import org.koin.core.KoinComponent
import org.koin.core.get

class VocableSharedPreferences : KoinComponent {

    companion object {
        private const val PREFERENCES_NAME =
            "com.willowtree.vocable.utils.vocable-encrypted-preferences"
        private const val KEY_MY_SAYINGS = "KEY_MY_SAYINGS"
        private const val KEY_MY_LOCALIZED_SAYINGS = "KEY_MY_LOCALIZED_SAYINGS"
        const val KEY_HEAD_TRACKING_ENABLED = "KEY_HEAD_TRACKING_ENABLED"
        const val KEY_SENSITIVITY = "KEY_SENSITIVITY"
        const val DEFAULT_SENSITIVITY = SensitivityFragment.MEDIUM_SENSITIVITY
        const val KEY_DWELL_TIME = "KEY_DWELL_TIME"
        const val DEFAULT_DWELL_TIME = SensitivityFragment.DWELL_TIME_ONE_SECOND
    }

    private val encryptedPrefs: EncryptedSharedPreferences by lazy {
        val context = get<Context>()
        EncryptedSharedPreferences.create(
            PREFERENCES_NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }

    fun registerOnSharedPreferenceChangeListener(vararg listeners: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.forEach {
            encryptedPrefs.registerOnSharedPreferenceChangeListener(it)
        }
    }

    fun unregisterOnSharedPreferenceChangeListener(vararg listeners: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.forEach {
            encryptedPrefs.unregisterOnSharedPreferenceChangeListener(it)
        }
    }

    fun getMySayings(): List<String> {
        encryptedPrefs.getStringSet(KEY_MY_SAYINGS, setOf())?.let {
            return it.toList()
        }
        return listOf()
    }

    fun setMySayings(mySayings: Set<String>) {
        encryptedPrefs.edit().putStringSet(KEY_MY_SAYINGS, mySayings).apply()
    }

    fun getDwellTime(): Long = encryptedPrefs.getLong(KEY_DWELL_TIME, DEFAULT_DWELL_TIME)

    fun setDwellTime(time: Long) {
        encryptedPrefs.edit().putLong(KEY_DWELL_TIME, time).apply()
    }

    fun getSensitivity(): Float = encryptedPrefs.getFloat(KEY_SENSITIVITY, DEFAULT_SENSITIVITY)

    fun setSensitivity(sensitivity: Float) {
        encryptedPrefs.edit().putFloat(KEY_SENSITIVITY, sensitivity).apply()
    }

    fun setHeadTrackingEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_HEAD_TRACKING_ENABLED, enabled).apply()
    }

    fun getHeadTrackingEnabled(): Boolean =
        encryptedPrefs.getBoolean(KEY_HEAD_TRACKING_ENABLED, true)
}