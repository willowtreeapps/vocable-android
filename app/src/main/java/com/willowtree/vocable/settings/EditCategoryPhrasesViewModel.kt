package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.IPhrasesUseCase
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.utils.ILocalizedResourceUtility
import kotlinx.coroutines.launch

class EditCategoryPhrasesViewModel(
    savedStateHandle: SavedStateHandle,
    private val phrasesUseCase: IPhrasesUseCase,
    private val localizedResourceUtility: ILocalizedResourceUtility
) : ViewModel() {

    val categoryPhraseList: LiveData<List<Phrase>> = phrasesUseCase.getPhrasesForCategoryFlow(savedStateHandle.get<Category>("category")!!.categoryId)
        .asLiveData()

    fun getCategoryName(category: Category): String {
        return localizedResourceUtility.getTextFromCategory(category)
    }

    fun deletePhraseFromCategory(phrase: Phrase) {
        viewModelScope.launch {
            phrasesUseCase.deletePhrase(phrase.phraseId)
        }
    }
}