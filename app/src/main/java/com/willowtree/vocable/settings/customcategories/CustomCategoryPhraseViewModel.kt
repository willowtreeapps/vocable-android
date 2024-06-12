package com.willowtree.vocable.settings.customcategories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.IPhrasesUseCase
import com.willowtree.vocable.presets.Phrase
import kotlinx.coroutines.launch

class CustomCategoryPhraseViewModel(
    private val phrasesUseCase: IPhrasesUseCase
) : ViewModel() {
    fun deletePhraseFromCategory(phrase: Phrase) {
        viewModelScope.launch {
            phrasesUseCase.deletePhrase(phrase.phraseId)
        }
    }
}