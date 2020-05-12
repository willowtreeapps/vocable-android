package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.utils.VocableSharedPreferences
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject

class SettingsViewModel : BaseViewModel(), KoinComponent {

    private val sharedPrefs: VocableSharedPreferences by inject()
    private val presetsRepository: PresetsRepository by inject()

    private val liveHeadTrackingEnabled = MutableLiveData<Boolean>()
    val headTrackingEnabled: LiveData<Boolean> = liveHeadTrackingEnabled

    init {
        liveHeadTrackingEnabled.postValue(sharedPrefs.getHeadTrackingEnabled())
    }

    fun onHeadTrackingChecked(isChecked: Boolean) {
        sharedPrefs.setHeadTrackingEnabled(isChecked)
        liveHeadTrackingEnabled.postValue(isChecked)
    }

}