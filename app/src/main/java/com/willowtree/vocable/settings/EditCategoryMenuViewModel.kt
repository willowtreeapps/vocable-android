package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.CategoriesUseCase
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.presets.PresetCategories
import com.willowtree.vocable.presets.LegacyCategoriesAndPhrasesRepository
import com.willowtree.vocable.room.CategorySortOrder
import kotlinx.coroutines.launch

class EditCategoryMenuViewModel(
    private val presetsRepository: LegacyCategoriesAndPhrasesRepository,
    private val categoriesUseCase: CategoriesUseCase
) : ViewModel() {

    // TODO: PK - Does this need to be LiveData? Is it updating?
    private val _currentCategory = MutableLiveData<Category>()
    val currentCategory: LiveData<Category> = _currentCategory

    private val _lastCategoryRemaining = MutableLiveData<Boolean>()
    val lastCategoryRemaining: LiveData<Boolean> = _lastCategoryRemaining

    fun updateCategoryById(categoryId: String) {
        viewModelScope.launch {
            _lastCategoryRemaining.postValue(presetsRepository.getAllCategories().size == 1)
            _currentCategory.postValue(categoriesUseCase.getCategoryById(categoryId))
        }
    }

    fun updateHiddenStatus(showCategoryStatus: Boolean) {
        viewModelScope.launch {
            _currentCategory.value = _currentCategory.value?.withHidden(!showCategoryStatus)
            _currentCategory.value?.let {
                categoriesUseCase.updateCategoryHidden(
                    it.categoryId,
                    it.hidden
                )
            }
        }
    }

    fun deleteCategory() {
        viewModelScope.launch {
            val category = _currentCategory.value

            // Delete any phrases whose only associated category is the one being deleted
            // First get the ids of all phrases associated with the category being deleted
            val phrasesForCategory = category?.let { categoryPhrase ->
                presetsRepository.getPhrasesForCategory(categoryPhrase.categoryId)
                    .sortedBy { it.sortOrder }
            }


            //Delete from Recents by utterance
            if (phrasesForCategory != null) {
                presetsRepository.deletePhrases(
                    presetsRepository.getPhrasesForCategory(PresetCategories.RECENTS.id)
                        .filter {
                            phrasesForCategory.map { phrase ->
                                phrase.localizedUtterance
                            }.contains(it.localizedUtterance)
                        }
                )
            }

            //Delete phrases
            presetsRepository.deletePhrases(
                phrasesForCategory ?: listOf()
            )

            // Delete the category
            if (category != null) {
                categoriesUseCase.deleteCategory(category.categoryId)
            }

            // Update the sort order of remaining categories
            val overallCategories = presetsRepository.getAllCategories()
            val categoriesToUpdate =
                overallCategories.filter { it.sortOrder > category?.sortOrder ?: 0 }
            categoriesToUpdate.forEach {
                it.sortOrder--
            }
            if (categoriesToUpdate.isNotEmpty()) {
                categoriesUseCase.updateCategorySortOrders(categoriesToUpdate.map {
                    CategorySortOrder(
                        it.categoryId,
                        it.sortOrder
                    )
                })
            }
        }
    }
}
