package com.willowtree.vocable.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.willowtree.vocable.settings.SensitivityFragment
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class VocableSharedPreferences :
    IVocableSharedPreferences,
    KoinComponent {

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
        const val KEY_FIRST_TIME = "KEY_FIRST_TIME_OPENING"
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        val context = get<Context>()
        EncryptedSharedPreferences.create(
            PREFERENCES_NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun registerOnSharedPreferenceChangeListener(vararg listeners: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.forEach {
            encryptedPrefs.registerOnSharedPreferenceChangeListener(it)
        }
    }

    override fun unregisterOnSharedPreferenceChangeListener(vararg listeners: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.forEach {
            encryptedPrefs.unregisterOnSharedPreferenceChangeListener(it)
        }
    }

    override fun getMySayings(): List<String> {
        encryptedPrefs.getStringSet(KEY_MY_SAYINGS, setOf())?.let {
            return it.toList()
        }
        return listOf()
    }

    override fun setMySayings(mySayings: Set<String>) {
        encryptedPrefs.edit().putStringSet(KEY_MY_SAYINGS, mySayings).apply()
    }

    override fun getDwellTime(): Long = encryptedPrefs.getLong(KEY_DWELL_TIME, DEFAULT_DWELL_TIME)

    override fun setDwellTime(time: Long) {
        encryptedPrefs.edit().putLong(KEY_DWELL_TIME, time).apply()
    }

    override fun getSensitivity(): Float = encryptedPrefs.getFloat(KEY_SENSITIVITY, DEFAULT_SENSITIVITY)

    override fun setSensitivity(sensitivity: Float) {
        encryptedPrefs.edit().putFloat(KEY_SENSITIVITY, sensitivity).apply()
    }

    override fun setHeadTrackingEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_HEAD_TRACKING_ENABLED, enabled).apply()
    }

    override fun getHeadTrackingEnabled(): Boolean =
        encryptedPrefs.getBoolean(KEY_HEAD_TRACKING_ENABLED, true)

    override fun setFirstTime() {
        encryptedPrefs.edit().putBoolean(KEY_FIRST_TIME, false).apply()
    }

    override fun getFirstTime(): Boolean =
        encryptedPrefs.getBoolean(KEY_FIRST_TIME, true)

    @SuppressLint("ApplySharedPref")
    fun clearAll() {
        encryptedPrefs.edit().clear().commit()
    }
}
