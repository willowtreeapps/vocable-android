package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.ICategoriesUseCase
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.room.CategorySortOrder
import com.willowtree.vocable.utils.ILocalizedResourceUtility
import com.willowtree.vocable.utils.locale.LocaleProvider
import com.willowtree.vocable.utils.locale.LocalesWithText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddUpdateCategoryViewModel(
    private val categoriesUseCase: ICategoriesUseCase,
    private val localizedResourceUtility: ILocalizedResourceUtility,
    private val localeProvider: LocaleProvider
) : ViewModel() {

    companion object {
        private const val CATEGORY_MESSAGE_DELAY = 2000L
    }

    private val liveShowCategoryUpdateMessage = MutableLiveData<Boolean>()
    val showCategoryUpdateMessage: LiveData<Boolean> = liveShowCategoryUpdateMessage

    private val liveShowDuplicateCategoryMessage = MutableLiveData<Boolean>()
    val showDuplicateCategoryMessage: LiveData<Boolean> = liveShowDuplicateCategoryMessage

    fun updateCategory(categoryId: String, updatedName: String) {
        viewModelScope.launch {
            // Don't allow duplicate category names
            if (categoryNameExists(updatedName)) {
                liveShowDuplicateCategoryMessage.postValue(true)
                return@launch
            }

            val toUpdate = categoriesUseCase.categories().first().first { it.categoryId == categoryId }

            val localesWithText = when(toUpdate) {
                is Category.PresetCategory -> LocalesWithText(mapOf(localeProvider.getDefaultLocaleString() to updatedName))
                is Category.StoredCategory -> toUpdate.localizedName
                is Category.Recents -> throw IllegalArgumentException("Cannot update Recents category name!")
            }

            val updatedLocalizedNames: LocalesWithText = localesWithText.set(localeProvider.getDefaultLocaleString(), updatedName)

            categoriesUseCase.updateCategoryName(toUpdate.categoryId, updatedLocalizedNames)
            liveShowCategoryUpdateMessage.postValue(true)
            delay(CATEGORY_MESSAGE_DELAY)
            liveShowCategoryUpdateMessage.postValue(false)
        }
    }

    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            val allCategories = categoriesUseCase.categories().first()
            // Don't allow duplicate category names
            if (categoryNameExists(categoryName)) {
                liveShowDuplicateCategoryMessage.postValue(true)
                return@launch
            }

            // Get the index of the first hidden category to find the sort order of new category
            var firstHiddenIndex = allCategories.indexOfFirst { it.hidden }
            if (firstHiddenIndex == -1) {
                firstHiddenIndex = allCategories.size
            }

            // Increase the sort order of all hidden categories since the new one will be sorted
            // before them
            val listToUpdate = allCategories.filter { it.hidden }.toMutableList()
            listToUpdate.forEachIndexed { index, category ->
                listToUpdate[index] = category.withSortOrder(category.sortOrder + 1)
            }

            with(categoriesUseCase) {
                addCategory(categoryName, firstHiddenIndex)
                updateCategorySortOrders(listToUpdate.map { CategorySortOrder(it.categoryId, it.sortOrder) })
            }

            liveShowCategoryUpdateMessage.postValue(true)
            delay(CATEGORY_MESSAGE_DELAY)
            liveShowCategoryUpdateMessage.postValue(false)
        }
    }

    private suspend fun categoryNameExists(categoryName: String): Boolean {
        val allCategories = categoriesUseCase.categories().first()
        allCategories.forEach {
            val name = localizedResourceUtility.getTextFromCategory(it)
            if (name == categoryName) {
                return true
            }
        }
        return false
    }
}
