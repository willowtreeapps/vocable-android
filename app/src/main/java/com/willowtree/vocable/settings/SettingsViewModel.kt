package com.willowtree.vocable.settings

import androidx.lifecycle.ViewModel
import com.willowtree.vocable.presets.LegacyCategoriesAndPhrasesRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsViewModel : ViewModel(), KoinComponent {

    private val presetsRepository: LegacyCategoriesAndPhrasesRepository by inject()
}
