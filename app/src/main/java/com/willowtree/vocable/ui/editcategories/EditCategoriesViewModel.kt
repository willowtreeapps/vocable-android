package com.willowtree.vocable.ui.editcategories

import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.domain.usecase.ICategoriesUseCase
import com.willowtree.vocable.ui.base.BaseViewModel
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * ViewModel for the Edit Categories screen. It manages the list of categories and pagination logic.
 */
class EditCategoriesViewModel(
    private val categoriesUseCase: ICategoriesUseCase
) : BaseViewModel<EditCategoriesState, EditCategoriesEvent>(EditCategoriesState()) {

    init {
        viewModelScope.launch {
            categoriesUseCase.categories().collect { categories ->
                val previousCategoriesSize = uiState.value.categories.size
                updateState {
                    val updatedState = copy(categories = categories)
                    updatedState.copy(currentPage = updatedState.currentPage.coerceAtMost(updatedState.totalPages - 1))
                }

                // After a new category is added, jump to the last page to show it
                if (previousCategoriesSize > 0 && categories.size > previousCategoriesSize) {
                    val lastPage = uiState.value.totalPages - 1
                    updateState { copy(currentPage = lastPage) }
                }
            }
        }
    }

    fun onIntent(intent: EditCategoriesIntent) {
        when (intent) {
            EditCategoriesIntent.Back -> sendEvent(EditCategoriesEvent.NavigateBack)
            EditCategoriesIntent.AddCategory -> sendEvent(EditCategoriesEvent.NavigateToAddCategory)
            is EditCategoriesIntent.EditCategory -> sendEvent(EditCategoriesEvent.NavigateToEditCategory(intent.category))
            is EditCategoriesIntent.MoveCategoryUp -> viewModelScope.launch {
                categoriesUseCase.moveCategoryUp(intent.categoryId)
            }
            is EditCategoriesIntent.MoveCategoryDown -> viewModelScope.launch {
                categoriesUseCase.moveCategoryDown(intent.categoryId)
            }
            is EditCategoriesIntent.UpdateItemsPerPage -> updateState {
                val newItemsPerPage = intent.itemsPerPage.coerceAtLeast(1)
                val newTotalPages = if (categories.isEmpty()) 1 else ceil(categories.size.toFloat() / newItemsPerPage).toInt()
                copy(
                    itemsPerPage = newItemsPerPage,
                    currentPage = currentPage.coerceAtMost(newTotalPages - 1)
                )
            }
            EditCategoriesIntent.NextPage -> updateState {
                copy(currentPage = (currentPage + 1) % totalPages)
            }
            EditCategoriesIntent.PrevPage -> updateState {
                copy(currentPage = if (currentPage - 1 < 0) totalPages - 1 else currentPage - 1)
            }
        }
    }
}