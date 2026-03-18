package com.willowtree.vocable.ui.editcategorymenu

/** Intents that the Edit Category Menu screen can receive from the user. */
sealed interface EditCategoryMenuIntent {
    object Back : EditCategoryMenuIntent
    object RenameCategory : EditCategoryMenuIntent
    object EditPhrases : EditCategoryMenuIntent
    data class SetCategoryShown(val shown: Boolean) : EditCategoryMenuIntent
    object DeleteCategory : EditCategoryMenuIntent
}