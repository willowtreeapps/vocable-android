package com.willowtree.vocable.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.utils.VocableSharedPreferences
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SplashViewModel : ViewModel(), KoinComponent {

    private val presetsRepository: PresetsRepository by inject()

    private val sharedPrefs: VocableSharedPreferences by inject()

    private val liveExitSplash = MutableLiveData<Boolean>()
    val exitSplash: LiveData<Boolean> = liveExitSplash

    init {
        populateDatabase()
    }

    private fun populateDatabase() {
        viewModelScope.launch {
            if (sharedPrefs.getFirstTime()) {
                presetsRepository.populateDatabase()
                sharedPrefs.setFirstTime()
            }

            liveExitSplash.postValue(true)
        }
    }
}
