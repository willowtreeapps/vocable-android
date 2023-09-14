package com.willowtree.vocable.presets

fun createCategory(
    categoryId: String,
    creationDate: Long = 0L,
    resourceId: Int? = null,
    localizedName: Map<String, String>? = mapOf("en_US" to "category"),
    hidden: Boolean = false,
    sortOrder: Int = 0
): Category = Category(
    categoryId = categoryId,
    creationDate = creationDate,
    resourceId = resourceId,
    localizedName = localizedName,
    hidden = hidden,
    sortOrder = sortOrder,
)