package com.willowtree.vocable.ui.editcategorymenu

import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.domain.usecase.ICategoriesUseCase
import com.willowtree.vocable.ui.base.BaseViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for the [EditCategoryMenuScreen]. Handles loading the category and responding to
 * user interactions.
 */
class EditCategoryMenuViewModel(
    private val categoriesUseCase: ICategoriesUseCase
) : BaseViewModel<EditCategoryMenuState, EditCategoryMenuEvent>(EditCategoryMenuState()) {

    fun loadCategory(categoryId: String) {
        viewModelScope.launch {
            val allCategories = categoriesUseCase.categories().first()
            val category = allCategories.firstOrNull { it.categoryId == categoryId }
            updateState {
                copy(
                    category = category,
                    isLastCategory = allCategories.size == 1
                )
            }
        }
    }

    fun onIntent(intent: EditCategoryMenuIntent) {
        when (intent) {
            EditCategoryMenuIntent.Back ->
                sendEvent(EditCategoryMenuEvent.NavigateBack)

            EditCategoryMenuIntent.RenameCategory ->
                uiState.value.category?.let {
                    sendEvent(EditCategoryMenuEvent.NavigateToRenameCategory(it.categoryId, ""))
                }

            EditCategoryMenuIntent.EditPhrases ->
                uiState.value.category?.let {
                    sendEvent(EditCategoryMenuEvent.NavigateToEditPhrases(it.categoryId))
                }

            is EditCategoryMenuIntent.SetCategoryShown -> viewModelScope.launch {
                val category = uiState.value.category ?: return@launch
                val updated = category.withHidden(!intent.shown)
                categoriesUseCase.updateCategoryHidden(updated.categoryId, updated.hidden)
                updateState { copy(category = updated) }
            }

            EditCategoryMenuIntent.DeleteCategory -> viewModelScope.launch {
                val category = uiState.value.category ?: return@launch
                categoriesUseCase.deleteCategory(category.categoryId)
                sendEvent(EditCategoryMenuEvent.NavigateBack)
            }
        }
    }
}
