package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.utils.VocableSharedPreferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsViewModel : ViewModel(), KoinComponent {

    private val presetsRepository: PresetsRepository by inject()
}
