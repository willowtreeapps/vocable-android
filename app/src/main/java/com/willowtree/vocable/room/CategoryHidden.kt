package com.willowtree.vocable.room

import androidx.room.ColumnInfo

data class CategoryHidden(
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "hidden") val hidden: Boolean
)
