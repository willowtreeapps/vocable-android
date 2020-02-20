package com.willowtree.vocable.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.koin.core.KoinComponent
import org.koin.core.get

class VocableSharedPreferences : KoinComponent {

    companion object {
        private const val PREFERENCES_NAME =
            "com.willowtree.vocable.utils.vocable-encrypted-preferences"
        private const val KEY_MY_SAYINGS = "KEY_MY_SAYINGS"
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

    fun getMySayings(): List<String> {
        encryptedPrefs.getStringSet(KEY_MY_SAYINGS, emptySet())?.let {
            return it.toList()
        }
        return emptyList()
    }

    fun addSaying(saying: String) {
        encryptedPrefs.getStringSet(KEY_MY_SAYINGS, emptySet())?.let {
            it.add(saying)
            encryptedPrefs.edit().putStringSet(KEY_MY_SAYINGS, it).apply()
        }
    }
}