package com.willowtree.vocable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.willowtree.vocable.keyboard.KeyboardViewModel
import com.willowtree.vocable.presets.PresetsViewModel
import com.willowtree.vocable.settings.*
import com.willowtree.vocable.splash.SplashViewModel

class BaseViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        when {
            modelClass.isAssignableFrom(SplashViewModel::class.java) -> {
                return SplashViewModel() as T
            }
            modelClass.isAssignableFrom(PresetsViewModel::class.java) -> {
                return PresetsViewModel() as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                return SettingsViewModel() as T
            }
            modelClass.isAssignableFrom(KeyboardViewModel::class.java) -> {
                return KeyboardViewModel() as T
            }
            modelClass.isAssignableFrom(EditPhrasesViewModel::class.java) -> {
                return EditPhrasesViewModel() as T
            }
            modelClass.isAssignableFrom(EditCategoriesViewModel::class.java) -> {
                return EditCategoriesViewModel() as T
            }
            modelClass.isAssignableFrom(AddUpdateCategoryViewModel::class.java) -> {
                return AddUpdateCategoryViewModel() as T
            }
            modelClass.isAssignableFrom(AddPhraseViewModel::class.java) -> {
                return AddPhraseViewModel() as T
            }
            else -> throw IllegalArgumentException("Unknown class: ${modelClass::class.java.canonicalName}")
        }
    }
}