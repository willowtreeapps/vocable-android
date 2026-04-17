package com.willowtree.vocable.ui.editcategories

import com.willowtree.vocable.domain.model.Category

/**
 * This file defines the intents that the EditCategoriesViewModel can receive from the UI layer.
 * These intents represent user actions or events that the ViewModel should handle.
 */
sealed interface EditCategoriesIntent {
    object Back : EditCategoriesIntent
    object AddCategory : EditCategoriesIntent
    data class EditCategory(val category: Category) : EditCategoriesIntent
    data class MoveCategoryUp(val categoryId: String) : EditCategoriesIntent
    data class MoveCategoryDown(val categoryId: String) : EditCategoriesIntent
    data class UpdateItemsPerPage(val itemsPerPage: Int) : EditCategoriesIntent
    object NextPage : EditCategoriesIntent
    object PrevPage : EditCategoriesIntent
}