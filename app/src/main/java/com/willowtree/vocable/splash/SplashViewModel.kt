package com.willowtree.vocable.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.room.RoomPresetPhrasesRepository
import com.willowtree.vocable.utils.VocableSharedPreferences
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

open class SplashViewModel(
    private val newPresetsRepository: RoomPresetPhrasesRepository,
    private val sharedPrefs: VocableSharedPreferences
) : ViewModel(), KoinComponent {

    private val liveExitSplash = MutableLiveData(false)
    val exitSplash: LiveData<Boolean> = liveExitSplash

    init {
        populateDatabase()
    }

    private fun populateDatabase() {
        viewModelScope.launch {
            if (sharedPrefs.getFirstTime()) {
                newPresetsRepository.populateDatabase()
                sharedPrefs.setFirstTime()
            }

            postExitSplash()
        }
    }

    protected open fun postExitSplash() {
        liveExitSplash.postValue(true)
    }
}
