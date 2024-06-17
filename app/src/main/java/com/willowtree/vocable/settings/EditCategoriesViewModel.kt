package com.willowtree.vocable.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.ICategoriesUseCase
import com.willowtree.vocable.presets.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditCategoriesViewModel(
    private val categoriesUseCase: ICategoriesUseCase
) : ViewModel() {

    val categoryList = categoriesUseCase.categories()

    val liveLastViewedIndex =  MutableStateFlow(0)

    private var overallCategories = listOf<Category>()

    fun refreshCategories() {
        viewModelScope.launch {

            val oldCategories = overallCategories

            overallCategories = categoriesUseCase.categories().first()

            // Check if a new category was added and scroll to it only immediately after added
            if (oldCategories.isNotEmpty() && oldCategories.size < overallCategories.size) {
                when (val firstHiddenIndex = overallCategories.indexOfFirst { it.hidden }) {
                    -1 -> {
                        liveLastViewedIndex.update { overallCategories.size - 1 }
                    }

                    0 -> {
                        liveLastViewedIndex.update { 0 }
                    }

                    else -> {
                        liveLastViewedIndex.update { firstHiddenIndex - 1 }
                    }
                }
            }
            else {
                liveLastViewedIndex.update { 0 }
            }
        }
    }

    fun moveCategoryUp(categoryId: String) {

        viewModelScope.launch {
            categoriesUseCase.moveCategoryUp(categoryId)
        }
    }

    fun moveCategoryDown(categoryId: String) {
        viewModelScope.launch {
            categoriesUseCase.moveCategoryDown(categoryId)
        }
    }

}