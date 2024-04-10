package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.IPhrasesUseCase
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.utils.ILocalizedResourceUtility
import kotlinx.coroutines.launch

class EditCategoryPhrasesViewModel(
    private val phrasesUseCase: IPhrasesUseCase,
    private val localizedResourceUtility: ILocalizedResourceUtility
): ViewModel() {
    private val liveCategoryPhraseList = MutableLiveData<List<Phrase>>()
    val categoryPhraseList: LiveData<List<Phrase>> = liveCategoryPhraseList

    fun getCategoryName(category: Category): String {
        return localizedResourceUtility.getTextFromCategory(category)
    }

    fun fetchCategoryPhrases(category: Category) {
        viewModelScope.launch {
            val phrasesForCategory = phrasesUseCase.getPhrasesForCategory(category.categoryId)
                .sortedBy { it.sortOrder }
            liveCategoryPhraseList.postValue(phrasesForCategory)
        }
    }

    fun deletePhraseFromCategory(phrase: Phrase, category: Category) {
        viewModelScope.launch {

            phrasesUseCase.deletePhrase(phrase.phraseId)

            // Refresh phrase list
            fetchCategoryPhrases(category)
        }
    }
}