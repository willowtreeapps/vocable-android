package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.CategoriesUseCase
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.IPresetsRepository
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.utils.ILocalizedResourceUtility
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EditCategoriesViewModel(
    private val presetsRepository: IPresetsRepository,
    private val categoriesUseCase: CategoriesUseCase,
    private val localizedResourceUtility: ILocalizedResourceUtility
) : ViewModel() {

    private val liveOrderCategoryList = MutableLiveData<List<Category>>()
    val orderCategoryList: LiveData<List<Category>> = liveOrderCategoryList

    private val liveAddRemoveCategoryList = MutableLiveData<List<Category>>()
    val addRemoveCategoryList: LiveData<List<Category>> = liveAddRemoveCategoryList

    private val liveLastViewedIndex = MutableLiveData<Int>()
    val lastViewedIndex: LiveData<Int> = liveLastViewedIndex

    private val liveCategoryPhraseList = MutableLiveData<List<Phrase>>()
    val categoryPhraseList: LiveData<List<Phrase>> = liveCategoryPhraseList

    private var overallCategories = listOf<Category>()

    fun refreshCategories() {
        viewModelScope.launch {

            val oldCategories = overallCategories

            overallCategories = categoriesUseCase.categories().first()
            overallCategories =
                overallCategories.filter { !it.hidden } + overallCategories.filter { it.hidden }


            liveOrderCategoryList.postValue(overallCategories)
            liveAddRemoveCategoryList.postValue(overallCategories)

            // Check if a new category was added and scroll to it
            if (oldCategories.isNotEmpty() && oldCategories.size < overallCategories.size) {
                when (val firstHiddenIndex = overallCategories.indexOfFirst { it.hidden }) {
                    -1 -> {
                        liveLastViewedIndex.postValue(overallCategories.size - 1)
                    }

                    0 -> {
                        liveLastViewedIndex.postValue(0)
                    }

                    else -> {
                        liveLastViewedIndex.postValue(firstHiddenIndex - 1)
                    }
                }
            }
        }
    }

    fun deletePhraseFromCategory(phrase: Phrase, category: Category) {
        viewModelScope.launch {

            presetsRepository.deletePhrase(phrase)
            presetsRepository.getPhrasesForCategory(PresetCategories.RECENTS.id)
                .firstOrNull {
                    it.localizedUtterance == phrase.localizedUtterance
                }?.let {
                    presetsRepository.deletePhrase(
                        it
                    )
                }

            // Refresh phrase list
            fetchCategoryPhrases(category)
        }
    }

    fun getCategoryName(category: Category): String {
        return localizedResourceUtility.getTextFromCategory(category)
    }

    fun fetchCategoryPhrases(category: Category) {
        viewModelScope.launch {
            val phrasesForCategory = presetsRepository.getPhrasesForCategory(category.categoryId)
                .sortedBy { it.sortOrder }
            liveCategoryPhraseList.postValue(phrasesForCategory)
        }
    }

    fun moveCategoryUp(category: Category) {
        viewModelScope.launch {
            val catIndex = overallCategories.indexOf(category)
            if (catIndex > 0) {
                val previousCat = overallCategories[catIndex - 1]

                overallCategories = overallCategories.sortedBy { it.sortOrder }
                liveOrderCategoryList.postValue(overallCategories)

                categoriesUseCase.updateCategories(
                    listOf(
                        category.withSortOrder(category.sortOrder - 1),
                        previousCat.withSortOrder(previousCat.sortOrder + 1)
                    )
                )
            }
        }
    }

    fun moveCategoryDown(category: Category) {
        viewModelScope.launch {
            val catIndex = overallCategories.indexOf(category)
            if (catIndex > -1) {
                val nextCat = overallCategories[catIndex + 1]

                overallCategories = overallCategories.sortedBy { it.sortOrder }
                liveOrderCategoryList.postValue(overallCategories)

                categoriesUseCase.updateCategories(
                    listOf(
                        category.withSortOrder(category.sortOrder + 1),
                        nextCat.withSortOrder(nextCat.sortOrder - 1)
                    )
                )
            }
        }
    }
}