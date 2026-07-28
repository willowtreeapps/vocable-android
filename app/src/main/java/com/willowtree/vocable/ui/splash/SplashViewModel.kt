package com.willowtree.vocable.ui.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.data.repository.RoomPresetPhrasesRepository
import com.willowtree.vocable.core.IdlingResourceContainer
import kotlinx.coroutines.launch

class SplashViewModel(
    private val newPresetsRepository: RoomPresetPhrasesRepository,
    private val idlingResourceContainer: IdlingResourceContainer
) : ViewModel() {

    private val liveExitSplash = MutableLiveData(false)
    val exitSplash: LiveData<Boolean> = liveExitSplash

    init {
        populateDatabase()
    }

    private fun populateDatabase() {
        viewModelScope.launch {
            idlingResourceContainer.run {
                // Runs on every launch, not just the first: seeding is idempotent, and it is also
                // what reconciles the stored preset phrases with the resource arrays. Gating it on
                // a first-time flag meant a phrase added to - or reordered within - an array in a
                // later release never reached anyone who had already opened the app.
                newPresetsRepository.populateDatabase()

                liveExitSplash.postValue(true)
            }
        }
    }
}
