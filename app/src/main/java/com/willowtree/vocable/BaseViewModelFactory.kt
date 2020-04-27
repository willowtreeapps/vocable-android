package com.willowtree.vocable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.willowtree.vocable.keyboard.KeyboardViewModel
import com.willowtree.vocable.presets.PresetsViewModel
import com.willowtree.vocable.settings.EditCategoriesViewModel
import com.willowtree.vocable.settings.EditPhrasesViewModel
import com.willowtree.vocable.settings.SettingsViewModel
import com.willowtree.vocable.splash.SplashViewModel

class BaseViewModelFactory(
    private val numbersCategoryId: String,
    private val mySayingsCategoryId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        when {
            modelClass.isAssignableFrom(SplashViewModel::class.java) -> {
                return SplashViewModel(numbersCategoryId, mySayingsCategoryId) as T
            }
            modelClass.isAssignableFrom(PresetsViewModel::class.java) -> {
                return PresetsViewModel(numbersCategoryId, mySayingsCategoryId) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                return SettingsViewModel(numbersCategoryId, mySayingsCategoryId) as T
            }
            modelClass.isAssignableFrom(KeyboardViewModel::class.java) -> {
                return KeyboardViewModel(numbersCategoryId, mySayingsCategoryId) as T
            }
            modelClass.isAssignableFrom(EditPhrasesViewModel::class.java) -> {
                return EditPhrasesViewModel(numbersCategoryId, mySayingsCategoryId) as T
            }
            modelClass.isAssignableFrom(EditCategoriesViewModel::class.java) -> {
                return EditCategoriesViewModel(numbersCategoryId, mySayingsCategoryId) as T
            }
            else -> throw IllegalArgumentException("Unknown class: ${modelClass::class.java.canonicalName}")
        }
    }
}