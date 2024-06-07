package com.willowtree.vocable.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.ICategoriesUseCase
import com.willowtree.vocable.presets.Category
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

            // Check if a new category was added and scroll to it only immediately after added
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
            else {
                liveLastViewedIndex.postValue(0)
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