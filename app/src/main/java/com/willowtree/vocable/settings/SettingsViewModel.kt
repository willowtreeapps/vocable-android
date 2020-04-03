package com.willowtree.vocable.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.utils.VocableSharedPreferences
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject

class SettingsViewModel : BaseViewModel(), KoinComponent {

    private val sharedPrefs: VocableSharedPreferences by inject()
    private val presetsRepository: PresetsRepository by inject()
    private val mySayingsCategoryId: String =
        get<Context>().getString(com.willowtree.vocable.R.string.category_my_sayings_id)

    private val liveHeadTrackingEnabled = MutableLiveData<Boolean>()
    val headTrackingEnabled: LiveData<Boolean> = liveHeadTrackingEnabled

    private val liveMySayingsIsEmpty = MutableLiveData<Boolean>()
    val mySayingsIsEmpty: LiveData<Boolean> = liveMySayingsIsEmpty

    init {
        liveHeadTrackingEnabled.postValue(sharedPrefs.getHeadTrackingEnabled())
        checkMySayingsIsEmpty()
    }

    fun onHeadTrackingChecked(isChecked: Boolean) {
        sharedPrefs.setHeadTrackingEnabled(isChecked)
        liveHeadTrackingEnabled.postValue(isChecked)
    }

    private fun checkMySayingsIsEmpty() {
        backgroundScope.launch {
            liveMySayingsIsEmpty.postValue(
                presetsRepository.getPhrasesForCategory(mySayingsCategoryId)
                    .isEmpty()
            )
        }
    }

}