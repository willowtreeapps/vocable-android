package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.ICategoriesUseCase
import com.willowtree.vocable.PhrasesUseCase
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.Phrase
import com.willowtree.vocable.room.CategorySortOrder
import com.willowtree.vocable.utils.ILocalizedResourceUtility
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EditCategoriesViewModel(
    private val phrasesUseCase: PhrasesUseCase,
    private val categoriesUseCase: ICategoriesUseCase,
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

            phrasesUseCase.deletePhrase(phrase.phraseId)

            // Refresh phrase list
            fetchCategoryPhrases(category)
        }
    }

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

    fun moveCategoryUp(categoryId: String) {
        viewModelScope.launch {
            val catIndex = overallCategories.indexOfFirst { it.categoryId == categoryId }
            if (catIndex > 0) {
                val previousCat = overallCategories[catIndex - 1]

                overallCategories = overallCategories.map {
                    when (it.categoryId) {
                        categoryId -> {
                            it.withSortOrder(it.sortOrder - 1)
                        }
                        previousCat.categoryId -> {
                            it.withSortOrder(it.sortOrder + 1)
                        }
                        else -> {
                            it
                        }
                    }
                }.sortedBy { it.sortOrder }

                liveOrderCategoryList.postValue(overallCategories)

                categoriesUseCase.updateCategorySortOrders(
                    overallCategories.map {
                        CategorySortOrder(it.categoryId, it.sortOrder)
                    }
                )
            }
        }
    }

    fun moveCategoryDown(categoryId: String) {
        viewModelScope.launch {
            val catIndex = overallCategories.indexOfFirst { it.categoryId == categoryId }
            if (catIndex > -1) {
                val nextCat = overallCategories[catIndex + 1]

                overallCategories = overallCategories.map {
                    when (it.categoryId) {
                        categoryId -> {
                            it.withSortOrder(it.sortOrder + 1)
                        }
                        nextCat.categoryId -> {
                            it.withSortOrder(it.sortOrder - 1)
                        }
                        else -> {
                            it
                        }
                    }
                }.sortedBy { it.sortOrder }

                liveOrderCategoryList.postValue(overallCategories)

                categoriesUseCase.updateCategorySortOrders(
                    overallCategories.map {
                        CategorySortOrder(it.categoryId, it.sortOrder)
                    }
                )
            }
        }
    }
}