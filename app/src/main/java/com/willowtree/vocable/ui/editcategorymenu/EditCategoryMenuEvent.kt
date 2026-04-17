package com.willowtree.vocable.ui.editcategorymenu

/** Events that the Edit Category Menu screen can send to its parent. */
sealed interface EditCategoryMenuEvent {
    object NavigateBack : EditCategoryMenuEvent
    data class NavigateToRenameCategory(val categoryId: String, val categoryName: String) : EditCategoryMenuEvent
    data class NavigateToEditPhrases(val categoryId: String) : EditCategoryMenuEvent
}