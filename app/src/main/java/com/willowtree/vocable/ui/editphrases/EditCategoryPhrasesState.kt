package com.willowtree.vocable.ui.editphrases

import com.willowtree.vocable.domain.model.Category
import com.willowtree.vocable.domain.model.Phrase
import kotlin.math.ceil

/** State for the Edit Category Phrases screen. */
data class EditCategoryPhrasesState(
    val category: Category? = null,
    val categoryName: String = "",
    val phrases: List<Phrase> = emptyList(),
    val currentPage: Int = 0,
    val itemsPerPage: Int = 8, // Single column, 8 rows per page matching wireframe
) {
    val totalPages: Int get() = if (phrases.isEmpty()) 1 else ceil(phrases.size.toFloat() / itemsPerPage).toInt()
    val currentPagePhrases: List<Phrase>
        get() = phrases.chunked(itemsPerPage).getOrElse(currentPage) { emptyList() }
}