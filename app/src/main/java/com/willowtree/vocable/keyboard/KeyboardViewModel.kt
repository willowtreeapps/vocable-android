package com.willowtree.vocable.keyboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.utils.LocalizedResourceUtility
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class KeyboardViewModel : ViewModel(), KoinComponent {

    companion object {
        const val PHRASE_ADDED_DELAY = 2000L
    }

    private val presetsRepository: PresetsRepository by inject()
    private val localizedResourceUtility: LocalizedResourceUtility by inject()

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    private val liveIsPhraseSaved = MutableLiveData<Boolean>()
    val isPhraseSaved: LiveData<Boolean> = liveIsPhraseSaved

    var currentText = ""
        set(value) {
            field = value
            checkIfPhraseSaved()
        }

    private fun checkIfPhraseSaved() {
        viewModelScope.launch {
            val mySayingsPhrases = presetsRepository.getPhrasesForCategory(PresetCategories.MY_SAYINGS.id)
            val isSaved = mySayingsPhrases.map { localizedResourceUtility.getTextFromPhrase(it) }.contains(currentText)
            liveIsPhraseSaved.postValue(isSaved)
        }
    }
}
