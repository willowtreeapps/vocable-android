package com.willowtree.vocable.ui.editcategories

import com.willowtree.vocable.domain.model.Category

/**
 * This file defines the events that the EditCategoriesViewModel can send to the UI layer.
 * These events represent one-time actions that the UI should perform, such as navigation.
 */
sealed interface EditCategoriesEvent {
    data class NavigateToEditCategory(val category: Category) : EditCategoriesEvent
    object NavigateToAddCategory : EditCategoriesEvent
    object NavigateBack : EditCategoriesEvent
}