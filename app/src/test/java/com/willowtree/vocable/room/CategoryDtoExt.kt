package com.willowtree.vocable.room

import com.willowtree.vocable.utils.locale.LocalesWithText

fun createCategoryDto(
    categoryId: String,
    creationDate: Long = 0L,
    resourceId: Int? = null,
    localizedName: LocalesWithText? = LocalesWithText(mapOf("en_US" to "category")),
    hidden: Boolean = false,
    sortOrder: Int = 0
): CategoryDto = CategoryDto(
    categoryId = categoryId,
    creationDate = creationDate,
    resourceId = resourceId,
    localizedName = localizedName,
    hidden = hidden,
    sortOrder = sortOrder,
)