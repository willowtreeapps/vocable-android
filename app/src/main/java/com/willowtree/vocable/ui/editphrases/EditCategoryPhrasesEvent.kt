package com.willowtree.vocable.ui.editphrases

/** Events that the Edit Category Phrases screen can send to its parent. */
sealed interface EditCategoryPhrasesEvent {
    object NavigateBack : EditCategoryPhrasesEvent
    data class NavigateToAddPhrase(val categoryId: String) : EditCategoryPhrasesEvent
    data class NavigateToEditPhrase(val phraseId: String, val text: String) : EditCategoryPhrasesEvent
}