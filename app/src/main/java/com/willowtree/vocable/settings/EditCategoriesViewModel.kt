package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.willowtree.vocable.BaseViewModel
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.PresetsRepository
import com.willowtree.vocable.room.CategoryDto
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.utils.LocalizedResourceUtility
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class EditCategoriesViewModel : BaseViewModel() {

    private val presetsRepository: PresetsRepository by inject()

    private val liveOrderCategoryList = MutableLiveData<List<CategoryDto>>()
    val orderCategoryList: LiveData<List<CategoryDto>> = liveOrderCategoryList

    private val liveAddRemoveCategoryList = MutableLiveData<List<CategoryDto>>()
    val addRemoveCategoryList: LiveData<List<CategoryDto>> = liveAddRemoveCategoryList

    private val liveLastViewedIndex = MutableLiveData<Int>()
    val lastViewedIndex: LiveData<Int> = liveLastViewedIndex

    private val liveCategoryPhraseList = MutableLiveData<List<Phrase>>()
    val categoryPhraseList: LiveData<List<Phrase>> = liveCategoryPhraseList

    private var overallCategories = listOf<CategoryDto>()

    private val localizedResourceUtility: LocalizedResourceUtility by inject()

    fun refreshCategories() {
        backgroundScope.launch {

            val oldCategories = overallCategories

            overallCategories = presetsRepository.getAllCategories()
            overallCategories = overallCategories.filter {!it.hidden} + overallCategories.filter {it.hidden}


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

    fun deleteCategory(category: CategoryDto) {
        backgroundScope.launch {

            // Delete any phrases whose only associated category is the one being deleted
            // First get the ids of all phrases associated with the category being deleted
            val phrasesForCategory = presetsRepository.getPhrasesForCategory(category.categoryId)
                .sortedBy { it.sortOrder }


            //Delete from Recents by utterance
            presetsRepository.deletePhrases(
                presetsRepository.getPhrasesForCategory(PresetCategories.RECENTS.id)
                    .filter {
                        phrasesForCategory.map { phrase ->
                            phrase.localizedUtterance
                        }.contains(it.localizedUtterance)
                    }
            )

            //Delete phrases
            presetsRepository.deletePhrases(
                phrasesForCategory
            )

            // Delete the category
            presetsRepository.deleteCategory(category)

            // Update the sort order of remaining categories
            val categoriesToUpdate = overallCategories.filter { it.sortOrder > category.sortOrder }
            categoriesToUpdate.forEach {
                it.sortOrder--
            }
            if (categoriesToUpdate.isNotEmpty()) {
                presetsRepository.updateCategories(categoriesToUpdate)
            }

            refreshCategories()
        }
    }

    fun deletePhraseFromCategory(phrase: Phrase, category: CategoryDto) {
        backgroundScope.launch {

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

    fun getUpdatedCategoryName(category: CategoryDto): String {
        val updatedCategory = overallCategories.firstOrNull { it.categoryId == category.categoryId }
        return localizedResourceUtility.getTextFromCategory(updatedCategory)
    }

    fun getUpdatedCategory(category: CategoryDto): CategoryDto {
        return overallCategories.first { it.categoryId == category.categoryId }
    }

    fun fetchCategoryPhrases(category: CategoryDto) {
        backgroundScope.launch {
            val phrasesForCategory = presetsRepository.getPhrasesForCategory(category.categoryId)
                .sortedBy { it.sortOrder }
            liveCategoryPhraseList.postValue(phrasesForCategory)
        }
    }

    fun moveCategoryUp(category: CategoryDto) {
        backgroundScope.launch {
            val catIndex = overallCategories.indexOf(category)
            if (catIndex > 0) {
                val previousCat = overallCategories[catIndex - 1]
                category.sortOrder--
                previousCat.sortOrder++

                overallCategories = overallCategories.sortedBy { it.sortOrder }
                liveOrderCategoryList.postValue(overallCategories)

                presetsRepository.updateCategories(listOf(category, previousCat))
            }
        }
    }

    fun moveCategoryDown(category: CategoryDto) {
        backgroundScope.launch {
            val catIndex = overallCategories.indexOf(category)
            if (catIndex > -1) {
                val nextCat = overallCategories[catIndex + 1]
                category.sortOrder++
                nextCat.sortOrder--

                overallCategories = overallCategories.sortedBy { it.sortOrder }
                liveOrderCategoryList.postValue(overallCategories)

                presetsRepository.updateCategories(listOf(category, nextCat))
            }
        }
    }
}