package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.PhrasesUseCase
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class EditPhrasesViewModel : ViewModel(), KoinComponent {

    private val phrasesUseCase: PhrasesUseCase by inject()

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    fun updatePhrase(phraseId: String, newText: String) {
        viewModelScope.launch {
            phrasesUseCase.updatePhrase(phraseId, newText)
            liveShowPhraseAdded.postValue(true)
        }
    }

    fun phraseToFalse() {
        liveShowPhraseAdded.value = false
    }
}
