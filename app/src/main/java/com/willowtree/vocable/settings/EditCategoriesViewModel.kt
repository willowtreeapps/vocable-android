package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.ICategoriesUseCase
import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.room.CategorySortOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EditCategoriesViewModel(
    private val categoriesUseCase: ICategoriesUseCase
) : ViewModel() {

    val categoryList = categoriesUseCase.categories()

    private val liveLastViewedIndex = MutableLiveData<Int>()
    val lastViewedIndex: LiveData<Int> = liveLastViewedIndex

    private var overallCategories = listOf<Category>()

    fun refreshCategories() {
        viewModelScope.launch {

            val oldCategories = overallCategories

            overallCategories = categoriesUseCase.categories().first()

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

    fun moveCategoryUp(categoryId: String) {
        viewModelScope.launch {
            val categories = categoriesUseCase.categories().first()
            val catIndex = categories.indexOfFirst { it.categoryId == categoryId }
            if (catIndex in 1 until categories.size) {
                val category = categories[catIndex]
                val previousCat = categories[catIndex - 1]

                swapSortOrders(categories, previousCat, category)
            }
        }
    }

    fun moveCategoryDown(categoryId: String) {
        viewModelScope.launch {
            val categories = categoriesUseCase.categories().first()
            val catIndex = categories.indexOfFirst { it.categoryId == categoryId }
            if (catIndex in 0 until categories.size - 1) {
                val category = categories[catIndex]
                val nextCat = categories[catIndex + 1]

                swapSortOrders(categories, category, nextCat)
            }
        }
    }

    private suspend fun swapSortOrders(
        categories: List<Category>,
        leftCategory: Category,
        rightCategory: Category
    ) {
        categoriesUseCase.updateCategorySortOrders(
            categories.map {
                val sortOrder = when (it.categoryId) {
                    rightCategory.categoryId -> leftCategory.sortOrder
                    leftCategory.categoryId -> rightCategory.sortOrder
                    else -> it.sortOrder
                }
                CategorySortOrder(it.categoryId, sortOrder)
            }
        )
    }
}