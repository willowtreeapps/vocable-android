package com.willowtree.vocable.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.willowtree.vocable.ui.sensitivity.SensitivityViewModel.Companion.DWELL_TIME_ONE_SECOND
import com.willowtree.vocable.ui.sensitivity.SensitivityViewModel.Companion.MEDIUM_SENSITIVITY
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
        const val DEFAULT_SENSITIVITY = MEDIUM_SENSITIVITY
        const val KEY_DWELL_TIME = "KEY_DWELL_TIME"
        const val DEFAULT_DWELL_TIME = DWELL_TIME_ONE_SECOND
        const val KEY_SELECTED_VOICE_NAME = "KEY_SELECTED_VOICE_NAME"
        const val DEFAULT_HEAD_TRACKING_ENABLED = true
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
        encryptedPrefs.edit { putStringSet(KEY_MY_SAYINGS, mySayings) }
    }

    override fun getDwellTime(): Long = encryptedPrefs.getLong(KEY_DWELL_TIME, DEFAULT_DWELL_TIME)

    override fun setDwellTime(time: Long) {
        encryptedPrefs.edit { putLong(KEY_DWELL_TIME, time) }
    }

    override fun getSensitivity(): Float = encryptedPrefs.getFloat(KEY_SENSITIVITY, DEFAULT_SENSITIVITY)

    override fun setSensitivity(sensitivity: Float) {
        encryptedPrefs.edit { putFloat(KEY_SENSITIVITY, sensitivity) }
    }

    override fun setHeadTrackingEnabled(enabled: Boolean) {
        encryptedPrefs.edit { putBoolean(KEY_HEAD_TRACKING_ENABLED, enabled) }
    }

    override fun getHeadTrackingEnabled(): Boolean =
        encryptedPrefs.getBoolean(KEY_HEAD_TRACKING_ENABLED, DEFAULT_HEAD_TRACKING_ENABLED)

    override fun setSelectedVoiceName(voiceName: String?) {
        encryptedPrefs.edit {
            if (voiceName == null) {
                remove(KEY_SELECTED_VOICE_NAME)
            } else {
                putString(KEY_SELECTED_VOICE_NAME, voiceName)
            }
        }
    }

    override fun getSelectedVoiceName(): String? = encryptedPrefs.getString(KEY_SELECTED_VOICE_NAME, null)

    @SuppressLint("ApplySharedPref")
    override fun clearAll() {
        // A bare clear() only wipes the store - Android's SharedPreferences only notifies
        // OnSharedPreferenceChangeListeners for keys explicitly put/removed in an edit, not for
        // clear() alone. Listeners driving live UI (GazeButton's dwell time, FaceTrackingViewModel's
        // sensitivity/head-tracking, FaceTrackingPermissions) need an explicit follow-up write of
        // each default value to actually be notified, rather than only reflecting it on next read.
        encryptedPrefs.edit(commit = true) { clear() }
        encryptedPrefs.edit(commit = true) {
            putStringSet(KEY_MY_SAYINGS, setOf())
            putLong(KEY_DWELL_TIME, DEFAULT_DWELL_TIME)
            putFloat(KEY_SENSITIVITY, DEFAULT_SENSITIVITY)
            putBoolean(KEY_HEAD_TRACKING_ENABLED, DEFAULT_HEAD_TRACKING_ENABLED)
        }
    }
}