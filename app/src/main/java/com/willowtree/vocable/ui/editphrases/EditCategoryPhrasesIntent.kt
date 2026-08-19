package com.willowtree.vocable.ui.editphrases

/** Intents that the Edit Category Phrases screen can send to its parent. */
sealed interface EditCategoryPhrasesIntent {
    object Back : EditCategoryPhrasesIntent
    object AddPhrase : EditCategoryPhrasesIntent
    data class EditPhrase(val phraseId: String, val text: String) : EditCategoryPhrasesIntent
    data class DeletePhrase(val phraseId: String) : EditCategoryPhrasesIntent
    data class UpdateItemsPerPage(val itemsPerPage: Int) : EditCategoryPhrasesIntent
    object NextPage : EditCategoryPhrasesIntent
    object PrevPage : EditCategoryPhrasesIntent
}