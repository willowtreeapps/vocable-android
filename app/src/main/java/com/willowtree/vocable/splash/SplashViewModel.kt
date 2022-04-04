package com.willowtree.vocable.splash

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.R
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.utils.VocableSharedPreferences
import kotlinx.coroutines.launch
import org.koin.core.get
import org.koin.core.inject
import timber.log.Timber
import java.util.*

class SplashViewModel : BaseViewModel() {

    private val presetsRepository: PresetsRepository by inject()

    private val sharedPrefs: VocableSharedPreferences by inject()

    private val liveExitSplash = MutableLiveData<Boolean>()
    val exitSplash: LiveData<Boolean> = liveExitSplash

    init {
        populateDatabase()
    }

    private fun populateDatabase() {
        backgroundScope.launch {
            if (sharedPrefs.getFirstTime()) {
                presetsRepository.populateDatabase()
                sharedPrefs.setFirstTime()
            }

            liveExitSplash.postValue(true)
        }
    }
}
