package com.willowtree.vocable.data.room

import androidx.room.ColumnInfo

data class PresetCategoryDeleted(
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "deleted") val deleted: Boolean
)
