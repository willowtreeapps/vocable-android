package com.willowtree.vocable.room

import androidx.room.ColumnInfo

data class CategoryLocalizedName(
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "localized_name") var localizedName: Map<String, String>
)