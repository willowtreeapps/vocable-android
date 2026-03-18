package com.willowtree.vocable.ui.editcategorymenu

import com.willowtree.vocable.domain.model.Category

/** State for the Edit Category Menu screen. */
data class EditCategoryMenuState(
    val category: Category? = null,
    /** True when this is the only category left (disable delete). */
    val isLastCategory: Boolean = false
)