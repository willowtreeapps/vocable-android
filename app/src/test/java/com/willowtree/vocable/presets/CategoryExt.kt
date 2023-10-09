package com.willowtree.vocable.presets

fun createStoredCategory(
    categoryId: String,
    resourceId: Int? = null,
    localizedName: Map<String, String>? = mapOf("en_US" to "category"),
    hidden: Boolean = false,
    sortOrder: Int = 0
): Category.StoredCategory = Category.StoredCategory(
    categoryId = categoryId,
    resourceId = resourceId,
    localizedName = localizedName,
    hidden = hidden,
    sortOrder = sortOrder,
)