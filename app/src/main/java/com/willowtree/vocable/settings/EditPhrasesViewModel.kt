package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.Phrase
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

class EditPhrasesViewModel : ViewModel(), KoinComponent {

    private val presetsRepository: PresetsRepository by inject()

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    fun updatePhrase(phrase: Phrase) {
        viewModelScope.launch {
            presetsRepository.updatePhrase(phrase)
            liveShowPhraseAdded.postValue(true)
        }
    }

    fun addNewPhrase(phraseStr: String) {
        viewModelScope.launch {
            val mySayingsPhrases = presetsRepository.getPhrasesForCategory(PresetCategories.MY_SAYINGS.id)
            presetsRepository.addPhrase(
                Phrase(
                    0L,
                    PresetCategories.RECENTS.id,
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    mapOf(Pair(Locale.getDefault().toString(), phraseStr)),
                    mySayingsPhrases.size
                )
            )
            liveShowPhraseAdded.postValue(true)
        }
    }

    fun phraseToFalse() {
        liveShowPhraseAdded.value = false
    }
}
