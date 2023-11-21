package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.PhrasesUseCase
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class EditPhrasesViewModel : ViewModel(), KoinComponent {

    private val phrasesUseCase: PhrasesUseCase by inject()

    private val liveShowPhraseAdded = MutableLiveData<Boolean>()
    val showPhraseAdded: LiveData<Boolean> = liveShowPhraseAdded

    fun updatePhrase(phraseId: Long, localizedUtterance: LocalesWithText) {
        viewModelScope.launch {
            phrasesUseCase.updatePhrase(phraseId, localizedUtterance)
            liveShowPhraseAdded.postValue(true)
        }
    }

    fun phraseToFalse() {
        liveShowPhraseAdded.value = false
    }
}
