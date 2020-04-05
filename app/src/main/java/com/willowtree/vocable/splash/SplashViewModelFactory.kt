package com.willowtree.vocable.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SplashViewModelFactory(
    private val numbersCategoryId: String,
    private val mySayingsCategoryId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
            return SplashViewModel(numbersCategoryId, mySayingsCategoryId) as T
        }
        throw IllegalArgumentException("Unknown class: ${modelClass::class.java.canonicalName}")
    }
}