package com.willowtree.vocable.ui.editphrases

import androidx.lifecycle.viewModelScope
import com.willowtree.vocable.core.ILocalizedResourceUtility
import com.willowtree.vocable.domain.usecase.ICategoriesUseCase
import com.willowtree.vocable.domain.usecase.IPhrasesUseCase
import com.willowtree.vocable.ui.base.BaseViewModel
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * ViewModel for the [EditCategoryPhrasesScreen]. Handles loading the category's phrases and responding to
 * user interactions.
 */
class EditCategoryPhrasesViewModel(
    private val categoriesUseCase: ICategoriesUseCase,
    private val phrasesUseCase: IPhrasesUseCase,
    private val localizedResourceUtility: ILocalizedResourceUtility
) : BaseViewModel<EditCategoryPhrasesState, EditCategoryPhrasesEvent>(EditCategoryPhrasesState()) {

    fun loadCategory(categoryId: String) {
        viewModelScope.launch {
            val category = categoriesUseCase.getCategoryById(categoryId)
            val name = localizedResourceUtility.getTextFromCategory(category)
            updateState { copy(category = category, categoryName = name) }

            phrasesUseCase.getPhrasesForCategoryFlow(categoryId).collect { newPhrases ->
                val previousPhraseCount = uiState.value.phrases.size
                updateState {
                    val updatedState = copy(phrases = newPhrases)
                    updatedState.copy(currentPage = updatedState.currentPage.coerceAtMost(updatedState.totalPages - 1))
                }

                // Jump to last page when a new phrase is added
                if (previousPhraseCount > 0 && newPhrases.size > previousPhraseCount) {
                    val lastPage = uiState.value.totalPages - 1
                    updateState { copy(currentPage = lastPage) }
                }
            }
        }
    }

    fun onIntent(intent: EditCategoryPhrasesIntent) {
        when (intent) {
            EditCategoryPhrasesIntent.Back ->
                sendEvent(EditCategoryPhrasesEvent.NavigateBack)

            EditCategoryPhrasesIntent.AddPhrase ->
                uiState.value.category?.let {
                    sendEvent(EditCategoryPhrasesEvent.NavigateToAddPhrase(it.categoryId))
                }

            is EditCategoryPhrasesIntent.EditPhrase ->
                sendEvent(EditCategoryPhrasesEvent.NavigateToEditPhrase(intent.phraseId, intent.text))

            is EditCategoryPhrasesIntent.DeletePhrase -> viewModelScope.launch {
                phrasesUseCase.deletePhrase(intent.phraseId)
            }

            is EditCategoryPhrasesIntent.UpdateItemsPerPage -> updateState {
                val newItemsPerPage = intent.itemsPerPage.coerceAtLeast(1)
                val newTotalPages = if (phrases.isEmpty()) 1 else ceil(phrases.size.toFloat() / newItemsPerPage).toInt()
                copy(
                    itemsPerPage = newItemsPerPage,
                    currentPage = currentPage.coerceAtMost(newTotalPages - 1)
                )
            }

            EditCategoryPhrasesIntent.NextPage -> updateState {
                copy(currentPage = (currentPage + 1) % totalPages)
            }

            EditCategoryPhrasesIntent.PrevPage -> updateState {
                copy(currentPage = if (currentPage - 1 < 0) totalPages - 1 else currentPage - 1)
            }
        }
    }
}
