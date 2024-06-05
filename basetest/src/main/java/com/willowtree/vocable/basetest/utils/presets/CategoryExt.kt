package com.willowtree.vocable.basetest.utils.presets

import com.willowtree.vocable.presets.Category
import com.willowtree.vocable.utils.locale.LocalesWithText

fun createStoredCategory(
    categoryId: String,
    localizedName: LocalesWithText = LocalesWithText(mapOf("en_US" to "category")),
    hidden: Boolean = false,
    sortOrder: Int = 0
): Category.StoredCategory = Category.StoredCategory(
    categoryId = categoryId,
    localizedName = localizedName,
    hidden = hidden,
    sortOrder = sortOrder,
)