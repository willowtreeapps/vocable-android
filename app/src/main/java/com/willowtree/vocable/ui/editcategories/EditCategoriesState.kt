package com.willowtree.vocable.ui.editcategories

import com.willowtree.vocable.domain.model.Category
import kotlin.math.ceil

/**
 * This file defines the state for the EditCategoriesViewModel. The state represents the current
 * data and UI configuration for the Edit Categories screen. It includes the list of categories,
 * pagination information, and any derived properties needed for the UI.
 */
data class EditCategoriesState(
    val categories: List<Category> = emptyList(),
    val currentPage: Int = 0,
    val itemsPerPage: Int = 6,
) {
    val totalPages: Int get() = if (categories.isEmpty()) 1 else ceil(categories.size.toFloat() / itemsPerPage).toInt()
    val currentPageCategories: List<Category>
        get() = categories.chunked(itemsPerPage).getOrElse(currentPage) { emptyList() }
    val visibleCount: Int get() = categories.count { !it.hidden }
}