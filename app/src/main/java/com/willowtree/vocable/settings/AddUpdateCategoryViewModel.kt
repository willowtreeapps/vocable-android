package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.CategoriesUseCase
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.utils.DateProvider
import com.willowtree.vocable.utils.ILocalizedResourceUtility
import com.willowtree.vocable.utils.LocaleProvider
import com.willowtree.vocable.utils.UUIDProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddUpdateCategoryViewModel(
    private val categoriesUseCase: CategoriesUseCase,
    private val localizedResourceUtility: ILocalizedResourceUtility,
    private val uuidProvider: UUIDProvider,
    private val dateProvider: DateProvider,
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

            val toUpdate = categoriesUseCase.categories().first().firstOrNull { it.categoryId == categoryId }
            toUpdate?.let {
                val currentName = it.localizedName?.get(localeProvider.getDefaultLocaleString())

                if (currentName == updatedName) {
                    return@let
                }

                val updatedNameMap = mapOf(localeProvider.getDefaultLocaleString() to updatedName)

                categoriesUseCase.updateCategory(it.copy(localizedName = updatedNameMap))
                liveShowCategoryUpdateMessage.postValue(true)
                delay(CATEGORY_MESSAGE_DELAY)
                liveShowCategoryUpdateMessage.postValue(false)
            }
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
            val listToUpdate = allCategories.filter { it.hidden }
            listToUpdate.forEach {
                it.sortOrder++
            }

            val newCategory = Category(
                uuidProvider.randomUUIDString(),
                dateProvider.currentTimeMillis(),
                null,
                mapOf(Pair(localeProvider.getDefaultLocaleString(), categoryName)),
                false,
                firstHiddenIndex
            )

            with(categoriesUseCase) {
                addCategory(newCategory)
                updateCategories(listToUpdate)
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
